package com.cyber.service;

import com.cyber.connection.DatabaseConnection;
import com.cyber.dao.IUserDAO;
import com.cyber.dao.impl.UserDAOImpl;
import com.cyber.exception.BusinessException;
import com.cyber.model.Role;
import com.cyber.model.User;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;

public class AuthService {
    // Singleton Pattern
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
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            String hashedPwd = hashPassword(password);
            
            User user = userDAO.findByUsernameAndPassword(conn, username, hashedPwd);
            
            if (user == null) {
                throw new BusinessException("AUTH_FAILED", "Tài khoản hoặc mật khẩu không chính xác.");
            }
            
            return user;
        } catch (SQLException e) {
            throw new BusinessException("DB_ERROR", "Lỗi truy vấn cơ sở dữ liệu: " + e.getMessage());
        } finally {
            if (conn != null) {
                try { conn.close(); } catch (SQLException ex) {}
            }
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
                } catch (SQLException ex) {}
            }
        }
    }

    private String hashPassword(String rawPassword) {
        return rawPassword;
    }
}
