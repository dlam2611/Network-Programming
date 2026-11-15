package com.multiserver.common.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Lưu vết truy cập của Client trên giao diện Server:
 * IP, Thao tác, Ngày giờ truy cập, Trạng thái, Chi tiết.
 */
public class AccessLog implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private long id;
    private LocalDateTime timestamp;
    private String clientIp;
    private String operation;
    private boolean success;
    private String details;

    public AccessLog() {
        this.timestamp = LocalDateTime.now();
    }

    public AccessLog(long id, LocalDateTime timestamp, String clientIp, String operation, boolean success, String details) {
        this.id = id;
        this.timestamp = timestamp != null ? timestamp : LocalDateTime.now();
        this.clientIp = clientIp != null ? clientIp : "127.0.0.1";
        this.operation = operation;
        this.success = success;
        this.details = details;
    }

    public AccessLog(long id, String clientIp, String operation, boolean success, String details) {
        this(id, LocalDateTime.now(), clientIp, operation, success, details);
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public String getFormattedTimestamp() {
        return timestamp != null ? timestamp.format(FORMATTER) : "";
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getClientIp() {
        return clientIp;
    }

    public void setClientIp(String clientIp) {
        this.clientIp = clientIp;
    }

    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }
}

