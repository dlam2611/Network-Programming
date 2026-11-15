package com.multiserver.server.database;

import com.multiserver.server.config.ServerConfig;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

/**
 * Tự động tạo cơ sở dữ liệu (nếu chưa có) và khởi tạo bảng users theo đúng đề bài:
 * CREATE TABLE IF NOT EXISTS users (
 *     user_id SERIAL PRIMARY KEY,
 *     name VARCHAR(100) NOT NULL,
 *     email VARCHAR(100) UNIQUE NOT NULL,
 *     password VARCHAR(255) NOT NULL,
 *     balance NUMERIC(15, 2) DEFAULT 0.00 NOT NULL,
 *     last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
 * );
 */
public class DatabaseInitializer {

    public static void initialize(ServerConfig config) {
        // 1. Kiểm tra và cố gắng tạo Database nếu chưa tồn tại
        tryCreateDatabaseIfNotExists(config);

        // 2. Tạo bảng users trong database chỉ định
        String createTableSQL = "CREATE TABLE IF NOT EXISTS users (\n" +
                "    user_id SERIAL PRIMARY KEY,\n" +
                "    name VARCHAR(100) NOT NULL,\n" +
                "    email VARCHAR(100) UNIQUE NOT NULL,\n" +
                "    password VARCHAR(255) NOT NULL,\n" +
                "    balance NUMERIC(15, 2) DEFAULT 0.00 NOT NULL,\n" +
                "    last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL\n" +
                ");";

        try (Connection conn = DriverManager.getConnection(config.getDbUrl(), config.getDbUser(), config.getDbPassword());
             Statement stmt = conn.createStatement()) {
            stmt.execute(createTableSQL);
            System.out.println("[" + config.getServerName() + "] Bảng 'users' đã sẵn sàng trong DB.");
        } catch (Exception e) {
            System.err.println("[" + config.getServerName() + "] Lỗi khởi tạo bảng users: " + e.getMessage());
        }
    }

    private static void tryCreateDatabaseIfNotExists(ServerConfig config) {
        try {
            // Tách tên DB từ URL jdbc:postgresql://localhost:5432/RMIDATABASE
            String dbUrl = config.getDbUrl();
            int lastSlash = dbUrl.lastIndexOf('/');
            if (lastSlash > 0) {
                String baseUrl = dbUrl.substring(0, lastSlash + 1) + "postgres";
                String targetDbName = dbUrl.substring(lastSlash + 1);
                int questionMark = targetDbName.indexOf('?');
                if (questionMark > 0) {
                    targetDbName = targetDbName.substring(0, questionMark);
                }

                try (Connection conn = DriverManager.getConnection(baseUrl, config.getDbUser(), config.getDbPassword());
                     Statement stmt = conn.createStatement()) {
                    // Kiểm tra database đã có chưa
                    var rs = stmt.executeQuery("SELECT 1 FROM pg_database WHERE datname = '" + targetDbName.toLowerCase() + "' OR datname = '" + targetDbName + "'");
                    if (!rs.next()) {
                        System.out.println("Đang tạo cơ sở dữ liệu '" + targetDbName + "'...");
                        stmt.execute("CREATE DATABASE \"" + targetDbName + "\"");
                    }
                }
            }
        } catch (Exception e) {
            // Bỏ qua nếu không có quyền tạo DB hoặc DB đã tồn tại
        }
    }
}

