package com.mailclient.protocol.smtp;

import com.mailclient.model.EmailAttachment;
import com.mailclient.model.EmailMessage;
import com.mailclient.model.SecurityType;
import com.mailclient.model.ServerConfig;
import com.mailclient.protocol.common.ProtocolLogger;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Cài đặt giao thức SMTP Client hoàn chỉnh qua Socket TCP/SSL.
 * Tuân thủ chuẩn RFC 5321 và RFC 2045/2046 (MIME).
 */
public class SmtpClient implements AutoCloseable {
    private final ServerConfig config;
    private final ProtocolLogger logger;
    private Socket socket;
    private BufferedReader reader;
    private BufferedWriter writer;
    private boolean isConnected = false;

    public SmtpClient(ServerConfig config, ProtocolLogger logger) {
        this.config = config;
        this.logger = logger;
    }

    /**
     * Kết nối tới máy chủ SMTP, thực hiện bắt tay EHLO/HELO, nâng cấp TLS nếu cần,
     * và đăng nhập (AUTH LOGIN) nếu có thông tin xác thực.
     */
    public void connect() throws IOException, SmtpException {
        logInfo("Đang kết nối tới máy chủ SMTP: " + config.getHost() + ":" + config.getPort() + " (" + config.getSecurityType() + ")...");

        if (config.getSecurityType() == SecurityType.SSL_TLS) {
            SSLSocketFactory ssf = (SSLSocketFactory) SSLSocketFactory.getDefault();
            socket = ssf.createSocket();
            socket.connect(new InetSocketAddress(config.getHost(), config.getPort()), config.getTimeoutMillis());
            ((SSLSocket) socket).startHandshake();
        } else {
            socket = new Socket();
            socket.connect(new InetSocketAddress(config.getHost(), config.getPort()), config.getTimeoutMillis());
        }

        socket.setSoTimeout(config.getTimeoutMillis());
        initStreams();

        // 1. Đọc lời chào (Greeting banner) từ máy chủ (Mã 220)
        SmtpResponse banner = readResponse();
        if (banner.code != 220) {
            throw new SmtpException(banner.code, "Máy chủ SMTP từ chối kết nối: " + banner.message);
        }

        // 2. Gửi lệnh EHLO (hoặc HELO nếu EHLO không hỗ trợ)
        String clientHost = "localhost";
        try {
            clientHost = socket.getLocalAddress().getHostName();
            if (clientHost == null || clientHost.isEmpty()) clientHost = "localhost";
        } catch (Exception ignored) {}

        SmtpResponse ehloResp = sendCommand("EHLO " + clientHost);
        if (ehloResp.code != 250) {
            // Thử lệnh HELO truyền thống
            SmtpResponse heloResp = sendCommand("HELO " + clientHost);
            if (heloResp.code != 250) {
                throw new SmtpException(heloResp.code, "Bắt tay HELO thất bại: " + heloResp.message);
            }
        }

        // 3. Nếu sử dụng STARTTLS, yêu cầu nâng cấp kết nối
        if (config.getSecurityType() == SecurityType.STARTTLS) {
            logInfo("Gửi lệnh STARTTLS để bảo mật đường truyền...");
            SmtpResponse tlsResp = sendCommand("STARTTLS");
            if (tlsResp.code != 220) {
                throw new SmtpException(tlsResp.code, "Máy chủ không chấp nhận STARTTLS: " + tlsResp.message);
            }

            // Nâng cấp Socket hiện tại thành SSLSocket
            SSLSocketFactory ssf = (SSLSocketFactory) SSLSocketFactory.getDefault();
            SSLSocket sslSocket = (SSLSocket) ssf.createSocket(socket, config.getHost(), config.getPort(), true);
            sslSocket.startHandshake();
            this.socket = sslSocket;
            initStreams();
            logInfo("Đã thiết lập kết nối mã hóa TLS thành công.");

            // Sau STARTTLS, phải gửi lại lệnh EHLO
            ehloResp = sendCommand("EHLO " + clientHost);
            if (ehloResp.code != 250) {
                throw new SmtpException(ehloResp.code, "Lỗi EHLO sau STARTTLS: " + ehloResp.message);
            }
        }

        // 4. Xác thực đăng nhập nếu có username
        if (config.getUsername() != null && !config.getUsername().trim().isEmpty()) {
            authenticate();
        }

        isConnected = true;
        logInfo("Đã kết nối và xác thực thành công với máy chủ SMTP!");
    }

