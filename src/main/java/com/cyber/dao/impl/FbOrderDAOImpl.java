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
        String sql = "INSERT INTO fb_orders (booking_id, status, total_amount) VALUES (?, 'PENDING', ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, order.getBookingId());
            stmt.setBigDecimal(2, order.getTotalAmount());
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
}