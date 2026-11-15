package server.core;

import common.Message;
import common.Message.Type;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Logic server: nhận kết nối, quản lý người online, định tuyến tin nhắn. Không chứa code giao diện. */
public class ChatServer {
    private final int port;
    private final ServerListener listener;
    private final Map<String, ClientHandler> clients = new ConcurrentHashMap<>();

    public ChatServer(int port, ServerListener listener) {
        this.port = port;
        this.listener = listener;
    }

    public void start() {
        new Thread(this::acceptLoop, "acceptor").start();
    }

    private void acceptLoop() {
        try (ServerSocket ss = new ServerSocket(port)) {
            log("-", "-", "Server khởi động, lắng nghe cổng " + port);
            while (true) new Thread(new ClientHandler(ss.accept(), this)).start();
        } catch (IOException e) {
            log("-", "-", "Lỗi server: " + e.getMessage());
        }
    }

    void log(String ip, String user, String action) {
        listener.onLog(ip, user, action);
    }

    /** @return false nếu username rỗng, bị cấm hoặc đã có người dùng */
    boolean register(String name, ClientHandler h) {
        if (name.isEmpty() || name.equalsIgnoreCase(Message.GROUP)) return false;
        return clients.putIfAbsent(name, h) == null;
    }

    void unregister(String name) {
        clients.remove(name);
        broadcastUsers();
    }

    void broadcastUsers() {
        Message m = new Message(Type.USERS);
        m.users = new ArrayList<>(clients.keySet());
        Collections.sort(m.users);
        for (ClientHandler h : clients.values()) h.send(m);
        listener.onOnlineChanged(m.users.size());
    }

    /** Chuyển tin nhắn tới nhóm hoặc tới một người, đồng thời ghi log. */
    void route(ClientHandler sender, Message m) {
        if (m.type != Type.TEXT && m.type != Type.FILE && m.type != Type.VOICE) return;
        boolean group = Message.GROUP.equals(m.to);
        String target = group ? "nhóm chung" : "@" + m.to;
        String what;
        if (m.type == Type.TEXT) what = "Nhắn tin tới " + target + ": " + m.text;
        else if (m.type == Type.FILE)
            what = "Gửi file \"" + m.fileName + "\" (" + (m.data.length / 1024) + " KB) tới " + target;
        else what = "Gửi tin nhắn thoại (" + m.text + "s) tới " + target;
        log(sender.getIp(), sender.getUser(), what);

        if (group) {
            for (ClientHandler h : clients.values()) if (h != sender) h.send(m);
        } else {
            ClientHandler h = clients.get(m.to);
            if (h != null) h.send(m);
        }
    }
}