    /**
     * Xác thực theo cơ chế AUTH LOGIN chuẩn RFC.
     */
    private void authenticate() throws IOException, SmtpException {
        logInfo("Bắt đầu xác thực người dùng (AUTH LOGIN)...");
        SmtpResponse authResp = sendCommand("AUTH LOGIN");

        // Máy chủ thường trả về mã 334 kèm chuỗi Base64 ("Username:")
        if (authResp.code != 334) {
            throw new SmtpException(authResp.code, "Máy chủ không chấp nhận AUTH LOGIN: " + authResp.message);
        }

        // Gửi Base64 username
        String userBase64 = Base64.getEncoder().encodeToString(config.getUsername().getBytes(StandardCharsets.UTF_8));
        SmtpResponse userResp = sendCommand(userBase64);
        if (userResp.code != 334) {
            throw new SmtpException(userResp.code, "Tài khoản đăng nhập bị từ chối: " + userResp.message);
        }

        // Gửi Base64 password
        String pass = config.getPassword() != null ? config.getPassword() : "";
        String passBase64 = Base64.getEncoder().encodeToString(pass.getBytes(StandardCharsets.UTF_8));
        SmtpResponse passResp = sendCommand(passBase64);

        if (passResp.code != 235) {
            throw new SmtpException(passResp.code, "Đăng nhập thất bại (Sai mật khẩu hoặc App Password): " + passResp.message);
        }
        logInfo("Xác thực SMTP thành công!");
    }

    /**
     * Gửi toàn bộ email qua các bước: MAIL FROM -> RCPT TO -> DATA -> MIME Payload -> kết thúc bằng <CRLF>.<CRLF>.
     */
    public void sendEmail(EmailMessage message) throws IOException, SmtpException {
        if (!isConnected) {
            connect();
        }

        // 1. MAIL FROM:<sender>
        String sender = message.getFrom();
        if (sender == null || sender.trim().isEmpty()) {
            sender = config.getUsername();
        }
        SmtpResponse mailFromResp = sendCommand("MAIL FROM:<" + sender.trim() + ">");
        if (mailFromResp.code != 250) {
            throw new SmtpException(mailFromResp.code, "Lỗi lệnh MAIL FROM: " + mailFromResp.message);
        }

        // 2. RCPT TO:<recipient> cho từng người nhận (To, Cc, Bcc)
        Set<String> allRecipients = new LinkedHashSet<>();
        allRecipients.addAll(message.getTo());
        allRecipients.addAll(message.getCc());
        allRecipients.addAll(message.getBcc());

        if (allRecipients.isEmpty()) {
            throw new SmtpException("Danh sách người nhận (To/Cc/Bcc) trống.");
        }

        for (String rcpt : allRecipients) {
            if (rcpt == null || rcpt.trim().isEmpty()) continue;
            SmtpResponse rcptResp = sendCommand("RCPT TO:<" + rcpt.trim() + ">");
            if (rcptResp.code != 250 && rcptResp.code != 251) {
                throw new SmtpException(rcptResp.code, "Lỗi lệnh RCPT TO đối với [" + rcpt + "]: " + rcptResp.message);
            }
        }

        // 3. DATA
        SmtpResponse dataResp = sendCommand("DATA");
        if (dataResp.code != 354) {
            throw new SmtpException(dataResp.code, "Máy chủ không đồng ý nhận nội dung DATA: " + dataResp.message);
        }

        // 4. Truyền tải nội dung MIME (Headers + Body + Attachments)
        String mimeContent = buildMimePayload(message, sender);

        logInfo("Đang truyền tải nội dung thư và tệp đính kèm...");
        // Ghi nội dung với xử lý dot-stuffing (nếu dòng bắt đầu bằng '.' thì thêm '.')
        sendRawData(mimeContent);

        // 5. Kết thúc khối DATA bằng <CRLF>.<CRLF>
        writer.write("\r\n.\r\n");
        writer.flush();
        if (logger != null) {
            logger.logSent("<CRLF>.<CRLF> (Kết thúc khối DATA)");
        }

        SmtpResponse endResp = readResponse();
        if (endResp.code != 250) {
            throw new SmtpException(endResp.code, "Lỗi khi kết thúc gửi thư: " + endResp.message);
        }

        logInfo("Email đã được gửi thành công tới máy chủ SMTP!");
    }

