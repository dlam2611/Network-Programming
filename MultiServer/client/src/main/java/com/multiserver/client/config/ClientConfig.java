package com.multiserver.client.config;

import com.multiserver.common.model.ServerInfo;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Đọc cấu hình Client từ client.properties:
 * Cấu hình rõ ràng địa chỉ IP và Port của từng Server:
 * server1.host, server1.port
 * server2.host, server2.port
 */
public class ClientConfig {
    private final List<ServerInfo> servers = new ArrayList<>();
    private int timeoutMs = 3000;

    public ClientConfig() {
    }

    public static ClientConfig load(String filePath) {
        ClientConfig config = new ClientConfig();
        Properties props = new Properties();

        File file = new File(filePath);
        if (file.exists()) {
            try (InputStream in = new FileInputStream(file)) {
                props.load(in);
            } catch (Exception e) {
                System.err.println("Không đọc được file: " + filePath + ", " + e.getMessage());
            }
        } else {
            try (InputStream in = ClientConfig.class.getClassLoader().getResourceAsStream(filePath)) {
                if (in != null) {
                    props.load(in);
                }
            } catch (Exception ignored) {
            }
        }

        config.timeoutMs = Integer.parseInt(props.getProperty("connect.timeout", "3000").trim());

        // 1. Đọc cấu hình tường minh từng Server: server1.host/port, server2.host/port
        String s1Host = props.getProperty("server1.host", props.getProperty("server1.ip", "127.0.0.1"));
        String s1Port = props.getProperty("server1.port", "1099");
        if (s1Host != null && s1Port != null) {
            config.servers.add(new ServerInfo(1, "Server 1", s1Host.trim(), Integer.parseInt(s1Port.trim())));
        }

        String s2Host = props.getProperty("server2.host", props.getProperty("server2.ip", "127.0.0.1"));
        String s2Port = props.getProperty("server2.port", "1098");
        if (s2Host != null && s2Port != null) {
            config.servers.add(new ServerInfo(2, "Server 2", s2Host.trim(), Integer.parseInt(s2Port.trim())));
        }

        // 2. Hỗ trợ dạng danh sách servers=IP:Port,IP:Port nếu có
        String serversStr = props.getProperty("servers");
        if (serversStr != null && !serversStr.isBlank()) {
            config.servers.clear();
            String[] parts = serversStr.split(",");
            int id = 1;
            for (String p : parts) {
                String trimmed = p.trim();
                if (!trimmed.isEmpty()) {
                    String[] hp = trimmed.split(":");
                    String host = hp[0].trim();
                    int port = Integer.parseInt(hp[1].trim());
                    config.servers.add(new ServerInfo(id, "Server " + id, host, port));
                    id++;
                }
            }
        }

        return config;
    }

    public List<ServerInfo> getServers() {
        return servers;
    }

    public int getTimeoutMs() {
        return timeoutMs;
    }
}
