package common;

import java.io.Serializable;
import java.util.List;

/** Gói tin trao đổi giữa client và server (bản sao giống hệt nằm ở cả 2 folder). */
public class Message implements Serializable {
    private static final long serialVersionUID = 1L;
    public static final String GROUP = "ALL"; // đích của nhắn tin nhóm

    public enum Type { LOGIN, LOGIN_OK, LOGIN_FAIL, USERS, TEXT, FILE, VOICE }

    public Type type;
    public String from, to, text, fileName;
    public byte[] data;          // nội dung file / voice (WAV)
    public List<String> users;   // danh sách online (type = USERS)
    public long time = System.currentTimeMillis();

    public Message(Type type) { this.type = type; }
}