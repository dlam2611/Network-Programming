package app;

import handler.RequestHandler;
import service.MailService;
import ui.ServerFrame;

import javax.swing.SwingUtilities;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

public class MailServer {
    private static final int SERVER_PORT = 2345;
    private static final int BUFFER_SIZE = 8192;

    private final RequestHandler handler = new RequestHandler(new MailService());
    private final ServerFrame frame;

    public MailServer(ServerFrame frame) {
        this.frame = frame;
    }

    public void start() {
        try (DatagramSocket socket = new DatagramSocket(SERVER_PORT)) {
            frame.log("Server đang chạy trên port: " + SERVER_PORT);
            while (true) {
                DatagramPacket packet = new DatagramPacket(new byte[BUFFER_SIZE], BUFFER_SIZE);
                socket.receive(packet);

                InetAddress clientAddress = packet.getAddress();
                int clientPort = packet.getPort();
                LocalDateTime now = LocalDateTime.now();

                frame.log("Client " + clientAddress.getHostAddress()
                        + ":" + clientPort
                        + " truy cập server lúc " + now);

                String request = new String(
                        packet.getData(), packet.getOffset(), packet.getLength(), StandardCharsets.UTF_8);
                byte[] response = handler.handle(request).getBytes(StandardCharsets.UTF_8);
                socket.send(new DatagramPacket(response, response.length, clientAddress, clientPort));
            }
        } catch (IOException e) {
            frame.log("Lỗi server: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ServerFrame frame = new ServerFrame();
            frame.setVisible(true);

            Thread thread = new Thread(new MailServer(frame)::start, "udp-server");
            thread.setDaemon(true); // đóng cửa sổ thì server tắt theo
            thread.start();
        });
    }
}