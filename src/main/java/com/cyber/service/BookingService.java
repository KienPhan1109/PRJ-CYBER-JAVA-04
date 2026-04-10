package com.cyber.service;
import com.cyber.connection.DatabaseConnection;
import com.cyber.dao.*;
import com.cyber.dao.impl.*;
import com.cyber.exception.*;
import com.cyber.model.*;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class BookingService {

    // Singleton Pattern
    private static BookingService instance;
    private IBookingDAO bookingDAO;
    private IUserDAO userDAO;

    private BookingService() {
        this.bookingDAO = BookingDAOImpl.getInstance();
        this.userDAO = UserDAOImpl.getInstance();
    }

    public static synchronized BookingService getInstance() {
        if (instance == null) {
            instance = new BookingService();
        }
        return instance;
    }

    /**
     * Gửi yêu cầu đặt máy — tạo booking với trạng thái PENDING.
     * Tiền CHƯA bị trừ, máy CHƯA chuyển sang IN_USE.
     * Staff phải gọi approveBooking() để kích hoạt phiên chơi.
     */
    public int bookComputer(int userId, Booking booking) throws BusinessException {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            User currentUser = userDAO.findById(conn, userId);
            if (currentUser == null) throw new RuntimeException("ERR_USER_NOT_FOUND");
            if (currentUser.getStatus() == com.cyber.model.enums.UserStatus.LOCKED) throw new RuntimeException("ERR_USER_LOCKED");

            // Đặt trạng thái PENDING — chờ Staff phê duyệt
            booking.setStatus("PENDING");
            booking.setUserId(userId);
            int newBookingId = bookingDAO.createBooking(conn, booking);

            conn.commit();
            return newBookingId;
        } catch (SQLException e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException rollbackEx) {}
            }
            throw new BusinessException("DB_ERROR", "Lỗi CSDL khi gửi yêu cầu đặt máy: " + e.getMessage());
        } catch (BusinessException be) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException rollbackEx) {}
            }
            throw be;
        } catch (RuntimeException re) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException rollbackEx) {}
            }
            throw new BusinessException("ERR_BUSINESS", re.getMessage().startsWith("ERR_") ? "Lỗi: " + re.getMessage() : re.getMessage());
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException closeEx) {}
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
                    "ACTIVE", BigDecimal.ZERO, currentRate);
            
            int newBookingId = bookingDAO.createBooking(conn, booking);

            comp.setStatus(com.cyber.model.enums.ComputerStatus.IN_USE);
            computerDAO.updateComputer(conn, comp);

            conn.commit();
            return newBookingId;
        } catch (SQLException e) {
            if (conn != null) try { conn.rollback(); } catch (SQLException ex) {}
            throw new BusinessException("DB_ERROR", "Lỗi CSDL khi start session: " + e.getMessage());
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException ex) {}
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
            if (!"ACTIVE".equals(booking.getStatus())) throw new BusinessException("INVALID_STATE", "Phiên chơi không hoạt động.");

            java.sql.Timestamp now = new java.sql.Timestamp(System.currentTimeMillis());
            booking.setEndTime(now);
            booking.setStatus("COMPLETED");

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
            if (conn != null) try { conn.rollback(); } catch (SQLException ex) {}
            throw new BusinessException("DB_ERROR", "Lỗi CSDL khi end session: " + e.getMessage());
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException ex) {}
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
                        com.cyber.service.LogService.getInstance().logStandalone(
                                com.cyber.model.enums.LogType.COMPUTER, 
                                systemAdmin, 
                                "Hệ thống tự động khóa máy do hết tiền: ID = " + b.getComputerId(),
                                b.getUserId()
                        );
                    } catch (BusinessException be) {
                        System.err.println("[Heartbeat] Error ending session: " + be.getMessage());
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
     * 1. Chuyển booking sang ACTIVE, cập nhật start_time = NOW.
     * 2. Chuyển computer sang IN_USE.
     * 3. Ghi log hành động.
     */
    public void approveBooking(int bookingId, User staffActor) throws BusinessException {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            Booking booking = bookingDAO.findById(conn, bookingId);
            if (booking == null) throw new BusinessException("NOT_FOUND", "Không tìm thấy yêu cầu đặt máy.");
            if (!"PENDING".equals(booking.getStatus())) throw new BusinessException("INVALID_STATE", "Yêu cầu này không ở trạng thái chờ duyệt.");

            // Cập nhật booking: ACTIVE, start_time = thời điểm duyệt
            booking.setStatus("ACTIVE");
            booking.setStartTime(new java.sql.Timestamp(System.currentTimeMillis()));
            bookingDAO.updateBooking(conn, booking);

            // Chuyển máy sang IN_USE
            com.cyber.dao.IComputerDAO computerDAO = com.cyber.dao.impl.ComputerDAOImpl.getInstance();
            Computer comp = computerDAO.findById(conn, booking.getComputerId());
            if (comp != null) {
                comp.setStatus(com.cyber.model.enums.ComputerStatus.IN_USE);
                computerDAO.updateComputer(conn, comp);
            }

            // Ghi log
            LogService.getInstance().log(conn, com.cyber.model.enums.LogType.COMPUTER, staffActor,
                    String.format("Phê duyệt mở máy: Booking #%d, Máy: %s, Khách: UserID=%d",
                            bookingId,
                            comp != null ? comp.getName() : String.valueOf(booking.getComputerId()),
                            booking.getUserId()),
                    bookingId);

            conn.commit();
        } catch (SQLException e) {
            if (conn != null) try { conn.rollback(); } catch (SQLException ex) {}
            throw new BusinessException("DB_ERROR", "Lỗi CSDL khi phê duyệt: " + e.getMessage());
        } catch (BusinessException be) {
            if (conn != null) try { conn.rollback(); } catch (SQLException ex) {}
            throw be;
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); conn.close(); } catch (SQLException ex) {}
            }
        }
    }

    /**
     * Staff từ chối yêu cầu mở máy.
     * 1. Chuyển booking sang CANCELLED.
     * 2. Ghi log hành động.
     */
    public void rejectBooking(int bookingId, User staffActor) throws BusinessException {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            Booking booking = bookingDAO.findById(conn, bookingId);
            if (booking == null) throw new BusinessException("NOT_FOUND", "Không tìm thấy yêu cầu đặt máy.");
            if (!"PENDING".equals(booking.getStatus())) throw new BusinessException("INVALID_STATE", "Yêu cầu này không ở trạng thái chờ duyệt.");

            booking.setStatus("CANCELLED");
            bookingDAO.updateBooking(conn, booking);

            // Ghi log
            LogService.getInstance().log(conn, com.cyber.model.enums.LogType.COMPUTER, staffActor,
                    String.format("Từ chối yêu cầu mở máy: Booking #%d, Máy ID=%d, Khách: UserID=%d",
                            bookingId, booking.getComputerId(), booking.getUserId()),
                    bookingId);

            conn.commit();
        } catch (SQLException e) {
            if (conn != null) try { conn.rollback(); } catch (SQLException ex) {}
            throw new BusinessException("DB_ERROR", "Lỗi CSDL khi từ chối: " + e.getMessage());
        } catch (BusinessException be) {
            if (conn != null) try { conn.rollback(); } catch (SQLException ex) {}
            throw be;
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); conn.close(); } catch (SQLException ex) {}
            }
        }
    }
}