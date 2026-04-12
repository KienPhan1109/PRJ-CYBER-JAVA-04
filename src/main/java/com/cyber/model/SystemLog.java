package com.cyber.model;

import com.cyber.model.enums.LogType;

import java.sql.Timestamp;

public class SystemLog {

    private int id;
    private LogType logType;
    private int actorId;
    private String action;
    private Integer targetId;   // Nullable
    private Timestamp createdAt;

    public SystemLog(LogType logType, int actorId, String action, Integer targetId) {
        this.logType = logType;
        this.actorId = actorId;
        this.action = action;
        this.targetId = targetId;
    }

    public SystemLog(int id, LogType logType, int actorId, String action,
                     Integer targetId, Timestamp createdAt) {
        this.id = id;
        this.logType = logType;
        this.actorId = actorId;
        this.action = action;
        this.targetId = targetId;
        this.createdAt = createdAt;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public LogType getLogType() {
        return logType;
    }

    public int getActorId() {
        return actorId;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public Integer getTargetId() {
        return targetId;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    @Override
    public String toString() {
        return String.format("[%s] actor=%d | action=%s | target=%s | at=%s",
                logType, actorId, action,
                targetId != null ? targetId.toString() : "N/A",
                createdAt != null ? createdAt.toString() : "N/A");
    }
}

