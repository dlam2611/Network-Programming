package com.multiserver.client;

import com.formdev.flatlaf.FlatLightLaf;
import com.multiserver.client.config.ClientConfig;
import com.multiserver.client.service.ClientBankingService;
import com.multiserver.client.ui.AuthFrame;

import javax.swing.*;

/**
 * Điểm khởi chạy của Ứng dụng Client Ngân Hàng.
 */
public class ClientMain {

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(new FlatLightLaf());
        } catch (Exception ignored) {}

        String configFile = args.length > 0 ? args[0] : "config/client.properties";
        System.out.println("Client đang nạp cấu hình từ: " + configFile);

        ClientConfig config = ClientConfig.load(configFile);
        ClientBankingService service = new ClientBankingService(config);

        // Thử kết nối ngẫu nhiên ban đầu tới một trong các server đang chạy
        boolean connected = service.connectRandomActiveServer();
        if (connected) {
            System.out.println("Kết nối ban đầu thành công tới: " + service.getCurrentServer());
        } else {
            System.out.println("Hệ thống server chưa sẵn sàng, đang chờ...");
        }

        if (!java.awt.GraphicsEnvironment.isHeadless()) {
            SwingUtilities.invokeLater(() -> {
                AuthFrame authFrame = new AuthFrame(service);
                authFrame.setVisible(true);
            });
        }
    }
}
