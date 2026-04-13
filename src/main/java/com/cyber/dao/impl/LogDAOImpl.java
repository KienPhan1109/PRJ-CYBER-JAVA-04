package com.cyber.dao.impl;

import com.cyber.connection.DatabaseConnection;
import com.cyber.dao.ILogDAO;
import com.cyber.model.SystemLog;
import com.cyber.model.enums.LogType;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class LogDAOImpl implements ILogDAO {
    private static final LogDAOImpl instance = new LogDAOImpl();

    private LogDAOImpl() {}

    public static LogDAOImpl getInstance() {
        return instance;
    }

    private SystemLog mapRowToLog(ResultSet rs) throws SQLException {
        LogType lt = LogType.valueOf(rs.getString("log_type").toUpperCase());
        int rawTargetId = rs.getInt("target_id");
        Integer targetId = rs.wasNull() ? null : rawTargetId;
        return new SystemLog(
                rs.getInt("id"),
                lt,
                rs.getInt("actor_id"),
                rs.getString("action"),
                targetId,
                rs.getTimestamp("created_at")
        );
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
    public List<SystemLog> getLogsByType(Connection conn, LogType logType) throws SQLException {
        String sql = "SELECT id, log_type, actor_id, action, target_id, created_at " +
                     "FROM system_logs WHERE log_type = ? ORDER BY created_at DESC";
        return executeLogQuery(conn, sql, logType, null);
    }

    @Override
    public List<SystemLog> getLogsByTypeAndActor(Connection conn, LogType logType, int actorId) throws SQLException {
        String sql = "SELECT id, log_type, actor_id, action, target_id, created_at " +
                     "FROM system_logs WHERE log_type = ? AND actor_id = ? ORDER BY created_at DESC";
        return executeLogQuery(conn, sql, logType, actorId);
    }

    private List<SystemLog> executeLogQuery(Connection conn, String sql, LogType logType, Integer actorId) throws SQLException {
        List<SystemLog> result = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, logType.name());
            if (actorId != null) {
                ps.setInt(2, actorId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapRowToLog(rs));
                }
            }
        }
        return result;
    }
}
