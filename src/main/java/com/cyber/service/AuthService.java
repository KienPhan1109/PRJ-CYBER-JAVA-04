package com.cyber.service;

import com.cyber.connection.DatabaseConnection;
import com.cyber.dao.IUserDAO;
import com.cyber.dao.impl.UserDAOImpl;
import com.cyber.exception.BusinessException;
import com.cyber.model.User;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class AuthService {
    private static AuthService instance;
    private final IUserDAO userDAO;

    private AuthService() {
        this.userDAO = UserDAOImpl.getInstance();
    }

    public static synchronized AuthService getInstance() {
        if (instance == null) {
            instance = new AuthService();
        }
        return instance;
    }

    public User login(String username, String password) throws BusinessException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String hashedPwd = hashPassword(password);

            User user = userDAO.findByUsernameAndPassword(conn, username, hashedPwd);

            if (user == null) {
                User oldUser = userDAO.findByUsernameAndPassword(conn, username, password);
                if (oldUser != null) {
                    userDAO.updatePassword(conn, oldUser.getUserId(), hashedPwd);
                    user = oldUser;
                    user.setPasswordHash(hashedPwd);
                } else {
                    throw new BusinessException("AUTH_FAILED", "Tài khoản hoặc mật khẩu không chính xác.");
                }
            }

            if (user.getStatus() == com.cyber.model.enums.UserStatus.LOCKED) {
                throw new BusinessException("ACCOUNT_LOCKED", "Tài khoản của bạn đã bị khóa. Vui lòng liên hệ quản trị viên.");
            }

            return user;
        } catch (
                SQLException e) {
            throw new BusinessException("DB_ERROR", "Lỗi truy vấn cơ sở dữ liệu: " + e.getMessage());
        }
    }

    public void register(User newUser, String rawPassword) throws BusinessException {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            if (userDAO.checkUsernameExist(conn, newUser.getUsername())) {
                throw new BusinessException("REGISTER_FAILED", "Tên đăng nhập đã tồn tại.");
            }
            
            String hashedPwd = hashPassword(rawPassword);
            newUser.setPasswordHash(hashedPwd);
            newUser.setBalance(BigDecimal.ZERO);
            newUser.setCreatedAt(new Timestamp(System.currentTimeMillis()));
            
            conn.setAutoCommit(false);
            try {
                userDAO.registerUser(conn, newUser);
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new BusinessException("DB_ERROR", "Lỗi CSDL khi đăng ký: " + e.getMessage());
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {}
            }
        }
    }

    private String hashPassword(String rawPassword) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(rawPassword.getBytes());
            StringBuilder hexString = new StringBuilder(2 * hash.length);
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if(hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Lỗi hệ thống: Không tìm thấy thuật toán mã hóa SHA-256", e);
        }
    }
}
