package server;

import server.core.ChatServer;
import server.ui.ServerFrame;

import javax.swing.*;
import java.net.InetAddress;

/** Điểm khởi chạy server: nối giao diện với logic. */
public class ServerMain {
    static final int PORT = 5555;

    public static void main(String[] args) {
        try { UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel"); } catch (Exception ignored) { }
        String ip = "?";
        try { ip = InetAddress.getLocalHost().getHostAddress(); } catch (Exception ignored) { }
        final String info = "Server chạy tại " + ip + ":" + PORT;
        SwingUtilities.invokeLater(() -> {
            ServerFrame frame = new ServerFrame(info);
            frame.setVisible(true);
            new ChatServer(PORT, frame).start();
        });
    }
}