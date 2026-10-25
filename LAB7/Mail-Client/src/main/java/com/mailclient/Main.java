package com.mailclient;

import com.formdev.flatlaf.FlatLightLaf;
import com.mailclient.ui.MainFrame;

import javax.swing.*;

/**
 * Lớp khởi chạy chính của chương trình Mail Client.
 */
public class Main {
    public static void main(String[] args) {
        // Thiết lập giao diện hiện đại (FlatLaf) với cơ chế fallback dự phòng
        try {
            FlatLightLaf.setup();
        } catch (Throwable t) {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {}
        }

        // Khởi tạo giao diện trên luồng Event Dispatch Thread (EDT)
        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame();
            frame.setVisible(true);
        });
    }
}
