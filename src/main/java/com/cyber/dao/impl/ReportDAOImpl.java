package com.cyber.dao.impl;

import com.cyber.dao.IReportDAO;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ReportDAOImpl implements IReportDAO {
    
    private static ReportDAOImpl instance;
    private ReportDAOImpl() {}
    public static synchronized ReportDAOImpl getInstance() {
        if (instance == null) instance = new ReportDAOImpl();
        return instance;
    }

    @Override
    public BigDecimal getTotalBookingRevenue(Connection conn, Timestamp start, Timestamp end) throws SQLException {
        String sql = "SELECT SUM(total_fee) FROM bookings WHERE status = 'COMPLETED' AND created_at BETWEEN ? AND ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setTimestamp(1, start);
            stmt.setTimestamp(2, end);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    BigDecimal val = rs.getBigDecimal(1);
                    return val != null ? val : BigDecimal.ZERO;
                }
            }
        }
        return BigDecimal.ZERO;
    }

    @Override
    public BigDecimal getTotalFbRevenue(Connection conn, Timestamp start, Timestamp end) throws SQLException {
        String sql = "SELECT SUM(total_amount) FROM fb_orders WHERE status = 'DELIVERED' AND created_at BETWEEN ? AND ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setTimestamp(1, start);
            stmt.setTimestamp(2, end);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    BigDecimal val = rs.getBigDecimal(1);
                    return val != null ? val : BigDecimal.ZERO;
                }
            }
        }
        return BigDecimal.ZERO;
    }

    @Override
    public List<Map<String, Object>> getTopSellingItems(Connection conn, Timestamp start, Timestamp end, int limit) throws SQLException {
        List<Map<String, Object>> result = new ArrayList<>();
        String sql = "SELECT item_name_snapshot, SUM(quantity) as total_qty " +
                     "FROM fb_order_details d " +
                     "JOIN fb_orders o ON d.order_id = o.order_id " +
                     "WHERE o.status = 'DELIVERED' AND o.created_at BETWEEN ? AND ? " +
                     "GROUP BY item_name_snapshot " +
                     "ORDER BY total_qty DESC LIMIT ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setTimestamp(1, start);
            stmt.setTimestamp(2, end);
            stmt.setInt(3, limit);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("itemName", rs.getString("item_name_snapshot"));
                    map.put("totalQty", rs.getInt("total_qty"));
                    result.add(map);
                }
            }
        }
        return result;
    }

    @Override
    public List<Map<String, Object>> getMachineUsageStats(Connection conn, Timestamp start, Timestamp end) throws SQLException {
        List<Map<String, Object>> result = new ArrayList<>();
        String sql = "SELECT c.name as computer_name, COUNT(b.booking_id) as total_bookings " +
                     "FROM bookings b " +
                     "JOIN computers c ON b.computer_id = c.computer_id " +
                     "WHERE b.status = 'COMPLETED' AND b.created_at BETWEEN ? AND ? " +
                     "GROUP BY c.name " +
                     "ORDER BY total_bookings DESC LIMIT 10";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setTimestamp(1, start);
            stmt.setTimestamp(2, end);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("computerName", rs.getString("computer_name"));
                    map.put("totalBookings", rs.getInt("total_bookings"));
                    result.add(map);
                }
            }
        }
        return result;
    }
}
