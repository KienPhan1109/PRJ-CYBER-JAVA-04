package com.cyber.dao.impl;

import com.cyber.dao.IFbMenuItemDAO;
import com.cyber.domain.fb.FbMenuItem;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Cài đặt DAO cho bảng fb_menu_items.
 * Singleton Pattern, dùng PreparedStatement chống SQL Injection.
 * Mọi Connection đều được truyền từ Service (quản lý transaction tại Service layer).
 */
public class FbMenuItemDAOImpl implements IFbMenuItemDAO {

    private static FbMenuItemDAOImpl instance;

    private FbMenuItemDAOImpl() {}

    public static synchronized FbMenuItemDAOImpl getInstance() {
        if (instance == null) {
            instance = new FbMenuItemDAOImpl();
        }
        return instance;
    }

    // -------------------------------------------------------
    // SQL Constants
    // -------------------------------------------------------
    private static final String SQL_FIND_ALL_ACTIVE =
            "SELECT m.*, c.category_name " +
            "FROM fb_menu_items m " +
            "JOIN fb_categories c ON m.category_id = c.category_id " +
            "WHERE m.status IN ('ACTIVE', 'OUT_OF_STOCK') AND m.is_deleted = 0 " +
            "ORDER BY c.category_name, m.name";

    private static final String SQL_FIND_ALL =
            "SELECT m.*, c.category_name " +
            "FROM fb_menu_items m " +
            "JOIN fb_categories c ON m.category_id = c.category_id " +
            "ORDER BY c.category_name, m.name";

    private static final String SQL_FIND_BY_ID =
            "SELECT m.*, c.category_name " +
            "FROM fb_menu_items m " +
            "JOIN fb_categories c ON m.category_id = c.category_id " +
            "WHERE m.menu_item_id = ?";

    private static final String SQL_FIND_BY_CATEGORY =
            "SELECT m.*, c.category_name " +
            "FROM fb_menu_items m " +
            "JOIN fb_categories c ON m.category_id = c.category_id " +
            "WHERE m.category_id = ? AND m.status = 'ACTIVE' AND m.is_deleted = 0 " +
            "ORDER BY m.name";

    private static final String SQL_CREATE =
            "INSERT INTO fb_menu_items " +
            "(category_id, name, description, base_price, stock_quantity, prep_time_minutes, item_tags, availability, temperature_level, status) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String SQL_UPDATE =
            "UPDATE fb_menu_items SET " +
            "category_id=?, name=?, description=?, base_price=?, stock_quantity=?, " +
            "prep_time_minutes=?, item_tags=?, availability=?, temperature_level=?, status=? " +
            "WHERE menu_item_id=?";

    private static final String SQL_SOFT_DELETE =
            "UPDATE fb_menu_items SET is_deleted = 1 WHERE menu_item_id=?";

    private static final String SQL_DEDUCT_STOCK =
            "UPDATE fb_menu_items " +
            "SET stock_quantity = stock_quantity - ?, " +
            "    status = CASE WHEN stock_quantity - ? = 0 THEN 'OUT_OF_STOCK' ELSE status END " +
            "WHERE menu_item_id = ? AND stock_quantity >= ?";

    // -------------------------------------------------------
    // Implementations
    // -------------------------------------------------------

