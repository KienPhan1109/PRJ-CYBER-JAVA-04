package com.cyber.dao.impl;

import com.cyber.connection.DatabaseConnection;
import com.cyber.dao.ILogDAO;
import com.cyber.model.SystemLog;
import com.cyber.model.enums.LogType;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class LogDAOImpl implements ILogDAO {
    private static LogDAOImpl instance;

    private LogDAOImpl() {}

    public static synchronized LogDAOImpl getInstance() {
        if (instance == null) {
            instance = new LogDAOImpl();
        }
        return instance;
    }

    @Override
    public void insertLog(Connection conn, SystemLog log) throws SQLException {
        String sql = "INSERT INTO system_logs (log_type, actor_id, action, target_id) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
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

    @Override
    public List<SystemLog> getLogsByType(LogType logType) throws SQLException {
        List<SystemLog> result = new ArrayList<>();
        String sql = "SELECT id, log_type, actor_id, action, target_id, created_at " +
                     "FROM system_logs WHERE log_type = ? ORDER BY created_at DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, logType.name());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    LogType lt = LogType.valueOf(rs.getString("log_type").toUpperCase());
                    int rawTargetId = rs.getInt("target_id");
                    Integer targetId = rs.wasNull() ? null : rawTargetId;
                    result.add(new SystemLog(
                            rs.getInt("id"),
                            lt,
                            rs.getInt("actor_id"),
                            rs.getString("action"),
                            targetId,
                            rs.getTimestamp("created_at")
                    ));
                }
            }
        }
        return result;
    }

    @Override
    public List<SystemLog> getLogsByTypeAndActor(LogType logType, int actorId) throws SQLException {
        List<SystemLog> result = new ArrayList<>();
        String sql = "SELECT id, log_type, actor_id, action, target_id, created_at " +
                     "FROM system_logs WHERE log_type = ? AND actor_id = ? ORDER BY created_at DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, logType.name());
            ps.setInt   (2, actorId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    LogType lt = LogType.valueOf(rs.getString("log_type").toUpperCase());
                    int rawTargetId = rs.getInt("target_id");
                    Integer targetId = rs.wasNull() ? null : rawTargetId;
                    result.add(new SystemLog(
                            rs.getInt("id"),
                            lt,
                            rs.getInt("actor_id"),
                            rs.getString("action"),
                            targetId,
                            rs.getTimestamp("created_at")
                    ));
                }
            }
        }
        return result;
    }
}
