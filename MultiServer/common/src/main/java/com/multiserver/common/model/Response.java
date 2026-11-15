package com.multiserver.common.model;

import java.io.Serializable;

/**
 * Đối tượng phản hồi chuẩn cho các lệnh RPC/RMI giữa Client và Server.
 */
public class Response<T> implements Serializable {
    private static final long serialVersionUID = 1L;

    private boolean success;
    private String message;
    private T data;
    private String serverName;

    public Response() {
    }

    public Response(boolean success, String message, T data, String serverName) {
        this.success = success;
        this.message = message;
        this.data = data;
        this.serverName = serverName;
    }

    public static <T> Response<T> ok(String message, T data, String serverName) {
        return new Response<>(true, message, data, serverName);
    }

    public static <T> Response<T> ok(String message, T data) {
        return new Response<>(true, message, data, null);
    }

    public static <T> Response<T> error(String message, String serverName) {
        return new Response<>(false, message, null, serverName);
    }

    public static <T> Response<T> error(String message) {
        return new Response<>(false, message, null, null);
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public String getServerName() {
        return serverName;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    @Override
    public String toString() {
        return "Response{" +
                "success=" + success +
                ", message='" + message + '\'' +
                ", serverName='" + serverName + '\'' +
                '}';
    }
}

