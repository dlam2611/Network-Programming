package com.multiserver.server.config;

import com.multiserver.common.util.FormatUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

/**
 * Đọc cấu hình Server từ duy nhất file server.properties:
 * Không có bất kỳ giá trị hardcode/mặc định nào trong code.
 * Mọi giá trị (IP, port, database, user, password) bắt buộc phải đọc từ file.
 */
public class ServerConfig {
    private String serverName;
    private String serverHost;
    private int serverPort;

    private String peerName;
    private String peerHost;
    private int peerPort;

    private String dbUrl;
    private String dbUser;
    private String dbPassword;

    public ServerConfig() {
    }

    public static ServerConfig load(String filePath) {
        ServerConfig config = new ServerConfig();

        // Tìm kiếm file tại 1 chỗ duy nhất: thư mục config/ của dự án
        File file = new File(filePath);
        if (!file.exists()) {
            file = new File("../" + filePath);
        }
        if (!file.exists()) {
            throw new IllegalStateException("LỖI: Không tìm thấy file cấu hình server tại: " + file.getAbsolutePath());
        }

        System.out.println("Server đang nạp cấu hình từ duy nhất file: " + file.getAbsolutePath());

        Properties props = new Properties();
        try (InputStream in = new FileInputStream(file)) {
            props.load(in);
        } catch (Exception e) {
            throw new RuntimeException("Không thể đọc file cấu hình: " + file.getAbsolutePath(), e);
        }

        // Đọc tường minh mọi cấu hình từ file, KHÔNG có giá trị hardcode trong code
        config.serverName = getRequiredProperty(props, "server.name", file);
        config.serverPort = Integer.parseInt(getRequiredProperty(props, "server.port", file));

        if (props.containsKey("server.host") && !props.getProperty("server.host").trim().isEmpty()) {
            config.serverHost = props.getProperty("server.host").trim();
        }

        config.peerName = getRequiredProperty(props, "peer.name", file);
        config.peerHost = getRequiredProperty(props, "peer.host", file);
        config.peerPort = Integer.parseInt(getRequiredProperty(props, "peer.port", file));

        config.dbUrl = getRequiredProperty(props, "db.url", file);
        config.dbUser = getRequiredProperty(props, "db.user", file);
        config.dbPassword = getRequiredProperty(props, "db.password", file);

        return config;
    }

    private static String getRequiredProperty(Properties props, String key, File file) {
        String val = props.getProperty(key);
        if (val == null || val.trim().isEmpty()) {
            throw new IllegalStateException("Lỗi cấu hình: Thiếu trường bắt buộc '" + key + "' trong file " + file.getAbsolutePath());
        }
        return val.trim();
    }

    public String getServerName() { return serverName; }
    public void setServerName(String serverName) { this.serverName = serverName; }

    public int getServerPort() { return serverPort; }
    public void setServerPort(int serverPort) { this.serverPort = serverPort; }

    public String getServerHost() {
        if (serverHost != null && !serverHost.isBlank()) {
            return serverHost;
        }
        return FormatUtil.getLocalIpAddress();
    }
    public void setServerHost(String serverHost) { this.serverHost = serverHost; }

    public String getPeerName() { return peerName; }
    public void setPeerName(String peerName) { this.peerName = peerName; }

    public String getPeerHost() { return peerHost; }
    public void setPeerHost(String peerHost) { this.peerHost = peerHost; }

    public int getPeerPort() { return peerPort; }
    public void setPeerPort(int peerPort) { this.peerPort = peerPort; }

    public String getDbUrl() { return dbUrl; }
    public void setDbUrl(String dbUrl) { this.dbUrl = dbUrl; }

    public String getDbUser() { return dbUser; }
    public void setDbUser(String dbUser) { this.dbUser = dbUser; }

    public String getDbPassword() { return dbPassword; }
    public void setDbPassword(String dbPassword) { this.dbPassword = dbPassword; }

    @Override
    public String toString() {
        return "ServerConfig{" +
                "name='" + serverName + '\'' +
                ", host='" + getServerHost() + '\'' +
                ", port=" + serverPort +
                ", peer=" + peerName + "(" + peerHost + ":" + peerPort + ")" +
                ", dbUrl='" + dbUrl + '\'' +
                '}';
    }
}
