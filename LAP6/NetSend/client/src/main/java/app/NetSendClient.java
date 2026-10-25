package app;

import ui.NetSendFrame;

import javax.swing.SwingUtilities;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

public class NetSendClient {
    private static final String DEFAULT_SERVER_HOST = "10.203.143.162";
    private static final int SERVER_PORT = 5000;
    private static final int BUFFER_SIZE = 8192;

    // Một socket duy nhất: vừa gửi lệnh lên server, vừa nhận tin server chuyển về
    private final DatagramSocket socket;
    private final InetAddress serverAddress;

    public NetSendClient(String serverHost) throws IOException {
        this.socket = new DatagramSocket();
        this.serverAddress = InetAddress.getByName(serverHost);
    }

    public void hello() throws IOException {
        transmit("HELLO");
    }

    public void bye() throws IOException {
        transmit("BYE");
    }

    public void join(String group) throws IOException {
        transmit("JOIN|" + group);
    }

    public void leave(String group) throws IOException {
        transmit("LEAVE|" + group);
    }

    // Nhận lệnh dạng: net send {IP | group | *} message
    public void sendCommand(String command) throws IOException {
        String[] parts = command.trim().split("\\s+", 4);
        boolean valid = parts.length == 4
                && parts[0].equalsIgnoreCase("net")
                && parts[1].equalsIgnoreCase("send");
        if (!valid) {
            throw new IllegalArgumentException("Cú pháp: net send {IP | group | *} message");
        }
        transmit("SEND|" + parts[2] + "|" + parts[3]);
    }

    // Lắng nghe mọi gói tin server gửi về ở luồng nền
    public void listen(Consumer<String> onMessage) {
        Thread thread = new Thread(() -> {
            while (!socket.isClosed()) {
                try {
                    DatagramPacket packet = new DatagramPacket(new byte[BUFFER_SIZE], BUFFER_SIZE);
                    socket.receive(packet);
                    onMessage.accept(new String(
                            packet.getData(), packet.getOffset(), packet.getLength(), StandardCharsets.UTF_8));
                } catch (IOException e) {
                    if (!socket.isClosed()) {
                        System.out.println("Lỗi nhận tin: " + e.getMessage());
                    }
                }
            }
        }, "receiver");
        thread.setDaemon(true);
        thread.start();
    }

    private void transmit(String text) throws IOException {
        byte[] data = text.getBytes(StandardCharsets.UTF_8);
        socket.send(new DatagramPacket(data, data.length, serverAddress, SERVER_PORT));
    }

    // Chạy: truyền IP server làm tham số (bỏ trống thì dùng 127.0.0.1)
    public static void main(String[] args) throws IOException {
        String host = args.length > 0 ? args[0] : DEFAULT_SERVER_HOST;
        NetSendClient client = new NetSendClient(host);
        SwingUtilities.invokeLater(() -> new NetSendFrame(client).setVisible(true));
    }
}