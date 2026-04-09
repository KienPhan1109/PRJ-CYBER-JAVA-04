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

    public int bookComputer(int userId, Booking booking) throws BusinessException {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            User currentUser = userDAO.findById(conn, userId);
            if (currentUser == null) throw new RuntimeException("ERR_USER_NOT_FOUND");
            if (currentUser.getStatus() == com.cyber.model.enums.UserStatus.LOCKED) throw new RuntimeException("ERR_USER_LOCKED");

            if (!bookingDAO.isComputerAvailable(conn, booking.getComputerId(), booking.getStartTime(), booking.getEndTime())) {
                throw new RuntimeException("Máy trạm này đã được đặt trong khoảng thời gian trên.");
            }

            BigDecimal totalCost = booking.getTotalFee() != null ? booking.getTotalFee() : BigDecimal.ZERO;
            if (currentUser.getBalance().compareTo(totalCost) < 0) {
                throw new BusinessException("ERR_INSUFFICIENT_BALANCE", "Bạn không đủ tiền để thuê máy. Vui lòng nạp thêm!");
            }
            userDAO.deductBalance(conn, userId, totalCost);

            booking.setUserId(userId);
            int newBookingId = bookingDAO.createBooking(conn, booking);
            
            // Note: Since computer is booked now, should we change computer status to IN_USE?
            com.cyber.dao.IComputerDAO computerDAO = com.cyber.dao.impl.ComputerDAOImpl.getInstance();
            Computer comp = computerDAO.findById(conn, booking.getComputerId());
            if (comp != null) {
                comp.setStatus(com.cyber.model.enums.ComputerStatus.IN_USE);
                computerDAO.updateComputer(conn, comp);
            }
            
            conn.commit();
            return newBookingId;
        } catch (SQLException e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException rollbackEx) {}
            }
            throw new BusinessException("DB_ERROR", "Lỗi CSDL khi đặt máy: " + e.getMessage());
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
}