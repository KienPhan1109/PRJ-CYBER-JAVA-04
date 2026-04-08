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
    private IFbOrderDAO orderDAO;
    private IUserDAO userDAO;
    private IServiceItemDAO itemDAO;

    private BookingService() {
        this.bookingDAO = BookingDAOImpl.getInstance();
        this.orderDAO = FbOrderDAOImpl.getInstance();
        this.userDAO = UserDAOImpl.getInstance();
        this.itemDAO = ServiceItemDAOImpl.getInstance();
    }

    public static synchronized BookingService getInstance() {
        if (instance == null) {
            instance = new BookingService();
        }
        return instance;
    }

    public void bookComputerWithFood(int userId, Booking booking, FbOrder order, List<OrderDetail> orderDetails) throws BusinessException {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            User currentUser = userDAO.findById(conn, userId);
            if (currentUser == null) throw new RuntimeException("ERR_USER_NOT_FOUND");

            if (!bookingDAO.isComputerAvailable(conn, booking.getComputerId(), booking.getStartTime(), booking.getEndTime())) {
                throw new RuntimeException("ERR_COMPUTER_NOT_AVAILABLE");
            }

            BigDecimal totalCost = BigDecimal.ZERO;
            if (booking.getTotalFee() != null) totalCost = totalCost.add(booking.getTotalFee());
            if (order != null && order.getTotalAmount() != null) totalCost = totalCost.add(order.getTotalAmount());

            if (currentUser.getBalance().compareTo(totalCost) < 0) {
                throw new RuntimeException("ERR_INSUFFICIENT_BALANCE");
            }
            userDAO.deductBalance(conn, userId, totalCost);

            booking.setUserId(userId);
            int newBookingId = bookingDAO.createBooking(conn, booking);

            if (order != null && orderDetails != null && !orderDetails.isEmpty()) {
                order.setBookingId(newBookingId);
                int newOrderId = orderDAO.createOrder(conn, order);

                for (OrderDetail detail : orderDetails) {
                    detail.setOrderId(newOrderId);
                    ServiceItem itemInfo = itemDAO.findById(conn, detail.getItemId());
                    if (itemInfo == null || itemInfo.getStockQuantity() < detail.getQuantity()) {
                        throw new RuntimeException("ERR_OUT_OF_STOCK");
                    }
                    itemDAO.deductStock(conn, detail.getItemId(), detail.getQuantity());
                }
                orderDAO.createOrderDetails(conn, orderDetails);
            }
            conn.commit();
        } catch (SQLException e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException rollbackEx) {}
            }
            throw new BusinessException("ERR_SQL_EXCEPTION", "Lỗi CSDL khi thao tác: " + e.getMessage());
        } catch (RuntimeException be) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException rollbackEx) {}
            }
            throw new BusinessException("ERR_BUSINESS", be.getMessage());
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException closeEx) {}
            }
        }
    }
}