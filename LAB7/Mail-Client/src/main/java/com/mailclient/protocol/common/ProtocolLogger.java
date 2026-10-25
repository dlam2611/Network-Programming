package com.mailclient.protocol.common;

/**
 * Interface cho việc lắng nghe và ghi nhận các gói tin, lệnh, phản hồi
 * trong quá trình giao tiếp qua giao thức mạng (SMTP / POP3).
 */
public interface ProtocolLogger {
    /**
     * Ghi nhận dữ liệu/lệnh gửi từ Client đến Server.
     */
    void logSent(String message);

    /**
     * Ghi nhận phản hồi nhận được từ Server về Client.
     */
    void logReceived(String message);

    /**
     * Ghi nhận thông tin trạng thái kết nối hoặc tiến trình.
     */
    void logInfo(String message);

    /**
     * Ghi nhận thông báo lỗi.
     */
    void logError(String message);
}
