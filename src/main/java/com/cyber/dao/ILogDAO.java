package com.cyber.dao;

import com.cyber.model.SystemLog;
import com.cyber.model.enums.LogType;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public interface ILogDAO {
    void insertLog(Connection conn, SystemLog log) throws SQLException;

    List<SystemLog> getLogsByType(LogType logType) throws SQLException;

    List<SystemLog> getLogsByTypeAndActor(LogType logType, int actorId) throws SQLException;
}
