package com.cyber.dao.impl;

import com.cyber.dao.IFbOptionDAO;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Cài đặt DAO cho bảng fb_toppings và fb_item_options.
 * Dùng Map<String, Object> để linh hoạt — tránh proliferating POJOs cho các bảng cấu hình.
 */
public class FbOptionDAOImpl implements IFbOptionDAO {

    private static FbOptionDAOImpl instance;

    private FbOptionDAOImpl() {}

    public static synchronized FbOptionDAOImpl getInstance() {
        if (instance == null) {
            instance = new FbOptionDAOImpl();
        }
        return instance;
    }

    // -------------------------------------------------------
    // SQL
    // -------------------------------------------------------
    private static final String SQL_ALL_TOPPINGS =
            "SELECT topping_id, name, extra_price FROM fb_toppings WHERE is_active = TRUE ORDER BY name";

    private static final String SQL_TOPPING_BY_ID =
            "SELECT topping_id, name, extra_price FROM fb_toppings WHERE topping_id = ? AND is_active = TRUE";

    private static final String SQL_OPTIONS_BY_ITEM =
            "SELECT option_id, option_type, option_label, extra_price " +
            "FROM fb_item_options WHERE menu_item_id = ? ORDER BY option_type, option_label";

    private static final String SQL_CREATE_TOPPING =
            "INSERT INTO fb_toppings (name, extra_price) VALUES (?, ?)";

    private static final String SQL_UPDATE_TOPPING =
            "UPDATE fb_toppings SET name=?, extra_price=? WHERE topping_id=?";

    private static final String SQL_DEACTIVATE_TOPPING =
            "UPDATE fb_toppings SET is_active=FALSE WHERE topping_id=?";

    // -------------------------------------------------------
    // Implementations
    // -------------------------------------------------------

    @Override
    public List<Map<String, Object>> findAllToppings(Connection conn) throws SQLException {
        List<Map<String, Object>> result = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(SQL_ALL_TOPPINGS);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.add(toToppingMap(rs));
            }
        }
        return result;
    }

    @Override
    public Map<String, Object> findToppingById(Connection conn, int toppingId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SQL_TOPPING_BY_ID)) {
            ps.setInt(1, toppingId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return toToppingMap(rs);
            }
        }
        return null;
    }

    @Override
    public List<Map<String, Object>> findOptionsByMenuItemId(Connection conn, int menuItemId) throws SQLException {
        List<Map<String, Object>> result = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(SQL_OPTIONS_BY_ITEM)) {
            ps.setInt(1, menuItemId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    row.put("option_id",    rs.getInt("option_id"));
                    row.put("option_type",  rs.getString("option_type"));
                    row.put("option_label", rs.getString("option_label"));
                    row.put("extra_price",  rs.getBigDecimal("extra_price"));
                    result.add(row);
                }
            }
        }
        return result;
    }

    @Override
    public int createTopping(Connection conn, String name, BigDecimal extraPrice) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SQL_CREATE_TOPPING, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString    (1, name);
            ps.setBigDecimal(2, extraPrice);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        }
        return -1;
    }

    @Override
    public void updateTopping(Connection conn, int toppingId, String name, BigDecimal extraPrice) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SQL_UPDATE_TOPPING)) {
            ps.setString    (1, name);
            ps.setBigDecimal(2, extraPrice);
            ps.setInt       (3, toppingId);
            ps.executeUpdate();
        }
    }

    @Override
    public void deactivateTopping(Connection conn, int toppingId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SQL_DEACTIVATE_TOPPING)) {
            ps.setInt(1, toppingId);
            ps.executeUpdate();
        }
    }

    // -------------------------------------------------------
    // Helpers
    // -------------------------------------------------------
    private Map<String, Object> toToppingMap(ResultSet rs) throws SQLException {
        Map<String, Object> map = new HashMap<>();
        map.put("topping_id",  rs.getInt("topping_id"));
        map.put("name",        rs.getString("name"));
        map.put("extra_price", rs.getBigDecimal("extra_price"));
        return map;
    }
}
