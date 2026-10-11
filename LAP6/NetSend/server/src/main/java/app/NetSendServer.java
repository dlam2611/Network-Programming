package app;

import handler.Outcome;
import handler.Reply;
import handler.RequestHandler;
import service.ClientRegistry;
import ui.ServerFrame;

import javax.swing.SwingUtilities;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

public class NetSendServer {
    private static final int SERVER_PORT = 5000;
    private static final int BUFFER_SIZE = 8192;

    private final RequestHandler handler = new RequestHandler(new ClientRegistry());
    private final ServerFrame frame;

    public NetSendServer(ServerFrame frame) {
        this.frame = frame;
    }

    public void start() {
        try (DatagramSocket socket = new DatagramSocket(SERVER_PORT)) {
            frame.log("Server đang chạy trên port: " + SERVER_PORT);
            while (true) {
                DatagramPacket packet = new DatagramPacket(new byte[BUFFER_SIZE], BUFFER_SIZE);
                socket.receive(packet);

                InetSocketAddress sender = (InetSocketAddress) packet.getSocketAddress();
                String request = new String(
                        packet.getData(), packet.getOffset(), packet.getLength(), StandardCharsets.UTF_8);

                Outcome outcome = handler.handle(request, sender);
                frame.log("[" + LocalDateTime.now() + "] Client " + sender.getAddress().getHostAddress()
                        + ":" + sender.getPort() + "\n    " + outcome.getDescription());

                for (Reply reply : outcome.getReplies()) {
                    byte[] data = reply.getText().getBytes(StandardCharsets.UTF_8);
                    socket.send(new DatagramPacket(data, data.length, reply.getTarget()));
                }
            }
        } catch (IOException e) {
            frame.log("Lỗi server: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ServerFrame frame = new ServerFrame();
            frame.setVisible(true);

            Thread thread = new Thread(new NetSendServer(frame)::start, "udp-server");
            thread.setDaemon(true);
            thread.start();
        });
    }
}