package com.cyber.connection;

import com.cyber.util.EnvUtils;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {
    private static final String URL = EnvUtils.get("DB_URL", "jdbc:mysql://localhost:3306/null");
    private static final String USER = EnvUtils.get("DB_USER", "null");
    private static final String PASSWORD = EnvUtils.get("DB_PASSWORD", "null");

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}