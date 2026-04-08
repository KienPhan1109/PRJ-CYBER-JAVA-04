package com.cyber.service;

import com.cyber.connection.DatabaseConnection;
import com.cyber.dao.IFbOrderDAO;
import com.cyber.dao.impl.FbOrderDAOImpl;
import com.cyber.exception.BusinessException;
import com.cyber.model.FbOrder;
import com.cyber.model.User;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class FbOrderService {
    private static FbOrderService instance;
    private final IFbOrderDAO orderDAO;

    private FbOrderService() {
        this.orderDAO = FbOrderDAOImpl.getInstance();
    }

    public static synchronized FbOrderService getInstance() {
        if (instance == null) {
            instance = new FbOrderService();
        }
        return instance;
    }

    // Method for Staff
    public List<FbOrder> getPendingOrders() throws BusinessException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return orderDAO.findAllOrdersByStatus(conn, "PENDING");
        } catch (SQLException e) {
            throw new BusinessException("DB_ERROR", "Lỗi lấy danh sách Order: " + e.getMessage());
        }
    }

    public void updateOrderStatus(int orderId, String newStatus) throws BusinessException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            orderDAO.updateOrderStatus(conn, orderId, newStatus);
        } catch (SQLException e) {
            throw new BusinessException("DB_ERROR", "Lỗi cập nhật Order: " + e.getMessage());
        }
    }

    public void orderFoodIndependently(int userId, Integer bookingId, FbOrder order, List<com.cyber.model.OrderDetail> orderDetails) throws BusinessException {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            com.cyber.dao.IUserDAO userDAO = com.cyber.dao.impl.UserDAOImpl.getInstance();
            com.cyber.dao.IServiceItemDAO itemDAO = com.cyber.dao.impl.ServiceItemDAOImpl.getInstance();

            User currentUser = userDAO.findById(conn, userId);
            if (currentUser == null) throw new BusinessException("ERR_USER_NOT_FOUND", "Không tìm thấy người dùng.");
            if (currentUser.getStatus() == com.cyber.model.enums.UserStatus.LOCKED) throw new BusinessException("ERR_USER_LOCKED", "Tài khoản đang bị khóa.");

            BigDecimal totalCost = order.getTotalAmount() != null ? order.getTotalAmount() : BigDecimal.ZERO;
            if (currentUser.getBalance().compareTo(totalCost) < 0) {
                throw new BusinessException("ERR_INSUFFICIENT_BALANCE", "Bạn không đủ tiền để gọi thêm đồ ăn. Cần: " + com.cyber.util.FormatUtils.formatVND(totalCost));
            }
            userDAO.deductBalance(conn, userId, totalCost);

            order.setUserId(userId);
            order.setBookingId(bookingId);
            int newOrderId = orderDAO.createOrder(conn, order);

            for (com.cyber.model.OrderDetail detail : orderDetails) {
                detail.setOrderId(newOrderId);
                com.cyber.model.ServiceItem itemInfo = itemDAO.findById(conn, detail.getItemId());
                if (itemInfo == null || itemInfo.getStockQuantity() < detail.getQuantity()) {
                    throw new BusinessException("ERR_OUT_OF_STOCK", "Xin lỗi, không có đủ hàng cho món ID " + detail.getItemId());
                }
                itemDAO.deductStock(conn, detail.getItemId(), detail.getQuantity());
            }
            orderDAO.createOrderDetails(conn, orderDetails);
            
            conn.commit();
        } catch (SQLException e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException rollbackEx) {}
            }
            throw new BusinessException("DB_ERROR", "Lỗi CSDL khi lên đơn: " + e.getMessage());
        } catch (BusinessException be) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException rollbackEx) {}
            }
            throw be;
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
