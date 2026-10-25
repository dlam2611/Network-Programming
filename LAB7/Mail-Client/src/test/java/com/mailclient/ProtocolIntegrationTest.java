package com.mailclient;

import com.mailclient.model.EmailAttachment;
import com.mailclient.model.EmailMessage;
import com.mailclient.model.SecurityType;
import com.mailclient.model.ServerConfig;
import com.mailclient.protocol.common.EmailParser;
import com.mailclient.protocol.pop3.Pop3Client;
import com.mailclient.protocol.pop3.Pop3MessageInfo;
import com.mailclient.protocol.pop3.Pop3Stat;
import com.mailclient.protocol.smtp.SmtpClient;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Kiểm thử tự động giao thức SMTP và POP3 bằng cách tạo máy chủ Socket giả lập (Mock Server).
 */
public class ProtocolIntegrationTest {

    @Test
    public void testSmtpProtocolExchange() throws Exception {
        // Khởi tạo Mock SMTP Server trên cổng ngẫu nhiên
        try (ServerSocket serverSocket = new ServerSocket(0)) {
            int port = serverSocket.getLocalPort();

            Thread serverThread = new Thread(() -> {
                try (Socket client = serverSocket.accept();
                     BufferedReader reader = new BufferedReader(new InputStreamReader(client.getInputStream(), StandardCharsets.UTF_8));
                     BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(client.getOutputStream(), StandardCharsets.UTF_8))) {

                    // 1. Gửi Greeting Banner
                    writer.write("220 mock.smtp.server ESMTP Ready\r\n");
                    writer.flush();

                    // 2. Nhận EHLO
                    String line = reader.readLine();
                    assertTrue(line.startsWith("EHLO") || line.startsWith("HELO"));
                    writer.write("250-mock.smtp.server Hello\r\n250 AUTH LOGIN\r\n");
                    writer.flush();

                    // 3. Nhận AUTH LOGIN
                    line = reader.readLine();
                    assertEquals("AUTH LOGIN", line);
                    writer.write("334 VXNlcm5hbWU6\r\n"); // "Username:" in Base64
                    writer.flush();

                    // 4. Nhận Username
                    line = reader.readLine();
                    assertEquals(Base64.getEncoder().encodeToString("testuser".getBytes(StandardCharsets.UTF_8)), line);
                    writer.write("334 UGFzc3dvcmQ6\r\n"); // "Password:" in Base64
                    writer.flush();

                    // 5. Nhận Password
                    line = reader.readLine();
                    assertEquals(Base64.getEncoder().encodeToString("testpass".getBytes(StandardCharsets.UTF_8)), line);
                    writer.write("235 Authentication succeeded\r\n");
                    writer.flush();

                    // 6. Nhận MAIL FROM
                    line = reader.readLine();
                    assertTrue(line.startsWith("MAIL FROM:<testuser>"));
                    writer.write("250 OK\r\n");
                    writer.flush();

                    // 7. Nhận RCPT TO
                    line = reader.readLine();
                    assertTrue(line.startsWith("RCPT TO:<receiver@example.com>"));
                    writer.write("250 OK\r\n");
                    writer.flush();

                    // 8. Nhận DATA
                    line = reader.readLine();
                    assertEquals("DATA", line);
                    writer.write("354 Start mail input; end with <CRLF>.<CRLF>\r\n");
                    writer.flush();

                    // 9. Đọc nội dung email cho tới khi gặp "\r\n.\r\n"
                    while (true) {
                        line = reader.readLine();
                        if (line == null || line.equals(".")) break;
                    }
                    writer.write("250 OK: Message accepted for delivery\r\n");
                    writer.flush();

                    // 10. Nhận QUIT
                    line = reader.readLine();
                    assertEquals("QUIT", line);
                    writer.write("221 Bye\r\n");
                    writer.flush();

                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            serverThread.start();

            // Khởi chạy Client kết nối tới Mock Server
            ServerConfig config = new ServerConfig("127.0.0.1", port, "testuser", "testpass", SecurityType.NONE);
            EmailMessage msg = new EmailMessage();
            msg.setFrom("testuser");
            msg.addTo("receiver@example.com");
            msg.setSubject("Tiêu đề thử nghiệm");
            msg.setBody("Nội dung thử nghiệm giao thức SMTP");

            try (SmtpClient client = new SmtpClient(config, null)) {
                client.connect();
                client.sendEmail(msg);
                client.quit();
            }

            serverThread.join(5000);
        }
    }

    @Test
    public void testPop3ProtocolExchange() throws Exception {
        // Khởi tạo Mock POP3 Server trên cổng ngẫu nhiên
        try (ServerSocket serverSocket = new ServerSocket(0)) {
            int port = serverSocket.getLocalPort();

            Thread serverThread = new Thread(() -> {
                try (Socket client = serverSocket.accept();
                     BufferedReader reader = new BufferedReader(new InputStreamReader(client.getInputStream(), StandardCharsets.UTF_8));
                     BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(client.getOutputStream(), StandardCharsets.UTF_8))) {

                    // 1. Greeting
                    writer.write("+OK Mock POP3 Server Ready\r\n");
                    writer.flush();

                    // 2. USER
                    String line = reader.readLine();
                    assertEquals("USER popuser", line);
                    writer.write("+OK User accepted\r\n");
                    writer.flush();

                    // 3. PASS
                    line = reader.readLine();
                    assertEquals("PASS poppass", line);
                    writer.write("+OK Pass accepted\r\n");
                    writer.flush();

                    // 4. STAT
                    line = reader.readLine();
                    assertEquals("STAT", line);
                    writer.write("+OK 2 3200\r\n");
                    writer.flush();

                    // 5. LIST
                    line = reader.readLine();
                    assertEquals("LIST", line);
                    writer.write("+OK 2 messages (3200 octets)\r\n1 1200\r\n2 2000\r\n.\r\n");
                    writer.flush();

                    // 6. RETR 1
                    line = reader.readLine();
                    assertEquals("RETR 1", line);
                    String rawEmail = "From: sender@example.com\r\n" +
                            "To: popuser@example.com\r\n" +
                            "Subject: Thu kiem tra POP3\r\n" +
                            "Date: Sun, 25 Oct 2026 12:00:00 +0700\r\n" +
                            "Content-Type: text/plain; charset=UTF-8\r\n\r\n" +
                            "Xin chao, day la thu POP3 test!\r\n";
                    writer.write("+OK 1200 octets\r\n" + rawEmail + ".\r\n");
                    writer.flush();

                    // 7. DELE 1
                    line = reader.readLine();
                    assertEquals("DELE 1", line);
                    writer.write("+OK Message 1 deleted\r\n");
                    writer.flush();

                    // 8. QUIT
                    line = reader.readLine();
                    assertEquals("QUIT", line);
                    writer.write("+OK Bye\r\n");
                    writer.flush();

                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            serverThread.start();

            // Khởi chạy Client kết nối tới Mock POP3 Server
            ServerConfig config = new ServerConfig("127.0.0.1", port, "popuser", "poppass", SecurityType.NONE);

            try (Pop3Client client = new Pop3Client(config, null)) {
                client.connect();
                client.login("popuser", "poppass");

                Pop3Stat stat = client.stat();
                assertEquals(2, stat.getMessageCount());
                assertEquals(3200, stat.getTotalOctets());

                List<Pop3MessageInfo> list = client.list();
                assertEquals(2, list.size());
                assertEquals(1, list.get(0).getMessageNumber());
                assertEquals(1200, list.get(0).getOctets());

                String raw = client.retr(1);
                assertTrue(raw.contains("Thu kiem tra POP3"));

                EmailMessage parsed = EmailParser.parse(raw, 1, raw.length());
                assertEquals("Thu kiem tra POP3", parsed.getSubject());
                assertEquals("sender@example.com", parsed.getFrom());
                assertTrue(parsed.getBody().contains("Xin chao, day la thu POP3 test!"));

                client.dele(1);
                client.quit();
            }

            serverThread.join(5000);
        }
    }

    @Test
    public void testEmailParserMimeAttachment() {
        String boundary = "===Part_12345===";
        String raw = "From: Alice <alice@test.com>\r\n" +
                "To: Bob <bob@test.com>\r\n" +
                "Subject: Test Attachment\r\n" +
                "MIME-Version: 1.0\r\n" +
                "Content-Type: multipart/mixed; boundary=\"" + boundary + "\"\r\n\r\n" +
                "--" + boundary + "\r\n" +
                "Content-Type: text/plain; charset=UTF-8\r\n\r\n" +
                "Hello world body!\r\n" +
                "--" + boundary + "\r\n" +
                "Content-Type: text/plain; name=\"hello.txt\"\r\n" +
                "Content-Disposition: attachment; filename=\"hello.txt\"\r\n" +
                "Content-Transfer-Encoding: base64\r\n\r\n" +
                Base64.getEncoder().encodeToString("File content 123".getBytes(StandardCharsets.UTF_8)) + "\r\n" +
                "--" + boundary + "--\r\n";

        EmailMessage email = EmailParser.parse(raw, 1, raw.length());
        assertEquals("Test Attachment", email.getSubject());
        assertEquals("Alice <alice@test.com>", email.getFrom());
        assertTrue(email.getBody().contains("Hello world body!"));
        assertEquals(1, email.getAttachments().size());
        EmailAttachment att = email.getAttachments().get(0);
        assertEquals("hello.txt", att.getFileName());
        assertEquals("File content 123", new String(att.getData(), StandardCharsets.UTF_8));
    }
}
