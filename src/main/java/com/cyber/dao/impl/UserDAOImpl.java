package com.cyber.dao.impl;
import com.cyber.dao.IUserDAO;
import com.cyber.model.Role;
import com.cyber.model.User;
import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserDAOImpl implements IUserDAO {
    private static UserDAOImpl instance;
    private UserDAOImpl() {}

    public static synchronized UserDAOImpl getInstance() {
        if (instance == null) {
            instance = new UserDAOImpl();
        }
        return instance;
    }

    @Override
    public User findById(Connection conn, int userId) throws SQLException {
        String sql = "SELECT u.*, r.role_name FROM users u JOIN roles r ON u.role_id = r.role_id WHERE u.user_id = ? AND u.is_deleted = 0";
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
        String sql = "SELECT u.*, r.role_name FROM users u JOIN roles r ON u.role_id = r.role_id WHERE u.username = ? AND u.password_hash = ? AND u.is_deleted = 0";
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
        String sql = "INSERT INTO users (username, password_hash, role_id, balance, full_name, phone) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, user.getUsername());
            stmt.setString(2, user.getPasswordHash());
            stmt.setInt(3, user.getRole() != null ? user.getRole().getRoleId() : 3); // 3 = CUSTOMER default
            stmt.setBigDecimal(4, user.getBalance());
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
            throw new SQLException("Không đủ số dư để thanh toán!");
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
        String sql = "SELECT u.*, r.role_name FROM users u JOIN roles r ON u.role_id = r.role_id WHERE u.is_deleted = 0";
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
        String sql = "SELECT u.*, r.role_name FROM users u JOIN roles r ON u.role_id = r.role_id WHERE u.is_deleted = 0 AND (u.full_name LIKE ? OR u.username LIKE ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, "%" + name + "%");
            stmt.setString(2, "%" + name + "%");
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    users.add(mapRowToUser(rs));
                }
            }
        }
        return users;
    }

    @Override
    public void updateUserStatus(Connection conn, int userId, com.cyber.model.enums.UserStatus status) throws SQLException {
        String sql = "UPDATE users SET status = ? WHERE user_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, status.name());
            stmt.setInt(2, userId);
            stmt.executeUpdate();
        }
    }

    @Override
    public void updateUser(Connection conn, User user) throws SQLException {
        String sql = "UPDATE users SET full_name = ?, phone = ?, role_id = ? WHERE user_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, user.getFullName());
            stmt.setString(2, user.getPhone());
            stmt.setInt(3, user.getRole() != null ? user.getRole().getRoleId() : 3);
            stmt.setInt(4, user.getUserId());
            stmt.executeUpdate();
        }
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
        
        Role role = new Role(rs.getInt("role_id"), rs.getString("role_name"));
        user.setRole(role);
        
        String statusStr = rs.getString("status");
        if (statusStr != null) {
            user.setStatus(com.cyber.model.enums.UserStatus.valueOf(statusStr));
        } else {
            user.setStatus(com.cyber.model.enums.UserStatus.ACTIVE);
        }
        user.setDeleted(rs.getBoolean("is_deleted"));
        
        return user;
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
        String sql = "UPDATE users SET is_deleted = 1 WHERE user_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.executeUpdate();
        }
    }
}