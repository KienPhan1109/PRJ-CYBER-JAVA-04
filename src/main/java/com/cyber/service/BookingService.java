package com.cyber.service;
import com.cyber.connection.DatabaseConnection;
import com.cyber.dao.*;
import com.cyber.dao.impl.*;
import com.cyber.exception.*;
import com.cyber.model.Booking;
import com.cyber.model.Computer;
import com.cyber.model.User;
import com.cyber.model.enums.BookingStatus;
import com.cyber.util.FormatUtils;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class BookingService {
    private static final BookingService INSTANCE = new BookingService();
    private final IBookingDAO bookingDAO;
    private final IUserDAO userDAO;

    private BookingService() {
        this.bookingDAO = BookingDAOImpl.getInstance();
        this.userDAO = UserDAOImpl.getInstance();
    }

    public static BookingService getInstance() {
        return INSTANCE;
    }

    public int bookComputer(int userId, Booking booking) throws BusinessException {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            User currentUser = userDAO.findById(conn, userId);
            if (currentUser == null) throw new BusinessException("NOT_FOUND", "Không tìm thấy người dùng.");
            if (currentUser.getStatus() == com.cyber.model.enums.UserStatus.LOCKED) 
                throw new BusinessException("LOCKED", "Tài khoản đang bị khóa.");

            // Ràng buộc: Mỗi User chỉ được phép có tối đa 1 session (ACTIVE hoặc RESERVED)
            List<Booking> activeList = bookingDAO.findActiveBookingsByUserId(conn, userId);
            if (!activeList.isEmpty()) {
                throw new BusinessException("ALREADY_ACTIVE", "Bạn đang có máy đang sử dụng. Vui lòng ngắt máy trước khi đặt máy mới.");
            }
            List<Booking> allList = bookingDAO.findAllBookingsByUserId(conn, userId);
            boolean hasReserved = allList.stream().anyMatch(b -> b.getStatus() == BookingStatus.RESERVED);
            if (hasReserved) {
                throw new BusinessException("ALREADY_RESERVED", "Bạn đã có lịch đặt máy trước. Mỗi tài khoản chỉ được có 1 phiên duy nhất.");
            }

            // Đặt trạng thái PENDING — chờ Staff phê duyệt
            booking.setStatus(BookingStatus.PENDING);
            booking.setUserId(userId);
            bookingDAO.createBooking(conn, booking);

            conn.commit();
            return booking.getBookingId();
        } catch (SQLException e) {
            if (conn != null) { try { conn.rollback(); } catch (SQLException ignored) {} }
            throw new BusinessException("DB_ERROR", "Lỗi CSDL khi gửi yêu cầu đặt máy: " + e.getMessage());
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); conn.close(); } catch (SQLException ignored) {}
            }
        }
    }

    public List<Booking> getActiveBookingsByUserId(int userId) throws BusinessException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return bookingDAO.findActiveBookingsByUserId(conn, userId);
        } catch (SQLException e) {
            throw new BusinessException("DB_ERROR", "Lỗi lấy danh sách đặt máy: " + e.getMessage());
        }
    }

    public List<Booking> getBookingHistoryByUserId(int userId) throws BusinessException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return bookingDAO.findAllBookingsByUserId(conn, userId);
        } catch (SQLException e) {
            throw new BusinessException("DB_ERROR", "Lỗi lấy danh sách lịch sử đặt máy: " + e.getMessage());
        }
    }

    public int startSession(int userId, int computerId) throws BusinessException {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            User currentUser = userDAO.findById(conn, userId);
            if (currentUser == null) throw new BusinessException("NOT_FOUND", "Không tìm thấy User.");
            if (currentUser.getStatus() == com.cyber.model.enums.UserStatus.LOCKED) throw new BusinessException("LOCKED", "Tài khoản bị khóa.");

            com.cyber.dao.IComputerDAO computerDAO = com.cyber.dao.impl.ComputerDAOImpl.getInstance();
            Computer comp = computerDAO.findById(conn, computerId);
            if (comp == null || comp.isDeleted()) throw new BusinessException("NOT_FOUND", "Máy trạm không tồn tại hoặc đã thanh lý.");
            if (comp.getStatus() == com.cyber.model.enums.ComputerStatus.IN_USE) throw new BusinessException("IN_USE", "Máy đang có khách.");

            // Lấy snapshot giá giờ chơi từ máy tính thời điểm hiện tại
            BigDecimal currentRate = comp.getPricePerHour();

            Booking booking = new Booking(0, userId, computerId, 
                    new java.sql.Timestamp(System.currentTimeMillis()), 
                    new java.sql.Timestamp(System.currentTimeMillis()), // Bắt đầu chơi
                    BookingStatus.ACTIVE, BigDecimal.ZERO, currentRate);
            
            bookingDAO.createBooking(conn, booking);

            comp.setStatus(com.cyber.model.enums.ComputerStatus.IN_USE);
            computerDAO.updateComputer(conn, comp);

            conn.commit();
            return booking.getBookingId();
        } catch (SQLException e) {
            if (conn != null) try { conn.rollback(); } catch (SQLException ignored) {}
            throw new BusinessException("DB_ERROR", "Lỗi CSDL khi start session: " + e.getMessage());
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); conn.close(); } catch (SQLException ignored) {}
            }
        }
    }

    public void endSession(int bookingId) throws BusinessException {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            Booking booking = bookingDAO.findById(conn, bookingId);
            if (booking == null) throw new BusinessException("NOT_FOUND", "Không tìm thấy session.");
            if (booking.getStatus() != BookingStatus.ACTIVE) throw new BusinessException("INVALID_STATE", "Phiên chơi không hoạt động.");

            java.sql.Timestamp now = new java.sql.Timestamp(System.currentTimeMillis());
            booking.setEndTime(now);
            booking.setStatus(BookingStatus.COMPLETED);

            // Tính tiền sử dụng snapshot trong bảng Booking, tuyệt đối không query giá từ Computer
            long diffInMillis = now.getTime() - booking.getStartTime().getTime();
            double hours = diffInMillis / (1000.0 * 60 * 60);
            if (hours < 0) hours = 0;
            // Charge at least for a partial time if needed, but here simple math:
            BigDecimal rate = booking.getHourlyRateSnapshot() != null ? booking.getHourlyRateSnapshot() : BigDecimal.ZERO;
            BigDecimal totalFee = rate.multiply(BigDecimal.valueOf(hours)).setScale(2, java.math.RoundingMode.HALF_UP);
            booking.setTotalFee(totalFee);

            bookingDAO.updateBooking(conn, booking);

            // Cập nhật lại status máy trống
            com.cyber.dao.IComputerDAO computerDAO = com.cyber.dao.impl.ComputerDAOImpl.getInstance();
            Computer comp = computerDAO.findById(conn, booking.getComputerId());
            if (comp != null) {
                comp.setStatus(com.cyber.model.enums.ComputerStatus.AVAILABLE);
                computerDAO.updateComputer(conn, comp);
            }

            conn.commit();
        } catch (SQLException e) {
            if (conn != null) try { conn.rollback(); } catch (SQLException ignored) {}
            throw new BusinessException("DB_ERROR", "Lỗi CSDL khi end session: " + e.getMessage());
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); conn.close(); } catch (SQLException ignored) {}
            }
        }
    }

    public void processHeartbeatSession(int intervalMs) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            List<Booking> activeBookings = bookingDAO.findAllActiveBookings(conn);
            for (Booking b : activeBookings) {
                BigDecimal hourly = b.getHourlyRateSnapshot();
                if (hourly == null) hourly = BigDecimal.ZERO;
                
                // Tiền tính cho intervalMs (1h = 3600000ms)
                BigDecimal rateForInterval = hourly.multiply(new BigDecimal(intervalMs)).divide(new BigDecimal(3600000), 2, java.math.RoundingMode.HALF_UP);
                
                boolean outOfMoney = false;
                
                conn.setAutoCommit(false);
                try {
                    int rows = userDAO.updateBalance(conn, b.getUserId(), rateForInterval.negate());
                    if (rows == 0) {
                        outOfMoney = true;
                    }
                    conn.commit();
                } catch (SQLException ex) {
                    conn.rollback();
                } finally {
                    conn.setAutoCommit(true);
                }

                if (outOfMoney) {
                    try {
                        endSession(b.getBookingId());
                        User systemAdmin = new User();
                        systemAdmin.setUserId(1);
                        systemAdmin.setUsername("SYSTEM_BACKGROUND");
                        com.cyber.service.LogService.getInstance().logStandalone(
                                com.cyber.model.enums.LogType.COMPUTER, 
                                systemAdmin, 
                                "Hệ thống tự động khóa máy do hết tiền: ID = " + b.getComputerId(),
                                b.getUserId()
                        );
                    } catch (BusinessException be) {
                        System.err.println("[Heartbeat] Error ending session (outOfMoney): " + be.getMessage());
                    }
                } else {
                    // Check for Hard-Kick (upcoming reservations)
                    try {
                        Booking nextRes = bookingDAO.findNextReservation(conn, b.getComputerId());
                        if (nextRes != null) {
                            if (System.currentTimeMillis() >= nextRes.getStartTime().getTime()) {
                                endSession(b.getBookingId());
                                User systemAdmin = new User();
                                systemAdmin.setUserId(1);
                                com.cyber.service.LogService.getInstance().logStandalone(
                                        com.cyber.model.enums.LogType.COMPUTER, 
                                        systemAdmin, 
                                        "Ngắt máy tự động để nhường máy cho lịch đặt trước #" + nextRes.getBookingId(),
                                        b.getUserId()
                                );
                                System.out.println("[Hard-Kick] Đã ngắt phiên " + b.getBookingId() + " do có người đặt trước lúc " + nextRes.getStartTime());
                            }
                        }
                    } catch (Exception ex) {
                        System.err.println("[Heartbeat] Error processing hard-kick: " + ex.getMessage());
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("[Heartbeat Error] DB Error: " + e.getMessage());
        }
    }

    // =========================================================
    // STAFF APPROVAL FLOW
    // =========================================================

    /**
     * Lấy danh sách booking đang chờ Staff phê duyệt.
     */
    public List<Booking> getPendingBookings() throws BusinessException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return bookingDAO.findPendingBookings(conn);
        } catch (SQLException e) {
            throw new BusinessException("DB_ERROR", "Lỗi lấy danh sách yêu cầu PENDING: " + e.getMessage());
        }
    }

    /**
     * Staff phê duyệt yêu cầu mở máy.
     * - PENDING: Chuyển thẳng sang ACTIVE.
     * - RESERVED: Hoàn tiền cọc rồi chuyển sang ACTIVE.
     */
    public void approveBooking(int bookingId, User staffActor) throws BusinessException {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            Booking booking = bookingDAO.findById(conn, bookingId);
            if (booking == null) throw new BusinessException("NOT_FOUND", "Không tìm thấy yêu cầu đặt máy.");

            BookingStatus oldStatus = booking.getStatus();
            if (oldStatus != BookingStatus.PENDING && oldStatus != BookingStatus.RESERVED) {
                throw new BusinessException("INVALID_STATE", "Yêu cầu này không ở trạng thái chờ duyệt.");
            }

            // Nếu là RESERVED → hoàn lại tiền cọc cho khách
            if (oldStatus == BookingStatus.RESERVED) {
                if (System.currentTimeMillis() < booking.getStartTime().getTime()) {
                    throw new BusinessException("INVALID_TIME", "Chưa đến giờ đặt trước. Vui lòng mở máy đúng theo lịch!");
                }
                
                BigDecimal deposit = booking.getTotalFee(); // Tiền cọc đã lưu trong total_fee
                if (deposit != null && deposit.compareTo(BigDecimal.ZERO) > 0) {
                    userDAO.updateBalance(conn, booking.getUserId(), deposit); // Cộng lại
                }
            }

            // Cập nhật booking: ACTIVE, start_time = thời điểm duyệt, reset total_fee, lưu staff phê duyệt
            booking.setStatus(BookingStatus.ACTIVE);
            booking.setStartTime(new java.sql.Timestamp(System.currentTimeMillis()));
            booking.setTotalFee(BigDecimal.ZERO); // Reset — heartbeat sẽ tính tiền từ đây
            booking.setStaffId(staffActor.getUserId());
            bookingDAO.updateBooking(conn, booking);

            // Chuyển máy sang IN_USE
            com.cyber.dao.IComputerDAO computerDAO = com.cyber.dao.impl.ComputerDAOImpl.getInstance();
            Computer comp = computerDAO.findById(conn, booking.getComputerId());
            if (comp != null) {
                comp.setStatus(com.cyber.model.enums.ComputerStatus.IN_USE);
                computerDAO.updateComputer(conn, comp);
            }

            // Ghi log
            String logMsg = oldStatus == BookingStatus.RESERVED
                    ? String.format("Phê duyệt mở máy (ĐẶT TRƯỚC — hoàn cọc %s): Booking #%d, Máy: %s, Khách: UserID=%d",
                            FormatUtils.formatVND(booking.getTotalFee()), bookingId,
                            comp != null ? comp.getName() : String.valueOf(booking.getComputerId()),
                            booking.getUserId())
                    : String.format("Phê duyệt mở máy: Booking #%d, Máy: %s, Khách: UserID=%d",
                            bookingId,
                            comp != null ? comp.getName() : String.valueOf(booking.getComputerId()),
                            booking.getUserId());
            LogService.getInstance().log(conn, com.cyber.model.enums.LogType.COMPUTER, staffActor, logMsg, bookingId);

            conn.commit();
        } catch (SQLException e) {
            if (conn != null) try { conn.rollback(); } catch (SQLException ignored) {}
            throw new BusinessException("DB_ERROR", "Lỗi CSDL khi phê duyệt: " + e.getMessage());
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); conn.close(); } catch (SQLException ignored) {}
            }
        }
    }

    /**
     * Staff từ chối yêu cầu mở máy.
     * - PENDING: Chỉ hủy (không có tiền cọc).
     * - RESERVED: Hoàn tiền cọc rồi hủy.
     */
    public void rejectBooking(int bookingId, User staffActor) throws BusinessException {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            Booking booking = bookingDAO.findById(conn, bookingId);
            if (booking == null) throw new BusinessException("NOT_FOUND", "Không tìm thấy yêu cầu đặt máy.");

            BookingStatus oldStatus = booking.getStatus();
            if (oldStatus != BookingStatus.PENDING && oldStatus != BookingStatus.RESERVED) {
                throw new BusinessException("INVALID_STATE", "Yêu cầu này không ở trạng thái chờ duyệt.");
            }

            // Nếu là RESERVED → hoàn lại tiền cọc cho khách
            if (oldStatus == BookingStatus.RESERVED) {
                BigDecimal deposit = booking.getTotalFee();
                if (deposit != null && deposit.compareTo(BigDecimal.ZERO) > 0) {
                    userDAO.updateBalance(conn, booking.getUserId(), deposit);
                }
            }

            booking.setStatus(BookingStatus.CANCELLED);
            bookingDAO.updateBooking(conn, booking);

            // Ghi log
            String logMsg = oldStatus == BookingStatus.RESERVED
                    ? String.format("Từ chối yêu cầu đặt trước (hoàn cọc %s): Booking #%d, Máy ID=%d, Khách: UserID=%d",
                            FormatUtils.formatVND(booking.getTotalFee()), bookingId, booking.getComputerId(), booking.getUserId())
                    : String.format("Từ chối yêu cầu mở máy: Booking #%d, Máy ID=%d, Khách: UserID=%d",
                            bookingId, booking.getComputerId(), booking.getUserId());
            LogService.getInstance().log(conn, com.cyber.model.enums.LogType.COMPUTER, staffActor, logMsg, bookingId);

            conn.commit();
        } catch (SQLException e) {
            if (conn != null) try { conn.rollback(); } catch (SQLException ignored) {}
            throw new BusinessException("DB_ERROR", "Lỗi CSDL khi từ chối: " + e.getMessage());
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); conn.close(); } catch (SQLException ignored) {}
            }
        }
    }

    // =========================================================
    // CHẾ ĐỘ 2: ĐẶT MÁY TRƯỚC (RESERVATION + DEPOSIT)
    // =========================================================

    /**
     * Khách đặt máy trước — trừ tiền cọc 1 giờ ngay lập tức.
     * Tạo booking với status RESERVED.
     */
    public int reserveComputer(int userId, int computerId, java.sql.Timestamp startTime) throws BusinessException {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            User currentUser = userDAO.findById(conn, userId);
            if (currentUser == null) throw new BusinessException("NOT_FOUND", "Không tìm thấy tài khoản.");
            if (currentUser.getStatus() == com.cyber.model.enums.UserStatus.LOCKED)
                throw new BusinessException("LOCKED", "Tài khoản bị khóa.");

            // Ràng buộc: Mỗi User chỉ được phép có tối đa 1 session (ACTIVE hoặc RESERVED)
            List<Booking> activeList = bookingDAO.findActiveBookingsByUserId(conn, userId);
            if (!activeList.isEmpty()) {
                throw new BusinessException("ALREADY_ACTIVE", "Bạn đang có máy đang sử dụng. Không thể đặt thêm lịch mới.");
            }
            List<Booking> allList = bookingDAO.findAllBookingsByUserId(conn, userId);
            boolean hasReserved = allList.stream().anyMatch(b -> b.getStatus() == BookingStatus.RESERVED);
            if (hasReserved) {
                throw new BusinessException("ALREADY_RESERVED", "Bạn đã có lịch đặt máy trước rồi.");
            }

            com.cyber.dao.IComputerDAO computerDAO = com.cyber.dao.impl.ComputerDAOImpl.getInstance();
            Computer comp = computerDAO.findById(conn, computerId);
            if (comp == null || comp.isDeleted())
                throw new BusinessException("NOT_FOUND", "Máy trạm không tồn tại hoặc đã thanh lý.");

            // Tính tiền cọc = 1 giờ chơi
            BigDecimal deposit = comp.getPricePerHour();
            if (currentUser.getBalance().compareTo(deposit) < 0) {
                throw new BusinessException("ERR_INSUFFICIENT_BALANCE",
                        String.format("Không đủ tiền cọc. Cần %s, hiện có %s.",
                                FormatUtils.formatVND(deposit), FormatUtils.formatVND(currentUser.getBalance())));
            }

            // Trừ tiền cọc ngay
            userDAO.deductBalance(conn, userId, deposit);

            // Tạo booking RESERVED — total_fee lưu số tiền cọc
            Booking booking = new Booking(0, userId, computerId,
                    startTime, null, BookingStatus.RESERVED, deposit, comp.getPricePerHour());
            int newBookingId = bookingDAO.createBooking(conn, booking);

            // Ghi log
            User systemActor = new User();
            systemActor.setUserId(userId);
            LogService.getInstance().log(conn, com.cyber.model.enums.LogType.COMPUTER, systemActor,
                    String.format("Đặt trước máy %s lúc %s — Trừ cọc %s",
                            comp.getName(), startTime.toString().substring(0, 16), FormatUtils.formatVND(deposit)),
                    newBookingId);

            conn.commit();
            return booking.getBookingId();
        } catch (SQLException e) {
            if (conn != null) try { conn.rollback(); } catch (SQLException ignored) {}
            throw new BusinessException("DB_ERROR", "Lỗi CSDL khi đặt máy trước: " + e.getMessage());
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); conn.close(); } catch (SQLException ignored) {}
            }
        }
    }

    /**
     * Tự động hủy các reservation quá hạn (gọi bởi Heartbeat).
     * Không hoàn tiền cọc — phạt khách không đến.
     */
    public void processOverdueReservations(int overdueMinutes) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            List<Booking> overdueList = bookingDAO.findOverdueReservations(conn, overdueMinutes);
            for (Booking b : overdueList) {
                conn.setAutoCommit(false);
                try {
                    b.setStatus(BookingStatus.CANCELLED);
                    bookingDAO.updateBooking(conn, b);

                    User systemAdmin = new User();
                    systemAdmin.setUserId(1);
                    systemAdmin.setUsername("SYSTEM_BACKGROUND");
                    LogService.getInstance().log(conn, com.cyber.model.enums.LogType.COMPUTER, systemAdmin,
                            String.format("Thu cọc do quá hạn %d phút: Booking #%d, Máy: %s, Khách: %s, Cọc: %s",
                                    overdueMinutes, b.getBookingId(),
                                    b.getComputerName() != null ? b.getComputerName() : String.valueOf(b.getComputerId()),
                                    b.getUserName() != null ? b.getUserName() : String.valueOf(b.getUserId()),
                                    FormatUtils.formatVND(b.getTotalFee())),
                            b.getBookingId());

                    conn.commit();
                    System.out.println("[Reservation] Hủy đặt trước quá hạn: Booking #" + b.getBookingId());
                } catch (SQLException ex) {
                    conn.rollback();
                    System.err.println("[Reservation Error] " + ex.getMessage());
                } finally {
                    conn.setAutoCommit(true);
                }
            }
        } catch (SQLException e) {
            System.err.println("[Reservation Error] DB Error: " + e.getMessage());
        }
    }

    public Booking getNextReservation(int computerId) throws BusinessException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return bookingDAO.findNextReservation(conn, computerId);
        } catch (SQLException e) {
            throw new BusinessException("DB_ERROR", "Lỗi tìm lịch đặt tiếp theo: " + e.getMessage());
        }
    }
}