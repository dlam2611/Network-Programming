package com.multiserver.client.config;

import com.multiserver.common.model.ServerInfo;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Đọc cấu hình Client từ duy nhất file client.properties:
 * Không có bất kỳ giá trị hardcode/mặc định nào trong code.
 * Mọi giá trị (IP, port của Server 1 và Server 2) bắt buộc phải đọc từ file.
 */
public class ClientConfig {
    private final List<ServerInfo> servers = new ArrayList<>();
    private int timeoutMs;

    public ClientConfig() {
    }

    public static ClientConfig load(String filePath) {
        ClientConfig config = new ClientConfig();

        // Tìm kiếm file tại 1 chỗ duy nhất: thư mục config/ của dự án
        File file = new File(filePath);
        if (!file.exists()) {
            file = new File("../" + filePath);
        }
        if (!file.exists()) {
            throw new IllegalStateException("LỖI: Không tìm thấy file cấu hình client tại: " + file.getAbsolutePath());
        }

        System.out.println("Client đang nạp cấu hình từ duy nhất file: " + file.getAbsolutePath());

        Properties props = new Properties();
        try (InputStream in = new FileInputStream(file)) {
            props.load(in);
        } catch (Exception e) {
            throw new RuntimeException("Không thể đọc file cấu hình: " + file.getAbsolutePath(), e);
        }

        // Đọc tường minh mọi cấu hình từ file, KHÔNG có giá trị hardcode trong code
        config.timeoutMs = Integer.parseInt(getRequiredProperty(props, "connect.timeout", file));

        String s1Host = getRequiredProperty(props, "server1.host", file);
        String s1Port = getRequiredProperty(props, "server1.port", file);
        config.servers.add(new ServerInfo(1, "Server 1", s1Host, Integer.parseInt(s1Port)));

        String s2Host = getRequiredProperty(props, "server2.host", file);
        String s2Port = getRequiredProperty(props, "server2.port", file);
        config.servers.add(new ServerInfo(2, "Server 2", s2Host, Integer.parseInt(s2Port)));

        // Hỗ trợ thêm trường hợp cấu hình danh sách servers nếu có
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

        System.out.println("Danh sách máy chủ đọc từ file cấu hình: " + config.servers);
        return config;
    }

    private static String getRequiredProperty(Properties props, String key, File file) {
        String val = props.getProperty(key);
        if (val == null || val.trim().isEmpty()) {
            throw new IllegalStateException("Lỗi cấu hình: Thiếu trường bắt buộc '" + key + "' trong file " + file.getAbsolutePath());
        }
        return val.trim();
    }

    public List<ServerInfo> getServers() {
        return servers;
    }

    public int getTimeoutMs() {
        return timeoutMs;
    }
}
