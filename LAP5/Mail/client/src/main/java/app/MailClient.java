package app;

import ui.MailFrame;

import javax.swing.SwingUtilities;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class MailClient {
    private static final String SERVER_HOST = "127.0.0.1";
    private static final int SERVER_PORT = 2345;
    private static final int TIMEOUT_MS = 3000;
    private static final int BUFFER_SIZE = 8192;

    public void register(String username, String password) throws IOException, MailException {
        request("REGISTER|" + username + "|" + password);
    }

    public List<String> login(String username, String password) throws IOException, MailException {
        String data = request("LOGIN|" + username + "|" + password);
        return data.isEmpty() ? Collections.emptyList() : Arrays.asList(data.split("\\|"));
    }

    public String read(String username, String password, String fileName) throws IOException, MailException {
        return request("READ|" + username + "|" + password + "|" + fileName);
    }

    public void send(String from, String password, String to, String content) throws IOException, MailException {
        request("SEND|" + from + "|" + password + "|" + to + "|" + content);
    }

    private String request(String command) throws IOException, MailException {
        String response = sendPacket(command);
        String[] parts = response.split("\\|", 2);
        String data = parts.length > 1 ? parts[1] : "";

        if (!"OK".equals(parts[0])) {
            throw new MailException(data);
        }
        return data;
    }

    private String sendPacket(String command) throws IOException {
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setSoTimeout(TIMEOUT_MS);

            byte[] sendBuffer = command.getBytes(StandardCharsets.UTF_8);
            socket.send(new DatagramPacket(
                    sendBuffer, sendBuffer.length, InetAddress.getByName(SERVER_HOST), SERVER_PORT));

            DatagramPacket packet = new DatagramPacket(new byte[BUFFER_SIZE], BUFFER_SIZE);
            socket.receive(packet);
            return new String(packet.getData(), packet.getOffset(), packet.getLength(), StandardCharsets.UTF_8);
        }
    }

    public void start() {
        SwingUtilities.invokeLater(() -> new MailFrame(this).setVisible(true));
    }

    public static void main(String[] args) {
        new MailClient().start();
    }
}