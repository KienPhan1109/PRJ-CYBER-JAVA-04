package com.cyber.model;

import com.cyber.model.enums.LogType;
import java.sql.Timestamp;

/**
 * Entity / DTO ánh xạ bảng system_logs trong DB.
 * Dùng để nhận kết quả từ DAO và truyền giữa các tầng.
 */
public class SystemLog {

    private int       id;
    private LogType   logType;
    private int       actorId;
    private String    action;
    private Integer   targetId;   // Nullable
    private Timestamp createdAt;

    // -------------------------------------------------------
    // Constructors
    // -------------------------------------------------------

    /** Constructor dùng khi INSERT (chưa có id và createdAt — DB tự sinh). */
    public SystemLog(LogType logType, int actorId, String action, Integer targetId) {
        this.logType  = logType;
        this.actorId  = actorId;
        this.action   = action;
        this.targetId = targetId;
    }

    /** Constructor đầy đủ khi map từ ResultSet. */
    public SystemLog(int id, LogType logType, int actorId, String action,
                     Integer targetId, Timestamp createdAt) {
        this.id        = id;
        this.logType   = logType;
        this.actorId   = actorId;
        this.action    = action;
        this.targetId  = targetId;
        this.createdAt = createdAt;
    }

    // -------------------------------------------------------
    // Getters & Setters
    // -------------------------------------------------------
    public int getId()               { return id; }
    public void setId(int id)        { this.id = id; }

    public LogType getLogType()                 { return logType; }
    public void setLogType(LogType logType)     { this.logType = logType; }

    public int getActorId()                     { return actorId; }
    public void setActorId(int actorId)         { this.actorId = actorId; }

    public String getAction()                   { return action; }
    public void setAction(String action)        { this.action = action; }

    public Integer getTargetId()                { return targetId; }
    public void setTargetId(Integer targetId)   { this.targetId = targetId; }

    public Timestamp getCreatedAt()             { return createdAt; }
    public void setCreatedAt(Timestamp ts)      { this.createdAt = ts; }

    @Override
    public String toString() {
        return String.format("[%s] actor=%d | action=%s | target=%s | at=%s",
                logType, actorId, action,
                targetId != null ? targetId.toString() : "N/A",
                createdAt != null ? createdAt.toString() : "N/A");
    }
}
