package client.model;

import common.Message;

import java.util.*;

/** Dữ liệu của phiên chat: người online, cuộc trò chuyện đang mở, lịch sử, tin chưa đọc. */
public class ChatModel {
    private final String name;
    private List<String> online = new ArrayList<>();
    private String current = Message.GROUP;
    private final Map<String, List<Message>> history = new HashMap<>();
    private final Set<String> unread = new HashSet<>();

    public ChatModel(String name) { this.name = name; }

    public String getName() { return name; }
    public String getCurrent() { return current; }
    public boolean isGroup() { return Message.GROUP.equals(current); }

    public void select(String key) {
        current = key;
        unread.remove(key);
    }

    public void setOnline(List<String> users) { online = users; }
    public boolean isOnline(String user) { return online.contains(user); }

    /** Người online (trừ bản thân) có tên chứa từ khóa. */
    public List<String> searchUsers(String query) {
        String q = query.trim().toLowerCase();
        List<String> r = new ArrayList<>();
        for (String u : online) if (!u.equals(name) && u.toLowerCase().contains(q)) r.add(u);
        return r;
    }

    public List<Message> history(String key) {
        return history.getOrDefault(key, Collections.emptyList());
    }

    /** Lưu tin nhận được, trả về khóa cuộc trò chuyện chứa nó. */
    public String receive(Message m) {
        String key = Message.GROUP.equals(m.to) ? Message.GROUP : m.from;
        history.computeIfAbsent(key, k -> new ArrayList<>()).add(m);
        if (!key.equals(current)) unread.add(key);
        return key;
    }

    /** Gắn người gửi/người nhận cho tin của mình rồi lưu vào cuộc trò chuyện hiện tại. */
    public void addOutgoing(Message m) {
        m.from = name;
        m.to = current;
        history.computeIfAbsent(current, k -> new ArrayList<>()).add(m);
    }

    public boolean isUnread(String key) { return unread.contains(key); }
}