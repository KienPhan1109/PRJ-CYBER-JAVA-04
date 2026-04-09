package com.cyber.dao.impl;

import com.cyber.dao.IFbOrderDetailDAO;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Cài đặt DAO cho bảng fb_order_details (Phase 2).
 * Lưu toàn bộ thông tin cấu hình Decorator (dạng JSON) để có thể load lại sau.
 */
public class FbOrderDetailDAOImpl implements IFbOrderDetailDAO {

    private static FbOrderDetailDAOImpl instance;

    private FbOrderDetailDAOImpl() {}

    public static synchronized FbOrderDetailDAOImpl getInstance() {
        if (instance == null) {
            instance = new FbOrderDetailDAOImpl();
        }
        return instance;
    }

    // -------------------------------------------------------
    // SQL
    // -------------------------------------------------------
    private static final String SQL_SAVE =
            "INSERT INTO fb_order_details " +
            "(order_id, menu_item_id, quantity, unit_price_snapshot, item_name_snapshot, item_description, item_config_json, discount_applied, discount_strategy_name) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String SQL_FIND_BY_ORDER =
            "SELECT d.* " +
            "FROM fb_order_details d " +
            "WHERE d.order_id = ? " +
            "ORDER BY d.detail_id";

    private static final String SQL_CALC_TOTAL =
            "SELECT SUM(quantity * unit_price_snapshot) as total " +
            "FROM fb_order_details " +
            "WHERE order_id = ?";

    // -------------------------------------------------------
    // Implementations
    // -------------------------------------------------------

    @Override
    public void insertOrderDetail(Connection conn, 
                                  int orderId, 
                                  int menuItemId, 
                                  int quantity, 
                                  BigDecimal unitPriceSnapshot, 
                                  String itemNameSnapshot, 
                                  String itemDescription, 
                                  String itemConfigJson, 
                                  BigDecimal discountApplied, 
                                  String discountStrategyName) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SQL_SAVE)) {
            ps.setInt       (1, orderId);
            ps.setInt       (2, menuItemId);
            ps.setInt       (3, quantity);
            ps.setBigDecimal(4, unitPriceSnapshot);
            ps.setString    (5, itemNameSnapshot);
            ps.setString    (6, itemDescription);
            ps.setString    (7, itemConfigJson);
            ps.setBigDecimal(8, discountApplied != null ? discountApplied : BigDecimal.ZERO);
            ps.setString    (9, discountStrategyName);
            ps.executeUpdate();
        }
    }


    @Override
    public List<Map<String, Object>> findDetailsByOrderId(Connection conn, int orderId) throws SQLException {
        List<Map<String, Object>> result = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_ORDER)) {
            ps.setInt(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    row.put("detail_id",             rs.getInt       ("detail_id"));
                    row.put("order_id",              rs.getInt       ("order_id"));
                    row.put("menu_item_id",          rs.getInt       ("menu_item_id"));
                    row.put("item_name",             rs.getString    ("item_name_snapshot"));
                    row.put("quantity",              rs.getInt       ("quantity"));
                    row.put("unit_price_snapshot",   rs.getBigDecimal("unit_price_snapshot"));
                    row.put("item_description",      rs.getString    ("item_description"));
                    row.put("item_config_json",      rs.getString    ("item_config_json"));
                    row.put("discount_applied",      rs.getBigDecimal("discount_applied"));
                    row.put("discount_strategy_name",rs.getString    ("discount_strategy_name"));
                    row.put("created_at",            rs.getTimestamp ("created_at"));
                    result.add(row);
                }
            }
        }
        return result;
    }

    @Override
    public BigDecimal calculateTotalOrder(Connection conn, int orderId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SQL_CALC_TOTAL)) {
            ps.setInt(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    BigDecimal total = rs.getBigDecimal("total");
                    return total != null ? total : BigDecimal.ZERO;
                }
            }
        }
        return BigDecimal.ZERO;
    }
}
