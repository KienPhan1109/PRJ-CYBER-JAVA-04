package com.cyber.dao;
import com.cyber.model.User;
import com.cyber.model.enums.UserStatus;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;

public interface IUserDAO {
    User findById(Connection conn, int userId) throws SQLException;

    User findByUsernameAndPassword(Connection conn, String username, String passwordHash) throws SQLException;

    boolean checkUsernameExist(Connection conn, String username) throws SQLException;

    void registerUser(Connection conn, User user) throws SQLException;

    void updatePassword(Connection conn, int userId, String newPasswordHash) throws SQLException;

    void deductBalance(Connection conn, int userId, BigDecimal amount) throws SQLException;

    void addBalance(Connection conn, int userId, BigDecimal amount) throws SQLException;

    java.util.List<User> getAllUsers(Connection conn) throws SQLException;

    java.util.List<User> searchUsersByName(Connection conn, String name) throws SQLException;

    void updateUserStatus(Connection conn, int userId, UserStatus status) throws SQLException;

    void updateUser(Connection conn, User user) throws SQLException;

    int updateBalance(Connection conn, int userId, BigDecimal amount) throws SQLException;

    void deleteUser(Connection conn, int userId) throws SQLException;
}