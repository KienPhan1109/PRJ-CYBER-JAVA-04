package com.cyber.dao.impl;
import com.cyber.dao.IServiceItemDAO;
import com.cyber.model.ServiceItem;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServiceItemDAOImpl implements IServiceItemDAO {

    // Singleton Pattern
    private static ServiceItemDAOImpl instance;
    private ServiceItemDAOImpl() {}
    public static synchronized ServiceItemDAOImpl getInstance() {
        if (instance == null) {
            instance = new ServiceItemDAOImpl();
        }
        return instance;
    }

    @Override
    public List<ServiceItem> getAllServiceItems(Connection conn) throws SQLException {
        List<ServiceItem> items = new ArrayList<>();
        String sql = "SELECT * FROM service_items ORDER BY item_id ASC";
        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                items.add(mapRowToServiceItem(rs));
            }
        }
        return items;
    }

    @Override
    public ServiceItem findById(Connection conn, int itemId) throws SQLException {
        String sql = "SELECT * FROM service_items WHERE item_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, itemId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRowToServiceItem(rs);
                }
            }
        }
        return null;
    }

    @Override
    public int addServiceItem(Connection conn, ServiceItem item) throws SQLException {
        String sql = "INSERT INTO service_items (name, description, price, stock_quantity, status) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, item.getName());
            stmt.setString(2, item.getDescription());
            stmt.setBigDecimal(3, item.getPrice());
            stmt.setInt(4, item.getStockQuantity());
            stmt.setString(5, item.getStatus() != null ? item.getStatus().name() : com.cyber.model.enums.ServiceItemStatus.ACTIVE.name());
            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return -1;
    }

    @Override
    public void updateServiceItem(Connection conn, ServiceItem item) throws SQLException {
        String sql = "UPDATE service_items SET name = ?, description = ?, price = ?, stock_quantity = ?, status = ? WHERE item_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, item.getName());
            stmt.setString(2, item.getDescription());
            stmt.setBigDecimal(3, item.getPrice());
            stmt.setInt(4, item.getStockQuantity());
            stmt.setString(5, item.getStatus() != null ? item.getStatus().name() : com.cyber.model.enums.ServiceItemStatus.ACTIVE.name());
            stmt.setInt(6, item.getItemId());
            stmt.executeUpdate();
        }
    }

    @Override
    public void deleteServiceItem(Connection conn, int itemId) throws SQLException {
        String sql = "DELETE FROM service_items WHERE item_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, itemId);
            stmt.executeUpdate();
        }
    }

    @Override
    public void deductStock(Connection conn, int itemId, int quantity) throws SQLException {
        String sql = "UPDATE service_items SET stock_quantity = stock_quantity - ? WHERE item_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, quantity);
            stmt.setInt(2, itemId);
            stmt.executeUpdate();
        }
    }

    private ServiceItem mapRowToServiceItem(ResultSet rs) throws SQLException {
        ServiceItem item = new ServiceItem();
        item.setItemId(rs.getInt("item_id"));
        item.setName(rs.getString("name"));
        item.setDescription(rs.getString("description"));
        item.setPrice(rs.getBigDecimal("price"));
        item.setStockQuantity(rs.getInt("stock_quantity"));
        
        String statusStr = rs.getString("status");
        if (statusStr != null) item.setStatus(com.cyber.model.enums.ServiceItemStatus.valueOf(statusStr.toUpperCase()));
        
        return item;
    }
}