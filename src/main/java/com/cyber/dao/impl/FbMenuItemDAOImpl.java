package com.cyber.dao.impl;

import com.cyber.dao.IFbMenuItemDAO;
import com.cyber.domain.fb.FbMenuItem;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class FbMenuItemDAOImpl implements IFbMenuItemDAO {
    private static FbMenuItemDAOImpl instance;
    private FbMenuItemDAOImpl() {}

    public static synchronized FbMenuItemDAOImpl getInstance() {
        if (instance == null) {
            instance = new FbMenuItemDAOImpl();
        }
        return instance;
    }

    @Override
    public List<FbMenuItem> getAllActiveItems(Connection conn) throws SQLException {
        List<FbMenuItem> result = new ArrayList<>();
        String sql = "SELECT m.*, c.category_name FROM fb_menu_items m " +
                     "JOIN fb_categories c ON m.category_id = c.category_id " +
                     "WHERE m.status IN ('ACTIVE', 'OUT_OF_STOCK') AND m.is_deleted = 0 " +
                     "ORDER BY m.menu_item_id ASC";
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                FbMenuItem item = new FbMenuItem();
                item.setMenuItemId(rs.getInt("menu_item_id"));
                item.setCategoryId(rs.getInt("category_id"));
                item.setCategoryName(rs.getString("category_name"));
                item.setName(rs.getString("name"));
                item.setDescription(rs.getString("description"));
                item.setBasePrice(rs.getBigDecimal("base_price"));
                item.setStockQuantity(rs.getInt("stock_quantity"));
                item.setPrepTimeInMinutes(rs.getInt("prep_time_minutes"));
                item.setAvailability(rs.getString("availability"));
                item.setDeleted(rs.getBoolean("is_deleted"));
                String tempStr = rs.getString("temperature_level");
                if (tempStr != null && !tempStr.isEmpty()) {
                    try { item.setTemperatureLevel(com.cyber.model.enums.FbTemperature.valueOf(tempStr.toUpperCase())); } catch (IllegalArgumentException e) {}
                }
                String statusStr = rs.getString("status");
                if (statusStr != null && !statusStr.isEmpty()) {
                    try { item.setStatus(com.cyber.model.enums.FBStatus.valueOf(statusStr.toUpperCase())); } catch (IllegalArgumentException e) { item.setStatus(com.cyber.model.enums.FBStatus.ACTIVE); }
                }
                result.add(item);
            }
        }
        return result;
    }

    @Override
    public List<FbMenuItem> getAllItemsForAdmin(Connection conn) throws SQLException {
        List<FbMenuItem> result = new ArrayList<>();
        String sql = "SELECT m.*, c.category_name FROM fb_menu_items m " +
                     "JOIN fb_categories c ON m.category_id = c.category_id " +
                     "ORDER BY m.menu_item_id ASC";
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                FbMenuItem item = new FbMenuItem();
                item.setMenuItemId(rs.getInt("menu_item_id"));
                item.setCategoryId(rs.getInt("category_id"));
                item.setCategoryName(rs.getString("category_name"));
                item.setName(rs.getString("name"));
                item.setDescription(rs.getString("description"));
                item.setBasePrice(rs.getBigDecimal("base_price"));
                item.setStockQuantity(rs.getInt("stock_quantity"));
                item.setPrepTimeInMinutes(rs.getInt("prep_time_minutes"));
                item.setAvailability(rs.getString("availability"));
                item.setDeleted(rs.getBoolean("is_deleted"));
                String tempStr = rs.getString("temperature_level");
                if (tempStr != null && !tempStr.isEmpty()) {
                    try { item.setTemperatureLevel(com.cyber.model.enums.FbTemperature.valueOf(tempStr.toUpperCase())); } catch (IllegalArgumentException e) {}
                }
                String statusStr = rs.getString("status");
                if (statusStr != null && !statusStr.isEmpty()) {
                    try { item.setStatus(com.cyber.model.enums.FBStatus.valueOf(statusStr.toUpperCase())); } catch (IllegalArgumentException e) { item.setStatus(com.cyber.model.enums.FBStatus.ACTIVE); }
                }
                result.add(item);
            }
        }
        return result;
    }

    @Override
    public FbMenuItem findById(Connection conn, int menuItemId) throws SQLException {
        String sql = "SELECT m.*, c.category_name FROM fb_menu_items m " +
                     "JOIN fb_categories c ON m.category_id = c.category_id " +
                     "WHERE m.menu_item_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, menuItemId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    FbMenuItem item = new FbMenuItem();
                    item.setMenuItemId(rs.getInt("menu_item_id"));
                    item.setCategoryId(rs.getInt("category_id"));
                    item.setCategoryName(rs.getString("category_name"));
                    item.setName(rs.getString("name"));
                    item.setDescription(rs.getString("description"));
                    item.setBasePrice(rs.getBigDecimal("base_price"));
                    item.setStockQuantity(rs.getInt("stock_quantity"));
                    item.setPrepTimeInMinutes(rs.getInt("prep_time_minutes"));
                    item.setAvailability(rs.getString("availability"));
                    item.setDeleted(rs.getBoolean("is_deleted"));
                    String tempStr = rs.getString("temperature_level");
                    if (tempStr != null && !tempStr.isEmpty()) {
                        try { item.setTemperatureLevel(com.cyber.model.enums.FbTemperature.valueOf(tempStr.toUpperCase())); } catch (IllegalArgumentException e) {}
                    }
                    String statusStr = rs.getString("status");
                    if (statusStr != null && !statusStr.isEmpty()) {
                        try { item.setStatus(com.cyber.model.enums.FBStatus.valueOf(statusStr.toUpperCase())); } catch (IllegalArgumentException e) { item.setStatus(com.cyber.model.enums.FBStatus.ACTIVE); }
                    }
                    return item;
                }
            }
        }
        return null;
    }

    @Override
    public List<FbMenuItem> findByCategoryId(Connection conn, int categoryId) throws SQLException {
        List<FbMenuItem> result = new ArrayList<>();
        String sql = "SELECT m.*, c.category_name FROM fb_menu_items m " +
                     "JOIN fb_categories c ON m.category_id = c.category_id " +
                     "WHERE m.category_id = ? AND m.status = 'ACTIVE' AND m.is_deleted = 0 " +
                     "ORDER BY m.name";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, categoryId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    FbMenuItem item = new FbMenuItem();
                    item.setMenuItemId(rs.getInt("menu_item_id"));
                    item.setCategoryId(rs.getInt("category_id"));
                    item.setCategoryName(rs.getString("category_name"));
                    item.setName(rs.getString("name"));
                    item.setDescription(rs.getString("description"));
                    item.setBasePrice(rs.getBigDecimal("base_price"));
                    item.setStockQuantity(rs.getInt("stock_quantity"));
                    item.setPrepTimeInMinutes(rs.getInt("prep_time_minutes"));
                    item.setAvailability(rs.getString("availability"));
                    item.setDeleted(rs.getBoolean("is_deleted"));
                    String tempStr = rs.getString("temperature_level");
                    if (tempStr != null && !tempStr.isEmpty()) {
                        try { item.setTemperatureLevel(com.cyber.model.enums.FbTemperature.valueOf(tempStr.toUpperCase())); } catch (IllegalArgumentException e) {}
                    }
                    String statusStr = rs.getString("status");
                    if (statusStr != null && !statusStr.isEmpty()) {
                        try { item.setStatus(com.cyber.model.enums.FBStatus.valueOf(statusStr.toUpperCase())); } catch (IllegalArgumentException e) { item.setStatus(com.cyber.model.enums.FBStatus.ACTIVE); }
                    }
                    result.add(item);
                }
            }
        }
        return result;
    }

    @Override
    public int create(Connection conn, FbMenuItem item) throws SQLException {
        String sql = "INSERT INTO fb_menu_items " +
                     "(category_id, name, description, base_price, stock_quantity, prep_time_minutes, availability, temperature_level, status) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, item.getCategoryId());
            ps.setString(2, item.getName());
            ps.setString(3, item.getDescription());
            ps.setBigDecimal(4, item.getBasePrice());
            ps.setInt(5, item.getStockQuantity());
            ps.setInt(6, item.getPrepTimeInMinutes());
            ps.setString(7, item.getAvailability());
            ps.setString(8, item.getTemperatureLevel() != null ? item.getTemperatureLevel().name() : null);
            ps.setString(9, item.getStatus() != null ? item.getStatus().name() : "ACTIVE");
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        }
        return -1;
    }

    @Override
    public void update(Connection conn, FbMenuItem item) throws SQLException {
        String sql = "UPDATE fb_menu_items SET " +
                     "category_id=?, name=?, description=?, base_price=?, stock_quantity=?, " +
                     "prep_time_minutes=?, availability=?, temperature_level=?, status=? " +
                     "WHERE menu_item_id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, item.getCategoryId());
            ps.setString(2, item.getName());
            ps.setString(3, item.getDescription());
            ps.setBigDecimal(4, item.getBasePrice());
            ps.setInt(5, item.getStockQuantity());
            ps.setInt(6, item.getPrepTimeInMinutes());
            ps.setString(7, item.getAvailability());
            ps.setString(8, item.getTemperatureLevel() != null ? item.getTemperatureLevel().name() : null);
            ps.setString(9, item.getStatus() != null ? item.getStatus().name() : "ACTIVE");
            ps.setInt(10, item.getMenuItemId());
            ps.executeUpdate();
        }
    }

    @Override
    public void deleteItem(Connection conn, int menuItemId) throws SQLException {
        String sql = "UPDATE fb_menu_items SET status = 'HIDDEN' WHERE menu_item_id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, menuItemId);
            ps.executeUpdate();
        }
    }

    @Override
    public void deductStock(Connection conn, int menuItemId, int quantity) throws SQLException {
        String sql = "UPDATE fb_menu_items " +
                     "SET stock_quantity = stock_quantity - ?, " +
                     "    status = CASE WHEN stock_quantity - ? = 0 THEN 'OUT_OF_STOCK' ELSE status END " +
                     "WHERE menu_item_id = ? AND stock_quantity >= ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, quantity);
            ps.setInt(2, quantity);
            ps.setInt(3, menuItemId);
            ps.setInt(4, quantity);
            int rows = ps.executeUpdate();
            if (rows == 0) {
                throw new SQLException("Không đủ tồn kho cho menu_item_id=" + menuItemId + " (yêu cầu: " + quantity + ")");
            }
        }
    }

    @Override
    public void addStock(Connection conn, int menuItemId, int quantity) throws SQLException {
        String sql = "UPDATE fb_menu_items SET stock_quantity = stock_quantity + ?, " +
                     "status = CASE WHEN status = 'OUT_OF_STOCK' AND stock_quantity + ? > 0 THEN 'ACTIVE' ELSE status END " +
                     "WHERE menu_item_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, quantity);
            ps.setInt(2, quantity);
            ps.setInt(3, menuItemId);
            ps.executeUpdate();
        }
    }

    @Override
    public FbMenuItem findByName(Connection conn, String name) throws SQLException {
        String sql = "SELECT m.*, c.category_name FROM fb_menu_items m " +
                     "JOIN fb_categories c ON m.category_id = c.category_id " +
                     "WHERE m.name = ? AND m.is_deleted = 0";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    FbMenuItem item = new FbMenuItem();
                    item.setMenuItemId(rs.getInt("menu_item_id"));
                    item.setCategoryId(rs.getInt("category_id"));
                    item.setCategoryName(rs.getString("category_name"));
                    item.setName(rs.getString("name"));
                    item.setDescription(rs.getString("description"));
                    item.setBasePrice(rs.getBigDecimal("base_price"));
                    item.setStockQuantity(rs.getInt("stock_quantity"));
                    item.setPrepTimeInMinutes(rs.getInt("prep_time_minutes"));
                    item.setAvailability(rs.getString("availability"));
                    item.setDeleted(rs.getBoolean("is_deleted"));
                    String tempStr = rs.getString("temperature_level");
                    if (tempStr != null && !tempStr.isEmpty()) {
                        try { item.setTemperatureLevel(com.cyber.model.enums.FbTemperature.valueOf(tempStr.toUpperCase())); } catch (IllegalArgumentException e) {}
                    }
                    String statusStr = rs.getString("status");
                    if (statusStr != null && !statusStr.isEmpty()) {
                        try { item.setStatus(com.cyber.model.enums.FBStatus.valueOf(statusStr.toUpperCase())); } catch (IllegalArgumentException e) { item.setStatus(com.cyber.model.enums.FBStatus.ACTIVE); }
                    }
                    return item;
                }
            }
        }
        return null;
    }
}
