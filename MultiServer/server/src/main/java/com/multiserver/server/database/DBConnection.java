package com.multiserver.server.database;

import com.multiserver.server.config.ServerConfig;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Quản lý kết nối JDBC tới PostgreSQL theo ServerConfig.
 */
public class DBConnection {
    private final ServerConfig config;

    public DBConnection(ServerConfig config) {
        this.config = config;
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Không tìm thấy driver PostgreSQL!", e);
        }
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(config.getDbUrl(), config.getDbUser(), config.getDbPassword());
    }

    public boolean testConnection() {
        try (Connection conn = getConnection()) {
            return conn != null && !conn.isClosed();
        } catch (SQLException e) {
            System.err.println("Kiểm tra kết nối DB thất bại: " + e.getMessage());
            return false;
        }
    }
}

