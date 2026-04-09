package com.cyber.dao.impl;

import com.cyber.dao.IComputerDAO;
import com.cyber.model.Computer;
import com.cyber.model.enums.ComputerStatus;
import com.cyber.model.enums.ComputerZone;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ComputerDAOImpl implements IComputerDAO {

    private static ComputerDAOImpl instance;

    private ComputerDAOImpl() {}

    public static synchronized ComputerDAOImpl getInstance() {
        if (instance == null) {
            instance = new ComputerDAOImpl();
        }
        return instance;
    }

    @Override
    public List<Computer> getAllActiveComputers(Connection conn) throws SQLException {
        List<Computer> computers = new ArrayList<>();
        String sql = "SELECT * FROM computers WHERE is_deleted = 0 ORDER BY computer_id ASC";
        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                computers.add(mapRowToComputer(rs));
            }
        }
        return computers;
    }

    @Override
    public Computer findById(Connection conn, int computerId) throws SQLException {
        String sql = "SELECT * FROM computers WHERE computer_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, computerId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRowToComputer(rs);
                }
            }
        }
        return null;
    }

    @Override
    public boolean checkNameExists(Connection conn, String name) throws SQLException {
        String sql = "SELECT 1 FROM computers WHERE name = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, name);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    @Override
    public int addComputer(Connection conn, Computer computer) throws SQLException {
        String sql = "INSERT INTO computers (name, zone, hardware_config, status, price_per_hour) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, computer.getName());
            stmt.setString(2, computer.getZone() != null ? computer.getZone().name() : ComputerZone.STANDARD.name());
            stmt.setString(3, computer.getHardwareConfig());
            stmt.setString(4, computer.getStatus() != null ? computer.getStatus().name() : ComputerStatus.AVAILABLE.name());
            stmt.setBigDecimal(5, computer.getPricePerHour());
            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return -1;
    }

    @Override
    public void updateComputer(Connection conn, Computer computer) throws SQLException {
        String sql = "UPDATE computers SET name = ?, zone = ?, hardware_config = ?, status = ?, price_per_hour = ? WHERE computer_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, computer.getName());
            stmt.setString(2, computer.getZone() != null ? computer.getZone().name() : ComputerZone.STANDARD.name());
            stmt.setString(3, computer.getHardwareConfig());
            stmt.setString(4, computer.getStatus() != null ? computer.getStatus().name() : ComputerStatus.AVAILABLE.name());
            stmt.setBigDecimal(5, computer.getPricePerHour());
            stmt.setInt(6, computer.getComputerId());
            stmt.executeUpdate();
        }
    }

    @Override
    public void deleteComputer(Connection conn, int computerId) throws SQLException {
        String sql = "UPDATE computers SET is_deleted = 1 WHERE computer_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, computerId);
            stmt.executeUpdate();
        }
    }

    private Computer mapRowToComputer(ResultSet rs) throws SQLException {
        Computer computer = new Computer();
        computer.setComputerId(rs.getInt("computer_id"));
        computer.setName(rs.getString("name"));
        
        String zoneStr = rs.getString("zone");
        if (zoneStr != null) computer.setZone(com.cyber.model.enums.ComputerZone.valueOf(zoneStr.toUpperCase()));
        
        computer.setHardwareConfig(rs.getString("hardware_config"));
        
        String statusStr = rs.getString("status");
        if (statusStr != null) computer.setStatus(com.cyber.model.enums.ComputerStatus.valueOf(statusStr.toUpperCase()));
        
        computer.setPricePerHour(rs.getBigDecimal("price_per_hour"));
        computer.setDeleted(rs.getBoolean("is_deleted"));
        return computer;
    }
}
