package model;

import java.io.Serializable;

public class AuditLog implements Serializable {
    private static final long serialVersionUID = 1L;

    private String logId;
    private String timestamp;
    private String userId;
    private String action;
    private String details;

    public AuditLog() {}

    public AuditLog(String logId, String timestamp, String userId, String action, String details) {
        this.logId = logId;
        this.timestamp = timestamp;
        this.userId = userId;
        this.action = action;
        this.details = details;
    }

    public String getLogId() { return logId; }
    public void setLogId(String logId) { this.logId = logId; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }

    @Override
    public String toString() {
        return timestamp + " | " + userId + " | " + action + ": " + details;
    }
}
