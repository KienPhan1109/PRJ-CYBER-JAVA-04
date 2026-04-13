package com.cyber.dao.impl;

import com.cyber.dao.IFbOrderDAO;
import com.cyber.model.FbOrder;
import com.cyber.model.enums.FbOrderStatus;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class FbOrderDAOImpl implements IFbOrderDAO {
    private static FbOrderDAOImpl instance;
    private FbOrderDAOImpl() {}

    public static synchronized FbOrderDAOImpl getInstance() {
        if (instance == null) {
            instance = new FbOrderDAOImpl();
        }
        return instance;
    }

    @Override
    public int createOrder(Connection conn, FbOrder order) throws SQLException {
        String sql = "INSERT INTO fb_orders (user_id, booking_id, status, total_amount) VALUES (?, ?, 'PENDING', ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, order.getUserId());
            if (order.getBookingId() != null) {
                stmt.setInt(2, order.getBookingId());
            } else {
                stmt.setNull(2, java.sql.Types.INTEGER);
            }
            stmt.setBigDecimal(3, order.getTotalAmount());
            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        throw new SQLException("Không thể tạo FbOrder.");
    }

    @Override
    public List<FbOrder> findAllOrdersByStatus(Connection conn, FbOrderStatus status) throws SQLException {
        List<FbOrder> orders = new ArrayList<>();
        String sql = "SELECT * FROM fb_orders WHERE status = ? ORDER BY created_at ASC";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, status.name());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    int bookingIdVal = rs.getInt("booking_id");
                    Integer bId = rs.wasNull() ? null : bookingIdVal;
                    
                    String statusStr = rs.getString("status");
                    FbOrderStatus s = FbOrderStatus.valueOf(statusStr.toUpperCase());
                    
                    FbOrder order = new FbOrder(
                        rs.getInt("user_id"),
                        bId,
                        s,
                        rs.getBigDecimal("total_amount")
                    );
                    int sId = rs.getInt("staff_id");
                    if (!rs.wasNull()) order.setStaffId(sId);
                    order.setOrderId(rs.getInt("order_id"));
                    orders.add(order);
                }
            }
        }
        return orders;
    }

    @Override
    public List<FbOrder> findAllActiveOrdersWithDetails(Connection conn) throws SQLException {
        String sql = "SELECT o.*, u.full_name as user_name, c.name as computer_name " +
                     "FROM fb_orders o " +
                     "JOIN users u ON o.user_id = u.user_id " +
                     "LEFT JOIN bookings b ON o.booking_id = b.booking_id " +
                     "LEFT JOIN computers c ON b.computer_id = c.computer_id " +
                     "WHERE o.status IN ('PENDING', 'PREPARING') " +
                     "ORDER BY o.created_at ASC";
        return executeActiveOrdersQuery(conn, sql, null);
    }

    @Override
    public List<FbOrder> findActiveOrdersByUserIdWithDetails(Connection conn, int userId) throws SQLException {
        String sql = "SELECT o.*, u.full_name as user_name, c.name as computer_name " +
                     "FROM fb_orders o " +
                     "JOIN users u ON o.user_id = u.user_id " +
                     "LEFT JOIN bookings b ON o.booking_id = b.booking_id " +
                     "LEFT JOIN computers c ON b.computer_id = c.computer_id " +
                     "WHERE o.status IN ('PENDING', 'PREPARING') AND o.user_id = ? " +
                     "ORDER BY o.created_at ASC";
        return executeActiveOrdersQuery(conn, sql, userId);
    }

    private List<FbOrder> executeActiveOrdersQuery(Connection conn, String sql, Integer userId) throws SQLException {
        List<FbOrder> orders = new ArrayList<>();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            if (userId != null) {
                stmt.setInt(1, userId);
            }
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    int bookingIdVal = rs.getInt("booking_id");
                    Integer bId = rs.wasNull() ? null : bookingIdVal;
                    
                    String statusStr = rs.getString("status");
                    FbOrderStatus s = FbOrderStatus.valueOf(statusStr.toUpperCase());

                    FbOrder order = new FbOrder(
                        rs.getInt("user_id"),
                        bId,
                        s,
                        rs.getBigDecimal("total_amount")
                    );
                    int sId = rs.getInt("staff_id");
                    if (!rs.wasNull()) order.setStaffId(sId);
                    order.setOrderId(rs.getInt("order_id"));
                    order.setUserName(rs.getString("user_name"));
                    String compName = rs.getString("computer_name");
                    order.setComputerName(compName != null ? compName : "Không có");
                    orders.add(order);
                }
            }
        }
        return orders;
    }

    @Override
    public List<FbOrder> findAllOrdersByUserIdWithDetails(Connection conn, int userId) throws SQLException {
        String sql = "SELECT o.*, u.full_name as user_name, c.name as computer_name " +
                     "FROM fb_orders o " +
                     "JOIN users u ON o.user_id = u.user_id " +
                     "LEFT JOIN bookings b ON o.booking_id = b.booking_id " +
                     "LEFT JOIN computers c ON b.computer_id = c.computer_id " +
                     "WHERE o.user_id = ? " +
                     "ORDER BY o.created_at DESC";
        return executeActiveOrdersQuery(conn, sql, userId);
    }

    @Override
    public void updateOrderStatus(Connection conn, int orderId, FbOrderStatus newStatus, Integer staffId) throws SQLException {
        String sql = "UPDATE fb_orders SET status = ?, staff_id = COALESCE(?, staff_id) WHERE order_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, newStatus.name());
            if (staffId != null) {
                stmt.setInt(2, staffId);
            } else {
                stmt.setNull(2, java.sql.Types.INTEGER);
            }
            stmt.setInt(3, orderId);
            stmt.executeUpdate();
        }
    }

    @Override
    public FbOrder findOrderById(Connection conn, int orderId) throws SQLException {
        String sql = "SELECT * FROM fb_orders WHERE order_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, orderId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    int bookingIdVal = rs.getInt("booking_id");
                    Integer bId = rs.wasNull() ? null : bookingIdVal;
                    String statusStr = rs.getString("status");
                    FbOrderStatus s = FbOrderStatus.valueOf(statusStr.toUpperCase());
                    
                    FbOrder order = new FbOrder(
                        rs.getInt("user_id"),
                        bId,
                        s,
                        rs.getBigDecimal("total_amount")
                    );
                    int sId = rs.getInt("staff_id");
                    if (!rs.wasNull()) order.setStaffId(sId);
                    order.setOrderId(rs.getInt("order_id"));
                    return order;
                }
            }
        }
        return null;
    }
}