    /**
     * Gửi lệnh QUIT và đóng kết nối.
     */
    public void quit() {
        if (isConnected && socket != null && !socket.isClosed()) {
            try {
                sendCommand("QUIT");
            } catch (Exception ignored) {
            }
        }
        close();
    }

    @Override
    public void close() {
        isConnected = false;
        try {
            if (writer != null) writer.close();
        } catch (Exception ignored) {}
        try {
            if (reader != null) reader.close();
        } catch (Exception ignored) {}
        try {
            if (socket != null) socket.close();
        } catch (Exception ignored) {}
        logInfo("Đã ngắt kết nối SMTP.");
    }

    /**
     * Xây dựng nội dung email theo chuẩn MIME (RFC 2045/RFC 2822).
     */
    public static String buildMimePayload(EmailMessage message, String sender) {
        StringBuilder sb = new StringBuilder();

        // 1. Headers chuẩn
        sb.append("Date: ").append(DateTimeFormatter.RFC_1123_DATE_TIME.format(ZonedDateTime.now())).append("\r\n");
        sb.append("From: ").append(sender).append("\r\n");
        sb.append("To: ").append(String.join(", ", message.getTo())).append("\r\n");
        if (!message.getCc().isEmpty()) {
            sb.append("Cc: ").append(String.join(", ", message.getCc())).append("\r\n");
        }
        sb.append("Subject: ").append(encodeHeader(message.getSubject())).append("\r\n");
        sb.append("Message-ID: <").append(UUID.randomUUID()).append("@").append("mailclient.local>\r\n");
        sb.append("MIME-Version: 1.0\r\n");

        List<EmailAttachment> attachments = message.getAttachments();

        if (attachments == null || attachments.isEmpty()) {
            // Không có file đính kèm: Gửi trực tiếp dạng text/plain UTF-8
            sb.append("Content-Type: text/plain; charset=UTF-8\r\n");
            sb.append("Content-Transfer-Encoding: 8bit\r\n");
            sb.append("\r\n");
            sb.append(message.getBody());
        } else {
            // Có file đính kèm: Dùng multipart/mixed
            String boundary = "===_NextPart_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8) + "_===";
            sb.append("Content-Type: multipart/mixed; boundary=\"").append(boundary).append("\"\r\n");
            sb.append("\r\n");
            sb.append("This is a multi-part message in MIME format.\r\n");

            // Phần thân thư (Text part)
            sb.append("--").append(boundary).append("\r\n");
            sb.append("Content-Type: text/plain; charset=UTF-8\r\n");
            sb.append("Content-Transfer-Encoding: 8bit\r\n");
            sb.append("\r\n");
            sb.append(message.getBody()).append("\r\n");

            // Các phần đính kèm (Attachment parts)
            for (EmailAttachment att : attachments) {
                sb.append("\r\n--").append(boundary).append("\r\n");
                sb.append("Content-Type: ").append(att.getContentType()).append("; name=\"").append(encodeHeader(att.getFileName())).append("\"\r\n");
                sb.append("Content-Transfer-Encoding: base64\r\n");
                sb.append("Content-Disposition: attachment; filename=\"").append(encodeHeader(att.getFileName())).append("\"\r\n");
                sb.append("\r\n");

                // Mã hóa dữ liệu tệp sang Base64 và cắt theo dòng 76 ký tự (theo RFC 2045)
                String base64Data = att.getBase64Data();
                int offset = 0;
                while (offset < base64Data.length()) {
                    int end = Math.min(offset + 76, base64Data.length());
                    sb.append(base64Data, offset, end).append("\r\n");
                    offset = end;
                }
            }

            // Kết thúc boundary
            sb.append("\r\n--").append(boundary).append("--");
        }

        return sb.toString();
    }

