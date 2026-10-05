import model.ExchangeRate;
import service.ExchangeRateService;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

public class ExchangeRateServer {
    private static final int SERVER_PORT = 2345;
    private final ExchangeRateService service;
    public ExchangeRateServer(){
        service=new ExchangeRateService();
    }
    public void start(){
        try(DatagramSocket  socket=new DatagramSocket(SERVER_PORT)){
            System.out.println("server đang chạy trên port: "+SERVER_PORT);
            while(true){
                byte[] receiveBuffer=new byte[1024];
                DatagramPacket receivePacket=new DatagramPacket(receiveBuffer, receiveBuffer.length);
                socket.receive(receivePacket);
                InetAddress clientAddress=receivePacket.getAddress();
                int clientPort = receivePacket.getPort();
                LocalDateTime now = LocalDateTime.now();

                System.out.println(
                        "Client " + clientAddress.getHostAddress()
                                + ":" + clientPort
                                + " truy cập server lúc " + now
                );
                String request = new String(
                        receivePacket.getData(),
                        receivePacket.getOffset(),
                        receivePacket.getLength(),
                        StandardCharsets.UTF_8
                ).trim();

                String response;
                switch (request) {
                    case "GET_RATE":
                        response = service.getLatestRate().toProtocolString();
                        break;
                    case "GET_TIME":
                        response = LocalDateTime.now().toString();
                        break;
                    default:
                        response = "ERROR|Lệnh không hợp lệ: " + request;
                }
                byte[] sendBuffer = response.getBytes(StandardCharsets.UTF_8);
                socket.send(new DatagramPacket(sendBuffer, sendBuffer.length, clientAddress, clientPort));

            }
        }catch(Exception e){
            System.out.println("error"+e.getMessage());
        }
    }
    public static void main(String[] args) { ExchangeRateServer server = new ExchangeRateServer(); server.start(); }
}
