package com.mailclient.model;

/**
 * Cấu hình kết nối máy chủ Mail (SMTP / POP3).
 */
public class ServerConfig {
    private String host;
    private int port;
    private String username;
    private String password;
    private SecurityType securityType;
    private int timeoutMillis = 15000; // 15s

    public ServerConfig() {
        this.securityType = SecurityType.NONE;
    }

    public ServerConfig(String host, int port, String username, String password, SecurityType securityType) {
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
        this.securityType = securityType != null ? securityType : SecurityType.NONE;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public SecurityType getSecurityType() {
        return securityType;
    }

    public void setSecurityType(SecurityType securityType) {
        this.securityType = securityType;
    }

    public int getTimeoutMillis() {
        return timeoutMillis;
    }

    public void setTimeoutMillis(int timeoutMillis) {
        this.timeoutMillis = timeoutMillis;
    }

    @Override
    public String toString() {
        return host + ":" + port + " (" + securityType + ")";
    }
}
