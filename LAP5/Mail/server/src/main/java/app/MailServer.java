package app;

import handler.RequestHandler;
import service.MailService;

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

    public void start() {
        try (DatagramSocket socket = new DatagramSocket(SERVER_PORT)) {
            System.out.println("Mail server đang chạy trên port: " + SERVER_PORT);
            while (true) {
                DatagramPacket packet = new DatagramPacket(new byte[BUFFER_SIZE], BUFFER_SIZE);
                socket.receive(packet);
                InetAddress clientAddress=packet.getAddress();
                int clientPort = packet.getPort();
                LocalDateTime now = LocalDateTime.now();

                System.out.println(
                        "Client " + clientAddress.getHostAddress()
                                + ":" + clientPort
                                + " truy cập server lúc " + now
                );
                String request = new String(
                        packet.getData(), packet.getOffset(), packet.getLength(), StandardCharsets.UTF_8);
                System.out.println(LocalDateTime.now() + " | " + packet.getAddress().getHostAddress()
                        + ":" + packet.getPort() + " | " + request.split("\\|")[0]);

                byte[] response = handler.handle(request).getBytes(StandardCharsets.UTF_8);
                socket.send(new DatagramPacket(response, response.length, packet.getAddress(), packet.getPort()));
            }
        } catch (IOException e) {
            System.out.println("Lỗi server: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        new MailServer().start();
    }
}