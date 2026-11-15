package com.multiserver.server.config;

import com.multiserver.common.util.FormatUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

/**
 * Đọc cấu hình Server từ file .properties:
 * 1. Port và Database của chính Server mình.
 * 2. Địa chỉ IP và Port của Server đối ứng (của người ta) để đồng bộ.
 */
public class ServerConfig {
    private String serverName = "Server 1";
    private int serverPort = 1099;

    private String peerName = "Server 2";
    private String peerHost = "127.0.0.1";
    private int peerPort = 1098;

    private String dbUrl = "jdbc:postgresql://localhost:5432/RMIDATABASE";
    private String dbUser = "postgres";
    private String dbPassword = "123";

    public ServerConfig() {
    }

    public static ServerConfig load(String filePath) {
        ServerConfig config = new ServerConfig();
        Properties props = new Properties();

        File file = new File(filePath);
        if (file.exists()) {
            try (InputStream in = new FileInputStream(file)) {
                props.load(in);
            } catch (Exception e) {
                System.err.println("Không thể đọc file cấu hình: " + filePath + ", lỗi: " + e.getMessage());
            }
        } else {
            try (InputStream in = ServerConfig.class.getClassLoader().getResourceAsStream(filePath)) {
                if (in != null) {
                    props.load(in);
                } else {
                    System.out.println("Không tìm thấy file cấu hình '" + filePath + "', sử dụng cấu hình mặc định.");
                }
            } catch (Exception e) {
                System.err.println("Lỗi khi đọc file classpath: " + filePath + ", " + e.getMessage());
            }
        }

        if (!props.isEmpty()) {
            config.serverName = props.getProperty("server.name", config.serverName).trim();
            config.serverPort = Integer.parseInt(props.getProperty("server.port", String.valueOf(config.serverPort)).trim());

            config.peerName = props.getProperty("peer.name", config.peerName).trim();
            config.peerHost = props.getProperty("peer.host", config.peerHost).trim();
            config.peerPort = Integer.parseInt(props.getProperty("peer.port", String.valueOf(config.peerPort)).trim());

            config.dbUrl = props.getProperty("db.url", config.dbUrl).trim();
            config.dbUser = props.getProperty("db.user", config.dbUser).trim();
            config.dbPassword = props.getProperty("db.password", config.dbPassword).trim();
        }

        return config;
    }

    public String getServerName() { return serverName; }
    public void setServerName(String serverName) { this.serverName = serverName; }

    public int getServerPort() { return serverPort; }
    public void setServerPort(int serverPort) { this.serverPort = serverPort; }

    // Tự động lấy IP của máy hiện tại, không bắt người dùng cấu hình thủ công
    public String getServerHost() { return FormatUtil.getLocalIpAddress(); }

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
                ", port=" + serverPort +
                ", peer=" + peerName + "(" + peerHost + ":" + peerPort + ")" +
                ", dbUrl='" + dbUrl + '\'' +
                '}';
    }
}
