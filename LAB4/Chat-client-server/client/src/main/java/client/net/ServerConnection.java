package client.net;

import common.Message;
import common.Message.Type;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/** Tầng mạng: kết nối socket, đăng nhập, gửi và nhận Message. Không biết gì về giao diện. */
public class ServerConnection {
    public interface Listener {
        void onMessage(Message m);   // gọi từ luồng nền
        void onDisconnected();
    }

    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private volatile boolean closed;
    private final ExecutorService sender = Executors.newSingleThreadExecutor();

    /** @return true nếu đăng nhập thành công, false nếu username bị từ chối */
    public boolean login(String host, int port, String name) throws Exception {
        socket = new Socket();
        socket.connect(new InetSocketAddress(host, port), 3000);
        out = new ObjectOutputStream(socket.getOutputStream());
        out.flush();
        in = new ObjectInputStream(socket.getInputStream());

        Message m = new Message(Type.LOGIN);
        m.from = name;
        out.writeObject(m);
        out.flush();

        if (((Message) in.readObject()).type == Type.LOGIN_OK) return true;
        close();
        return false;
    }

    public void listen(Listener l) {
        Thread t = new Thread(() -> {
            try {
                while (true) l.onMessage((Message) in.readObject());
            } catch (Exception e) {
                if (!closed) l.onDisconnected();
            }
        }, "reader");
        t.setDaemon(true);
        t.start();
    }

    public void send(Message m, Consumer<String> onError) {
        sender.submit(() -> {
            try {
                out.writeObject(m);
                out.flush();
                out.reset();
            } catch (IOException e) {
                onError.accept(e.getMessage());
            }
        });
    }

    public void close() {
        closed = true;
        try { if (socket != null) socket.close(); } catch (IOException ignored) { }
        sender.shutdown();
    }
}