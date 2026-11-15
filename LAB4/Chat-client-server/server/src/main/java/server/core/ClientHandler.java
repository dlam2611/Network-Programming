package server.core;

import common.Message;
import common.Message.Type;

import java.io.*;
import java.net.Socket;

/** Xử lý một client: đăng nhập, đọc tin nhắn và chuyển cho ChatServer. */
class ClientHandler implements Runnable {
    private final Socket socket;
    private final ChatServer server;
    private final String ip;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private String user;

    ClientHandler(Socket socket, ChatServer server) {
        this.socket = socket;
        this.server = server;
        this.ip = socket.getInetAddress().getHostAddress() + ":" + socket.getPort();
    }

    String getIp() { return ip; }
    String getUser() { return user; }

    @Override public void run() {
        try {
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            in = new ObjectInputStream(socket.getInputStream());
            server.log(ip, "-", "Kết nối tới server");

            Message m = (Message) in.readObject();
            String name = m.from == null ? "" : m.from.trim();
            if (m.type != Type.LOGIN || !server.register(name, this)) {
                send(new Message(Type.LOGIN_FAIL));
                server.log(ip, name, "Đăng nhập thất bại (trống hoặc trùng username)");
                return;
            }
            user = name;
            send(new Message(Type.LOGIN_OK));
            server.log(ip, user, "Đăng nhập");
            server.broadcastUsers();

            while (true) {
                m = (Message) in.readObject();
                m.from = user;
                m.time = System.currentTimeMillis();
                server.route(this, m);
            }
        } catch (Exception ignored) {
            // client ngắt kết nối
        } finally {
            if (user != null) {
                server.log(ip, user, "Đăng xuất / ngắt kết nối");
                server.unregister(user);
            } else {
                server.log(ip, "-", "Ngắt kết nối");
            }
            try { socket.close(); } catch (IOException ignored) { }
        }
    }

    synchronized void send(Message m) {
        try {
            out.writeObject(m);
            out.flush();
            out.reset();
        } catch (IOException ignored) { }
    }
}