package com.mailclient.protocol.pop3;

import com.mailclient.model.SecurityType;
import com.mailclient.model.ServerConfig;
import com.mailclient.protocol.common.ProtocolLogger;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Cài đặt giao thức nhận thư POP3 Client hoàn chỉnh qua Socket TCP/SSL.
 * Tuân thủ chuẩn RFC 1939.
 */
public class Pop3Client implements AutoCloseable {
    private final ServerConfig config;
    private final ProtocolLogger logger;
    private Socket socket;
    private BufferedReader reader;
    private BufferedWriter writer;
    private boolean isConnected = false;
    private boolean isLoggedIn = false;

    public Pop3Client(ServerConfig config, ProtocolLogger logger) {
        this.config = config;
        this.logger = logger;
    }

    /**
     * Mở kết nối TCP/SSL tới máy chủ POP3 và đọc thông điệp chào mừng (+OK).
     */
    public void connect() throws IOException, Pop3Exception {
        logInfo("Đang kết nối tới máy chủ POP3: " + config.getHost() + ":" + config.getPort() + " (" + config.getSecurityType() + ")...");

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
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));

        // Đọc banner chào mừng từ máy chủ POP3 (bắt đầu bằng +OK)
        String banner = readSingleLineResponse();
        if (!banner.startsWith("+OK")) {
            throw new Pop3Exception("Máy chủ POP3 từ chối kết nối: " + banner);
        }

        isConnected = true;
        logInfo("Đã kết nối thành công tới máy chủ POP3.");
    }

    /**
     * Thực hiện đăng nhập với USER và PASS.
     */
    public void login(String username, String password) throws IOException, Pop3Exception {
        if (!isConnected) {
            connect();
        }

        logInfo("Gửi thông tin xác thực tài khoản POP3...");

        // 1. Gửi USER <username>
        String userResp = sendCommand("USER " + username);
        if (!userResp.startsWith("+OK")) {
            throw new Pop3Exception("Lệnh USER thất bại: " + userResp);
        }

        // 2. Gửi PASS <password>
        String passResp = sendCommand("PASS " + password, true);
        if (!passResp.startsWith("+OK")) {
            throw new Pop3Exception("Đăng nhập thất bại (Sai mật khẩu hoặc App Password): " + passResp);
        }

        isLoggedIn = true;
        logInfo("Đăng nhập hộp thư POP3 thành công!");
    }

    /**
     * Lấy trạng thái hộp thư (số lượng thư và tổng dung lượng) qua lệnh STAT.
     */
    public Pop3Stat stat() throws IOException, Pop3Exception {
        checkLoggedIn();
        String resp = sendCommand("STAT");
        if (!resp.startsWith("+OK")) {
            throw new Pop3Exception("Lệnh STAT thất bại: " + resp);
        }

        // Định dạng phản hồi: +OK <count> <octets>
        String[] parts = resp.trim().split("\\s+");
        if (parts.length >= 3) {
            try {
                int count = Integer.parseInt(parts[1]);
                long octets = Long.parseLong(parts[2]);
                return new Pop3Stat(count, octets);
            } catch (NumberFormatException e) {
                throw new Pop3Exception("Phản hồi STAT không đúng định dạng: " + resp);
            }
        }
        return new Pop3Stat(0, 0);
    }

    /**
     * Lấy danh sách ID và kích thước các thư qua lệnh LIST.
     */
    public List<Pop3MessageInfo> list() throws IOException, Pop3Exception {
        checkLoggedIn();
        String resp = sendCommand("LIST");
        if (!resp.startsWith("+OK")) {
            throw new Pop3Exception("Lệnh LIST thất bại: " + resp);
        }

        List<Pop3MessageInfo> messageList = new ArrayList<>();
        List<String> lines = readMultiLineResponse();

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) continue;
            String[] parts = trimmed.split("\\s+");
            if (parts.length >= 2) {
                try {
                    int msgNum = Integer.parseInt(parts[0]);
                    long octets = Long.parseLong(parts[1]);
                    messageList.add(new Pop3MessageInfo(msgNum, octets));
                } catch (NumberFormatException ignored) {}
            }
        }

        return messageList;
    }

    /**
     * Tải nội dung thô (RFC 822) của một thư theo số thứ tự qua lệnh RETR.
     */
    public String retr(int messageNumber) throws IOException, Pop3Exception {
        checkLoggedIn();
        logInfo("Đang tải thư #" + messageNumber + " qua lệnh RETR...");
        String resp = sendCommand("RETR " + messageNumber);
        if (!resp.startsWith("+OK")) {
            throw new Pop3Exception("Lệnh RETR thất bại cho thư #" + messageNumber + ": " + resp);
        }

        List<String> lines = readMultiLineResponse();
        StringBuilder raw = new StringBuilder();
        for (String line : lines) {
            // Byte un-stuffing: Nếu dòng bắt đầu bằng "..", loại bỏ dấu '.' đầu tiên
            if (line.startsWith("..")) {
                line = line.substring(1);
            }
            raw.append(line).append("\r\n");
        }

        logInfo("Đã tải xong thư #" + messageNumber + " (" + lines.size() + " dòng).");
        return raw.toString();
    }

    /**
     * Tải header và n dòng đầu tiên của thư qua lệnh TOP.
     */
    public String top(int messageNumber, int nLines) throws IOException, Pop3Exception {
        checkLoggedIn();
        String resp = sendCommand("TOP " + messageNumber + " " + nLines);
        if (!resp.startsWith("+OK")) {
            throw new Pop3Exception("Lệnh TOP thất bại: " + resp);
        }

        List<String> lines = readMultiLineResponse();
        StringBuilder raw = new StringBuilder();
        for (String line : lines) {
            if (line.startsWith("..")) {
                line = line.substring(1);
            }
            raw.append(line).append("\r\n");
        }
        return raw.toString();
    }

    /**
     * Đánh dấu xóa thư qua lệnh DELE. Thư sẽ bị xóa vĩnh viễn khi kết thúc phiên (QUIT).
     */
    public void dele(int messageNumber) throws IOException, Pop3Exception {
        checkLoggedIn();
        String resp = sendCommand("DELE " + messageNumber);
        if (!resp.startsWith("+OK")) {
            throw new Pop3Exception("Lệnh DELE thất bại cho thư #" + messageNumber + ": " + resp);
        }
        logInfo("Đã đánh dấu xóa thư #" + messageNumber + " thành công.");
    }

    /**
     * Khôi phục tất cả các thư đã đánh dấu xóa trong phiên qua lệnh RSET.
     */
    public void rset() throws IOException, Pop3Exception {
        checkLoggedIn();
        String resp = sendCommand("RSET");
        if (!resp.startsWith("+OK")) {
            throw new Pop3Exception("Lệnh RSET thất bại: " + resp);
        }
        logInfo("Đã khôi phục các thư đã đánh dấu xóa (RSET).");
    }

    /**
     * Gửi lệnh NOOP để duy trì kết nối.
     */
    public void noop() throws IOException, Pop3Exception {
        checkLoggedIn();
        String resp = sendCommand("NOOP");
        if (!resp.startsWith("+OK")) {
            throw new Pop3Exception("Lệnh NOOP thất bại: " + resp);
        }
    }

    /**
     * Gửi lệnh QUIT (xác nhận các lệnh DELE và đóng phiên làm việc).
     */
    public void quit() {
        if (isConnected && socket != null && !socket.isClosed()) {
            try {
                sendCommand("QUIT");
            } catch (Exception ignored) {}
        }
        close();
    }

    @Override
    public void close() {
        isConnected = false;
        isLoggedIn = false;
        try {
            if (writer != null) writer.close();
        } catch (Exception ignored) {}
        try {
            if (reader != null) reader.close();
        } catch (Exception ignored) {}
        try {
            if (socket != null) socket.close();
        } catch (Exception ignored) {}
        logInfo("Đã ngắt kết nối POP3.");
    }

    private void checkLoggedIn() throws Pop3Exception {
        if (!isConnected || !isLoggedIn) {
            throw new Pop3Exception("Chưa kết nối hoặc chưa đăng nhập vào máy chủ POP3.");
        }
    }

    private String sendCommand(String command) throws IOException {
        return sendCommand(command, false);
    }

    private String sendCommand(String command, boolean isPassword) throws IOException {
        if (logger != null) {
            if (isPassword) {
                logger.logSent("PASS ********");
            } else {
                logger.logSent(command);
            }
        }
        writer.write(command + "\r\n");
        writer.flush();
        return readSingleLineResponse();
    }

    private String readSingleLineResponse() throws IOException {
        String line = reader.readLine();
        if (line == null) {
            throw new IOException("Máy chủ POP3 ngắt kết nối bất ngờ.");
        }
        if (logger != null) {
            logger.logReceived(line);
        }
        return line;
    }

    /**
     * Đọc chuỗi phản hồi nhiều dòng từ POP3 (kết thúc bằng dòng chỉ chứa dấu chấm ".").
     */
    private List<String> readMultiLineResponse() throws IOException {
        List<String> lines = new ArrayList<>();
        while (true) {
            String line = reader.readLine();
            if (line == null) {
                throw new IOException("Máy chủ POP3 ngắt kết nối giữa chừng khi đọc dữ liệu.");
            }
            if (line.equals(".")) {
                if (logger != null) {
                    logger.logReceived(". (Kết thúc dữ liệu)");
                }
                break;
            }
            // Ghi log chọn lọc để tránh tràn giao diện nếu thư rất dài
            if (logger != null && lines.size() < 20) {
                logger.logReceived(line);
            } else if (logger != null && lines.size() == 20) {
                logger.logReceived("... [các dòng tiếp theo được ẩn bớt trong nhật ký giao thức] ...");
            }
            lines.add(line);
        }
        return lines;
    }

    private void logInfo(String info) {
        if (logger != null) {
            logger.logInfo(info);
        }
    }

    public boolean isConnected() {
        return isConnected && socket != null && !socket.isClosed();
    }

    public boolean isLoggedIn() {
        return isLoggedIn;
    }
}
