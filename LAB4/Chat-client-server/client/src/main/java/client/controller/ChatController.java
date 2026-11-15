package client.controller;

import client.model.ChatModel;
import client.net.ServerConnection;
import client.service.AudioService;
import common.Message;
import common.Message.Type;

import java.io.File;
import java.nio.file.Files;
import java.util.concurrent.Executor;

/**
 * Cầu nối giữa giao diện (View) và logic (model, mạng, âm thanh).
 * Giao diện chỉ gọi các hàm ở đây và nhận kết quả qua interface View.
 */
public class ChatController implements ServerConnection.Listener {
    /** Những gì giao diện phải cung cấp. Luôn được gọi trên luồng giao diện. */
    public interface View {
        void onUsersChanged();
        void onMessageForCurrentChat(Message m);
        void onUnreadChanged();
        void onRecordingChanged(boolean recording);
        void onError(String text);
        void onDisconnected();
    }

    private static final long MAX_FILE = 20L * 1024 * 1024;

    private final ChatModel model;
    private final ServerConnection conn;
    private final Executor ui;   // chuyển việc về luồng giao diện (SwingUtilities::invokeLater)
    private final AudioService audio = new AudioService();
    private View view;

    public ChatController(ChatModel model, ServerConnection conn, Executor ui) {
        this.model = model;
        this.conn = conn;
        this.ui = ui;
    }

    public ChatModel getModel() { return model; }

    public void attach(View view) {
        this.view = view;
        conn.listen(this);
    }

    // ---- nhận từ mạng (luồng nền) ----
    @Override public void onMessage(Message m) {
        ui.execute(() -> {
            if (m.type == Type.USERS) {
                model.setOnline(m.users);
                view.onUsersChanged();
            } else {
                String key = model.receive(m);
                if (key.equals(model.getCurrent())) view.onMessageForCurrentChat(m);
                else view.onUnreadChanged();
            }
        });
    }

    @Override public void onDisconnected() {
        ui.execute(() -> view.onDisconnected());
    }

    // ---- hành động từ giao diện ----
    public void selectChat(String key) { model.select(key); }

    public void sendText(String text) {
        text = text.trim();
        if (text.isEmpty()) return;
        Message m = new Message(Type.TEXT);
        m.text = text;
        send(m);
    }

    public void sendFile(File f) {
        if (f.length() > MAX_FILE) {
            view.onError("File quá lớn (tối đa 20 MB)!");
            return;
        }
        try {
            Message m = new Message(Type.FILE);
            m.fileName = f.getName();
            m.data = Files.readAllBytes(f.toPath());
            send(m);
        } catch (Exception e) {
            view.onError("Không đọc được file: " + e.getMessage());
        }
    }

    public void toggleRecording() {
        try {
            if (!audio.isRecording()) {
                audio.startRecording();
                view.onRecordingChanged(true);
            } else {
                byte[] wav = audio.stopRecording();
                view.onRecordingChanged(false);
                if (wav != null) {
                    Message m = new Message(Type.VOICE);
                    m.data = wav;
                    m.text = String.valueOf(audio.getLastDuration());
                    send(m);
                }
            }
        } catch (Exception e) {
            view.onRecordingChanged(false);
            view.onError("Lỗi micro: " + e.getMessage());
        }
    }

    public void saveFile(Message m, File dest) {
        try {
            Files.write(dest.toPath(), m.data);
        } catch (Exception e) {
            view.onError("Lưu thất bại: " + e.getMessage());
        }
    }

    public void playVoice(Message m) {
        try {
            AudioService.play(m.data);
        } catch (Exception e) {
            view.onError("Không phát được: " + e.getMessage());
        }
    }

    public void disconnect() { conn.close(); }

    private void send(Message m) {
        model.addOutgoing(m);
        view.onMessageForCurrentChat(m);
        conn.send(m, err -> ui.execute(() -> view.onError("Gửi thất bại: " + err)));
    }
}