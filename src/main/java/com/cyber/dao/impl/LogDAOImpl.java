package com.cyber.dao.impl;

import com.cyber.connection.DatabaseConnection;
import com.cyber.dao.ILogDAO;
import com.cyber.model.SystemLog;
import com.cyber.model.enums.LogType;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Cài đặt ILogDAO cho bảng system_logs.
 * Singleton Pattern, dùng PreparedStatement chống SQL Injection.
 * <p>
 * insertLog() nhận Connection từ ngoài (chạy chung transaction).
 * Các method read() tự mở Connection riêng (read-only).
 */
public class LogDAOImpl implements ILogDAO {

    // -------------------------------------------------------
    // Singleton
    // -------------------------------------------------------
    private static LogDAOImpl instance;

    private LogDAOImpl() {}

    public static synchronized LogDAOImpl getInstance() {
        if (instance == null) {
            instance = new LogDAOImpl();
        }
        return instance;
    }

    // -------------------------------------------------------
    // SQL Constants
    // -------------------------------------------------------
    private static final String SQL_INSERT =
            "INSERT INTO system_logs (log_type, actor_id, action, target_id) " +
            "VALUES (?, ?, ?, ?)";

    private static final String SQL_BY_TYPE =
            "SELECT id, log_type, actor_id, action, target_id, created_at " +
            "FROM system_logs " +
            "WHERE log_type = ? " +
            "ORDER BY created_at DESC";

    private static final String SQL_BY_TYPE_AND_ACTOR =
            "SELECT id, log_type, actor_id, action, target_id, created_at " +
            "FROM system_logs " +
            "WHERE log_type = ? AND actor_id = ? " +
            "ORDER BY created_at DESC";

    // -------------------------------------------------------
    // WRITE — sử dụng Connection được truyền vào (cùng transaction)
    // -------------------------------------------------------

    @Override
    public void insertLog(Connection conn, SystemLog log) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SQL_INSERT)) {
            ps.setString(1, log.getLogType().name());
            ps.setInt   (2, log.getActorId());
            ps.setString(3, log.getAction());
            if (log.getTargetId() != null) {
                ps.setInt(4, log.getTargetId());
            } else {
                ps.setNull(4, Types.INTEGER);
            }
            ps.executeUpdate();
        }
    }

    // -------------------------------------------------------
    // READ — tự mở Connection riêng
    // -------------------------------------------------------

    @Override
    public List<SystemLog> getLogsByType(LogType logType) throws SQLException {
        List<SystemLog> result = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_BY_TYPE)) {
            ps.setString(1, logType.name());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapRow(rs));
                }
            }
        }
        return result;
    }

    @Override
    public List<SystemLog> getLogsByTypeAndActor(LogType logType, int actorId) throws SQLException {
        List<SystemLog> result = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_BY_TYPE_AND_ACTOR)) {
            ps.setString(1, logType.name());
            ps.setInt   (2, actorId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapRow(rs));
                }
            }
        }
        return result;
    }

    // -------------------------------------------------------
    // Private Helper
    // -------------------------------------------------------

    /** Map một ResultSet row sang SystemLog object. */
    private SystemLog mapRow(ResultSet rs) throws SQLException {
        LogType logType = LogType.valueOf(rs.getString("log_type").toUpperCase());

        int rawTargetId = rs.getInt("target_id");
        Integer targetId = rs.wasNull() ? null : rawTargetId;

        return new SystemLog(
                rs.getInt      ("id"),
                logType,
                rs.getInt      ("actor_id"),
                rs.getString   ("action"),
                targetId,
                rs.getTimestamp("created_at")
        );
    }
}
