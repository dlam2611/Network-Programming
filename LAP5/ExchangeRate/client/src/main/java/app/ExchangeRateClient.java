package app;

import model.ExchangeRate;
import ui.ExchangeRateFrame;

import javax.swing.SwingUtilities;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;

public class ExchangeRateClient {
    private static final String SERVER_HOST = "127.0.0.1";
    private static final int SERVER_PORT = 2345;
    private static final int TIMEOUT_MS = 3000;

    private String sendRequest(String command) throws IOException {
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setSoTimeout(TIMEOUT_MS);

            byte[] sendBuffer = command.getBytes(StandardCharsets.UTF_8);
            InetAddress serverAddress = InetAddress.getByName(SERVER_HOST);
            socket.send(new DatagramPacket(sendBuffer, sendBuffer.length, serverAddress, SERVER_PORT));

            byte[] receiveBuffer = new byte[1024];
            DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
            socket.receive(receivePacket);

            return new String(receivePacket.getData(), receivePacket.getOffset(),
                    receivePacket.getLength(), StandardCharsets.UTF_8);
        }
    }

    public ExchangeRate fetchRate() throws IOException {
        return ExchangeRate.fromString(sendRequest("GET_RATE"));
    }

    public void start() {
        System.out.println("Application Run.........");
        SwingUtilities.invokeLater(() -> new ExchangeRateFrame(this).setVisible(true));
    }

    public static void main(String[] args) {
        new ExchangeRateClient().start();
    }
}