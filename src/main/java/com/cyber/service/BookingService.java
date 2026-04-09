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
}