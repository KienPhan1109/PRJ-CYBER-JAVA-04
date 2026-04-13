package com.cyber.dao.impl;

import com.cyber.dao.IUserDAO;
import com.cyber.model.User;
import com.cyber.model.enums.UserRole;
import com.cyber.model.enums.UserStatus;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserDAOImpl implements IUserDAO {
    private static final UserDAOImpl instance = new UserDAOImpl();

    private UserDAOImpl() {}

    public static UserDAOImpl getInstance() {
        return instance;
    }

    private User mapRowToUser(ResultSet rs) throws SQLException {
        User user = new User();
        user.setUserId(rs.getInt("user_id"));
        user.setUsername(rs.getString("username"));
        user.setPasswordHash(rs.getString("password_hash"));
        user.setBalance(rs.getBigDecimal("balance"));
        user.setFullName(rs.getString("full_name"));
        user.setPhone(rs.getString("phone"));
        user.setCreatedAt(rs.getTimestamp("created_at"));

        String roleStr = rs.getString("role");
        user.setRole(roleStr != null ? UserRole.valueOf(roleStr) : UserRole.CUSTOMER);

        String statusStr = rs.getString("status");
        user.setStatus(statusStr != null ? UserStatus.valueOf(statusStr) : UserStatus.ACTIVE);

        user.setDeleted(rs.getBoolean("is_deleted"));
        return user;
    }

    @Override
    public User findById(Connection conn, int userId) throws SQLException {
        String sql = "SELECT * FROM users WHERE user_id = ? AND is_deleted = 0";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRowToUser(rs);
                }
            }
        }
        return null;
    }

    @Override
    public User findByUsernameAndPassword(Connection conn, String username, String passwordHash) throws SQLException {
        String sql = "SELECT * FROM users WHERE username = ? AND password_hash = ? AND is_deleted = 0";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            stmt.setString(2, passwordHash);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRowToUser(rs);
                }
            }
        }
        return null;
    }

    @Override
    public boolean checkUsernameExist(Connection conn, String username) throws SQLException {
        String sql = "SELECT 1 FROM users WHERE username = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    @Override
    public void registerUser(Connection conn, User user) throws SQLException {
        String sql = "INSERT INTO users (username, password_hash, role, balance, full_name, phone) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, user.getUsername());
            stmt.setString(2, user.getPasswordHash());
            stmt.setString(3, user.getRole() != null ? user.getRole().name() : UserRole.CUSTOMER.name());
            stmt.setBigDecimal(4, user.getBalance() != null ? user.getBalance() : BigDecimal.ZERO);
            stmt.setString(5, user.getFullName());
            stmt.setString(6, user.getPhone());
            stmt.executeUpdate();

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    user.setUserId(generatedKeys.getInt(1));
                }
            }
        }
    }

    @Override
    public void deductBalance(Connection conn, int userId, BigDecimal amount) throws SQLException {
        int rows = updateBalance(conn, userId, amount.negate());
        if (rows == 0) {
            throw new SQLException("Không đủ số dư để thanh toán hoặc tài khoản không tồn tại!");
        }
    }

    @Override
    public void updatePassword(Connection conn, int userId, String newPasswordHash) throws SQLException {
        String sql = "UPDATE users SET password_hash = ? WHERE user_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, newPasswordHash);
            stmt.setInt(2, userId);
            stmt.executeUpdate();
        }
    }

    @Override
    public void addBalance(Connection conn, int userId, BigDecimal amount) throws SQLException {
        updateBalance(conn, userId, amount);
    }

    @Override
    public List<User> getAllUsers(Connection conn) throws SQLException {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM users WHERE is_deleted = 0";
        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                users.add(mapRowToUser(rs));
            }
        }
        return users;
    }

    @Override
    public List<User> searchUsersByName(Connection conn, String name) throws SQLException {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM users WHERE is_deleted = 0 AND (full_name LIKE ? OR username LIKE ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            String searchPattern = "%" + name + "%";
            stmt.setString(1, searchPattern);
            stmt.setString(2, searchPattern);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    users.add(mapRowToUser(rs));
                }
            }
        }
        return users;
    }

    @Override
    public void updateUserStatus(Connection conn, int userId, UserStatus status) throws SQLException {
        String sql = "UPDATE users SET status = ? WHERE user_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, status.name());
            stmt.setInt(2, userId);
            stmt.executeUpdate();
        }
    }

    @Override
    public void updateUser(Connection conn, User user) throws SQLException {
        String sql = "UPDATE users SET username = ?, full_name = ?, phone = ?, role = ? WHERE user_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, user.getUsername());
            stmt.setString(2, user.getFullName());
            stmt.setString(3, user.getPhone());
            stmt.setString(4, user.getRole() != null ? user.getRole().name() : UserRole.CUSTOMER.name());
            stmt.setInt(5, user.getUserId());
            stmt.executeUpdate();
        }
    }

    @Override
    public int updateBalance(Connection conn, int userId, BigDecimal amount) throws SQLException {
        String sql;
        if (amount.compareTo(BigDecimal.ZERO) >= 0) {
            sql = "UPDATE users SET balance = balance + ? WHERE user_id = ? AND is_deleted = 0";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setBigDecimal(1, amount);
                stmt.setInt(2, userId);
                return stmt.executeUpdate();
            }
        } else {
            sql = "UPDATE users SET balance = balance + ? WHERE user_id = ? AND is_deleted = 0 AND balance >= ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setBigDecimal(1, amount);
                stmt.setInt(2, userId);
                stmt.setBigDecimal(3, amount.abs());
                return stmt.executeUpdate();
            }
        }
    }

    @Override
    public void deleteUser(Connection conn, int userId) throws SQLException {
        String suffix = "_del_" + System.currentTimeMillis();
        String sql = "UPDATE users SET is_deleted = 1, username = CONCAT(username, ?) WHERE user_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, suffix);
            stmt.setInt(2, userId);
            stmt.executeUpdate();
        }
    }
}