    @Override
    public List<FbMenuItem> getAllActiveItems(Connection conn) throws SQLException {
        List<FbMenuItem> result = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(SQL_FIND_ALL_ACTIVE);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.add(mapRow(rs));
            }
        }
        return result;
    }

    @Override
    public List<FbMenuItem> getAllItemsForAdmin(Connection conn) throws SQLException {
        List<FbMenuItem> result = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(SQL_FIND_ALL);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.add(mapRow(rs));
            }
        }
        return result;
    }

    @Override
    public FbMenuItem findById(Connection conn, int menuItemId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_ID)) {
            ps.setInt(1, menuItemId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }

    @Override
    public List<FbMenuItem> findByCategoryId(Connection conn, int categoryId) throws SQLException {
        List<FbMenuItem> result = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_CATEGORY)) {
            ps.setInt(1, categoryId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapRow(rs));
                }
            }
        }
        return result;
    }

    @Override
    public int create(Connection conn, FbMenuItem item) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SQL_CREATE, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt   (1, item.getCategoryId());
            ps.setString(2, item.getName());
            ps.setString(3, item.getDescription());
            ps.setBigDecimal(4, item.getBasePrice());
            ps.setInt   (5, item.getStockQuantity());
            ps.setInt   (6, item.getPrepTimeInMinutes());
            ps.setString(7, item.getItemTags());
            ps.setString(8, item.getAvailability());
            ps.setString(9, item.getTemperatureLevel() != null ? item.getTemperatureLevel().name() : null);
            ps.setString(10, item.getStatus() != null ? item.getStatus().name() : "ACTIVE");
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        }
        return -1;
    }

    @Override
    public void update(Connection conn, FbMenuItem item) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SQL_UPDATE)) {
            ps.setInt   (1, item.getCategoryId());
            ps.setString(2, item.getName());
            ps.setString(3, item.getDescription());
            ps.setBigDecimal(4, item.getBasePrice());
            ps.setInt   (5, item.getStockQuantity());
            ps.setInt   (6, item.getPrepTimeInMinutes());
            ps.setString(7, item.getItemTags());
            ps.setString(8, item.getAvailability());
            ps.setString(9, item.getTemperatureLevel() != null ? item.getTemperatureLevel().name() : null);
            ps.setString(10, item.getStatus() != null ? item.getStatus().name() : "ACTIVE");
            ps.setInt   (11, item.getMenuItemId());
            ps.executeUpdate();
        }
    }

    @Override
    public void deleteItem(Connection conn, int menuItemId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SQL_SOFT_DELETE)) {
            ps.setInt(1, menuItemId);
            ps.executeUpdate();
        }
    }

    @Override
    public void deductStock(Connection conn, int menuItemId, int quantity) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SQL_DEDUCT_STOCK)) {
            ps.setInt(1, quantity);
            ps.setInt(2, quantity); // For CASE check
            ps.setInt(3, menuItemId);
            ps.setInt(4, quantity); // WHERE stock_quantity >= quantity
            int rows = ps.executeUpdate();
            if (rows == 0) {
                throw new SQLException("Không đủ tồn kho cho menu_item_id=" + menuItemId
                        + " (yêu cầu: " + quantity + ")");
            }
        }
    }

    // -------------------------------------------------------
    // Private Helpers
    // -------------------------------------------------------

    /** Map một ResultSet row sang FbMenuItem object. */
    private FbMenuItem mapRow(ResultSet rs) throws SQLException {
        FbMenuItem item = new FbMenuItem();
        item.setMenuItemId      (rs.getInt   ("menu_item_id"));
        item.setCategoryId      (rs.getInt   ("category_id"));
        item.setCategoryName    (rs.getString("category_name"));
        item.setName            (rs.getString("name"));
        item.setDescription     (rs.getString("description"));
        item.setBasePrice       (rs.getBigDecimal("base_price"));
        item.setStockQuantity   (rs.getInt   ("stock_quantity"));
        item.setPrepTimeInMinutes(rs.getInt  ("prep_time_minutes"));
        item.setItemTags        (rs.getString("item_tags"));
        item.setAvailability    (rs.getString("availability"));
        item.setDeleted         (rs.getBoolean("is_deleted"));
        
        String tempStr = rs.getString("temperature_level");
        if (tempStr != null && !tempStr.isEmpty()) {
            try {
                item.setTemperatureLevel(com.cyber.model.enums.FbTemperature.valueOf(tempStr.toUpperCase()));
            } catch (IllegalArgumentException e) {
                // Ignore or handle
            }
        }
        
        String statusStr = rs.getString("status");
        if (statusStr != null && !statusStr.isEmpty()) {
            try {
                item.setStatus(com.cyber.model.enums.FBStatus.valueOf(statusStr.toUpperCase()));
            } catch (IllegalArgumentException e) {
                item.setStatus(com.cyber.model.enums.FBStatus.ACTIVE);
            }
        }
        return item;
    }
}
