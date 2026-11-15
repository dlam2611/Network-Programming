package web.core;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Nhận kết nối TCP và giao cho thread pool xử lý. */
public class HttpServer {
    private final int port;
    private final WebListener listener;
    private final StaticFileService files;
    private final ExecutorService pool = Executors.newFixedThreadPool(64);

    public HttpServer(int port, File root, WebListener listener) {
        this.port = port;
        this.listener = listener;
        this.files = new StaticFileService(root);
    }

    public void start() {
        new Thread(this::acceptLoop, "acceptor").start();
    }

    private void acceptLoop() {
        try (ServerSocket ss = new ServerSocket(port)) {
            listener.onStatus("Server khởi động, lắng nghe cổng " + port);
            while (true) pool.execute(new ClientConnection(ss.accept(), files, listener));
        } catch (IOException e) {
            listener.onStatus("Lỗi server: " + e.getMessage());
        }
    }
}