    /**
     * Xử lý gửi chuỗi DATA với quy tắc Dot-Stuffing (RFC 5321 mục 4.5.2).
     */
    private void sendRawData(String data) throws IOException {
        String[] lines = data.split("\r?\n", -1);
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            if (line.startsWith(".")) {
                writer.write("."); // Prepend extra dot
            }
            writer.write(line);
            if (i < lines.length - 1) {
                writer.write("\r\n");
            }
        }
        writer.flush();
    }

    private void initStreams() throws IOException {
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
    }

    private SmtpResponse sendCommand(String command) throws IOException {
        if (logger != null) {
            // Không lộ mật khẩu trong log
            if (isBase64(command) && config.getPassword() != null &&
                    command.equals(Base64.getEncoder().encodeToString(config.getPassword().getBytes(StandardCharsets.UTF_8)))) {
                logger.logSent("****** (Mật khẩu đã mã hóa Base64)");
            } else {
                logger.logSent(command);
            }
        }
        writer.write(command + "\r\n");
        writer.flush();
        return readResponse();
    }

    private SmtpResponse readResponse() throws IOException {
        StringBuilder fullResponse = new StringBuilder();
        int code = -1;

        while (true) {
            String line = reader.readLine();
            if (line == null) {
                throw new IOException("Máy chủ SMTP đóng kết nối bất ngờ.");
            }
            if (logger != null) {
                logger.logReceived(line);
            }
            if (fullResponse.length() > 0) fullResponse.append("\n");
            fullResponse.append(line);

            if (line.length() >= 3) {
                try {
                    code = Integer.parseInt(line.substring(0, 3));
                } catch (NumberFormatException ignored) {}
            }

            // Dòng cuối cùng của response có dấu cách ở ký tự thứ 4 (ví dụ "250 OK")
            // Dòng trung gian có dấu gạch ngang (ví dụ "250-STARTTLS")
            if (line.length() == 3 || line.charAt(3) != '-') {
                break;
            }
        }

        return new SmtpResponse(code, fullResponse.toString());
    }

    private void logInfo(String info) {
        if (logger != null) {
            logger.logInfo(info);
        }
    }

    private static String encodeHeader(String text) {
        if (text == null) return "";
        // Nếu có ký tự non-ASCII, mã hóa theo chuẩn RFC 2047: =?UTF-8?B?...?=
        boolean nonAscii = text.chars().anyMatch(c -> c > 127);
        if (nonAscii) {
            String b64 = Base64.getEncoder().encodeToString(text.getBytes(StandardCharsets.UTF_8));
            return "=?UTF-8?B?" + b64 + "?=";
        }
        return text;
    }

    private static boolean isBase64(String str) {
        if (str == null || str.isEmpty() || str.length() % 4 != 0) return false;
        try {
            Base64.getDecoder().decode(str);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public boolean isConnected() {
        return isConnected && socket != null && !socket.isClosed();
    }

    public static class SmtpResponse {
        public final int code;
        public final String message;

        public SmtpResponse(int code, String message) {
            this.code = code;
            this.message = message;
        }

        @Override
        public String toString() {
            return code + " " + message;
        }
    }
}
