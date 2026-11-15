package web;

import web.core.HttpServer;
import web.ui.ServerFrame;

import javax.swing.*;
import java.io.File;
import java.net.InetAddress;

/** Điểm khởi chạy web server. */
public class WebMain {
    static final int PORT = 8080;

    public static void main(String[] args) {
        try { UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel"); } catch (Exception ignored) { }
        File root = new File("www").getAbsoluteFile();
        root.mkdirs();
        String ip = "localhost";
        try { ip = InetAddress.getLocalHost().getHostAddress(); } catch (Exception ignored) { }
        final String info = "Web server: http://" + ip + ":" + PORT + "   |   Thư mục web: " + root;
        SwingUtilities.invokeLater(() -> {
            ServerFrame frame = new ServerFrame(info, "http://localhost:" + PORT);
            frame.setVisible(true);
            new HttpServer(PORT, root, frame).start();
        });
    }
}