package com.cyber.dao.impl;
import com.cyber.dao.IFbOrderDAO;
import com.cyber.model.FbOrder;
import com.cyber.model.OrderDetail;
import java.sql.*;
import java.util.List;

public class FbOrderDAOImpl implements IFbOrderDAO {

    // Singleton Pattern
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
    public void createOrderDetails(Connection conn, List<OrderDetail> details) throws SQLException {
        String sql = "INSERT INTO order_details (order_id, item_id, quantity, unit_price) VALUES (?, ?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (OrderDetail detail : details) {
                stmt.setInt(1, detail.getOrderId());
                stmt.setInt(2, detail.getItemId());
                stmt.setInt(3, detail.getQuantity());
                stmt.setBigDecimal(4, detail.getUnitPrice());
                stmt.addBatch();
            }
            stmt.executeBatch();
        }
    }

    @Override
    public boolean hasDependentOrders(Connection conn, int itemId) throws SQLException {
        String sql = "SELECT 1 FROM order_details WHERE item_id = ? LIMIT 1";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, itemId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    @Override
    public List<FbOrder> findAllOrdersByStatus(Connection conn, String status) throws SQLException {
        List<FbOrder> orders = new java.util.ArrayList<>();
        String sql = "SELECT * FROM fb_orders WHERE status = ? ORDER BY created_at ASC";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, status);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    int bookingIdVal = rs.getInt("booking_id");
                    Integer bId = rs.wasNull() ? null : bookingIdVal;
                    FbOrder order = new FbOrder(
                        rs.getInt("user_id"),
                        bId,
                        rs.getString("status"),
                        rs.getBigDecimal("total_amount")
                    );
                    order.setOrderId(rs.getInt("order_id"));
                    // User ID if needed in the future
                    orders.add(order);
                }
            }
        }
        return orders;
    }

    @Override
    public void updateOrderStatus(Connection conn, int orderId, String newStatus) throws SQLException {
        String sql = "UPDATE fb_orders SET status = ? WHERE order_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, newStatus);
            stmt.setInt(2, orderId);
            stmt.executeUpdate();
        }
    }
}