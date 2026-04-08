package com.cyber.service;

import com.cyber.connection.DatabaseConnection;
import com.cyber.dao.IUserDAO;
import com.cyber.dao.impl.UserDAOImpl;
import com.cyber.exception.BusinessException;
import com.cyber.model.User;
import com.cyber.model.enums.UserStatus;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class UserService {
    private static UserService instance;
    private final IUserDAO userDAO;

    private UserService() {
        this.userDAO = UserDAOImpl.getInstance();
    }

    public static synchronized UserService getInstance() {
        if (instance == null) {
            instance = new UserService();
        }
        return instance;
    }

    public List<User> getAllUsers() throws BusinessException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return userDAO.getAllUsers(conn);
        } catch (SQLException e) {
            throw new BusinessException("DB_ERROR", "Lỗi lấy danh sách User: " + e.getMessage());
        }
    }

    public void updateUserStatus(int userId, UserStatus status) throws BusinessException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            User user = userDAO.findById(conn, userId);
            if (user == null) {
                throw new BusinessException("USER_NOT_FOUND", "Không tìm thấy người dùng (ID=" + userId + ")");
            }
            userDAO.updateUserStatus(conn, userId, status);
        } catch (SQLException e) {
            throw new BusinessException("DB_ERROR", "Lỗi cập nhật trạng thái User: " + e.getMessage());
        }
    }

    public void topUpUser(int userId, BigDecimal amount) throws BusinessException {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("INVALID_AMOUNT", "Số tiền nạp phải lớn hơn 0");
        }
        try (Connection conn = DatabaseConnection.getConnection()) {
            User user = userDAO.findById(conn, userId);
            if (user == null) {
                throw new BusinessException("USER_NOT_FOUND", "Không tìm thấy người dùng (ID=" + userId + ")");
            }
            userDAO.addBalance(conn, userId, amount);
        } catch (SQLException e) {
            throw new BusinessException("DB_ERROR", "Lỗi nạp tiền cho User: " + e.getMessage());
        }
    }

    public void updateUser(User user) throws BusinessException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            User existing = userDAO.findById(conn, user.getUserId());
            if (existing == null) {
                throw new BusinessException("USER_NOT_FOUND", "Không tìm thấy người dùng");
            }
            userDAO.updateUser(conn, user);
        } catch (SQLException e) {
            throw new BusinessException("DB_ERROR", "Lỗi cập nhật User: " + e.getMessage());
        }
    }
}
