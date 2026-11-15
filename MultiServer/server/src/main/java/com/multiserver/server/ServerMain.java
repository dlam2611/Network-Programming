package com.multiserver.server;

import com.formdev.flatlaf.FlatLightLaf;
import com.multiserver.server.config.ServerConfig;
import com.multiserver.server.dao.UserDAO;
import com.multiserver.server.database.DBConnection;
import com.multiserver.server.database.DatabaseInitializer;
import com.multiserver.server.service.BankServiceImpl;
import com.multiserver.server.service.PeerSyncManager;
import com.multiserver.server.ui.ServerGUI;

import javax.swing.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

/**
 * Điểm khởi chạy của Server Ngân Hàng.
 * Mỗi Server chỉ quản lý Database của chính nó (RMIDATABASE)
 * và chỉ lưu địa chỉ IP + Port của Server khác để gọi đồng bộ RMI.
 */
public class ServerMain {

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(new FlatLightLaf());
        } catch (Exception ignored) {}

        String configFile = "config/server.properties";
        boolean isInstance2 = false;

        if (args.length > 0) {
            String arg = args[0].trim();
            if ("2".equals(arg) || "server2".equalsIgnoreCase(arg)) {
                isInstance2 = true;
            } else if (!"1".equals(arg) && !"server1".equalsIgnoreCase(arg)) {
                configFile = arg;
            }
        }

        System.out.println("Đang nạp cấu hình từ: " + configFile);
        ServerConfig config = ServerConfig.load(configFile);

        // Nếu chạy đóng vai Server 2 trên cùng một máy, hoán đổi Port lắng nghe và Port đối ứng
        if (isInstance2) {
            int myPort = config.getServerPort();
            int peerPort = config.getPeerPort();
            config.setServerName("Server 2");
            config.setServerPort(peerPort);
            config.setPeerName("Server 1");
            config.setPeerPort(myPort);
        }

        System.out.println("Thông tin Server khởi chạy: " + config);

        // Đảm bảo RMI sử dụng đúng IP
        System.setProperty("java.rmi.server.hostname", config.getServerHost());

        // 1. Tự động khởi tạo Database của Server và bảng 'users' nếu chưa có
        DatabaseInitializer.initialize(config);

        // 2. Khởi tạo DAO & kết nối DB của chính Server này
        DBConnection dbConnection = new DBConnection(config);
        if (!dbConnection.testConnection()) {
            System.err.println("CẢNH BÁO: Không thể kết nối tới cơ sở dữ liệu " + config.getDbUrl());
            if (!java.awt.GraphicsEnvironment.isHeadless()) {
                JOptionPane.showMessageDialog(null,
                        "Không thể kết nối cơ sở dữ liệu PostgreSQL!\nURL: " + config.getDbUrl() +
                                "\nVui lòng kiểm tra PostgreSQL đang chạy và mật khẩu trong file cấu hình.",
                        "Lỗi kết nối CSDL", JOptionPane.ERROR_MESSAGE);
            }
        }

        UserDAO userDAO = new UserDAO(dbConnection);

        // 3. Khởi tạo PeerSyncManager để đồng bộ với Server đối ứng (chỉ dùng IP:Port của họ)
        PeerSyncManager peerSyncManager = new PeerSyncManager(config, userDAO);

        try {
            // 4. Khởi tạo dịch vụ RMI BankServiceImpl
            BankServiceImpl bankService = new BankServiceImpl(config, userDAO, peerSyncManager);

            // 5. Mở RMI Registry và bind dịch vụ
            Registry registry;
            try {
                registry = LocateRegistry.createRegistry(config.getServerPort());
            } catch (Exception e) {
                registry = LocateRegistry.getRegistry(config.getServerPort());
            }
            registry.rebind("BankService", bankService);

            // 6. Khởi động GUI Server (nếu có môi trường đồ họa)
            if (!java.awt.GraphicsEnvironment.isHeadless()) {
                SwingUtilities.invokeLater(() -> {
                    ServerGUI gui = new ServerGUI(config, userDAO, peerSyncManager);
                    bankService.addEventListener(gui);
                    peerSyncManager.setStatusListener(gui);
                    gui.setVisible(true);

                    // Bắt đầu luồng kiểm tra và đồng bộ Peer Server qua IP:Port của họ
                    peerSyncManager.start();
                });
            } else {
                peerSyncManager.start();
            }

            System.out.println("=== " + config.getServerName() + " ĐÃ KHỞI CHẠY THÀNH CÔNG TRÊN PORT " + config.getServerPort() + " ===");

        } catch (Exception e) {
            e.printStackTrace();
            if (!java.awt.GraphicsEnvironment.isHeadless()) {
                JOptionPane.showMessageDialog(null, "Lỗi khởi động Server RMI: " + e.getMessage(), "Lỗi Fatal", JOptionPane.ERROR_MESSAGE);
            }
            System.exit(1);
        }
    }
}
