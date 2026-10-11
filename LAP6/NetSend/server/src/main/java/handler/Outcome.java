package handler;

import java.util.List;

// Kết quả xử lý một request: mô tả để ghi log + các gói tin cần gửi đi
public class Outcome {
    private final String description;
    private final List<Reply> replies;

    public Outcome(String description, List<Reply> replies) {
        this.description = description;
        this.replies = replies;
    }

    public String getDescription() {
        return description;
    }

    public List<Reply> getReplies() {
        return replies;
    }
}