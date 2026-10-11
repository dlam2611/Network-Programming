package handler;

import java.net.SocketAddress;

// Một gói tin server cần gửi đi: gửi đến đâu, nội dung gì
public class Reply {
    private final SocketAddress target;
    private final String text;

    public Reply(SocketAddress target, String text) {
        this.target = target;
        this.text = text;
    }

    public SocketAddress getTarget() {
        return target;
    }

    public String getText() {
        return text;
    }
}