# Mail Client - Lập trình Giao thức Mạng SMTP & POP3 (Java Swing)

Ứng dụng Mail Client hoàn chỉnh được xây dựng cho học phần **Lập trình Mạng (Network Programming) - LAB 7**, triển khai **giao thức truyền tin SMTP** và **giao thức nhận tin POP3** trực tiếp ở tầng Socket TCP (`java.net.Socket` & `javax.net.ssl.SSLSocket`) thay vì phụ thuộc vào thư viện JavaMail cấp cao.

---

## 🚀 Tính năng nổi bật

### 1. Giao thức truyền tin SMTP (RFC 5321 & MIME RFC 2045/2046)
- **Kết nối Socket đa chế độ**:
  - `Plain TCP` (Port 25, 2525)
  - `SSL / TLS` (Port 465 - SMTPS)
  - `STARTTLS` (Port 587 - Nâng cấp bảo mật sau EHLO)
- **Bắt tay & Xác thực**:
  - Gửi lệnh `EHLO` / `HELO`, đọc phản hồi đa dòng (250).
  - Xác thực người dùng `AUTH LOGIN` mã hóa Base64 tài khoản và mật khẩu (Mã 334 & 235).
- **Quy trình gửi thư**:
  - `MAIL FROM:<người_gửi>` (Mã 250).
  - `RCPT TO:<người_nhận>` hỗ trợ danh sách `To`, `Cc`, `Bcc` (Mã 250).
  - `DATA` và truyền tải tiêu đề MIME chuẩn (`From`, `To`, `Cc`, `Subject`, `Date`, `Message-ID`).
  - Hỗ trợ **gửi tệp đính kèm (Attachments)** qua cấu trúc MIME `multipart/mixed` và mã hóa Base64 cắt dòng 76 ký tự.
  - Xử lý **Dot-stuffing** chuẩn RFC 5321 (thêm dấu `.` khi đầu dòng có dấu `.`).
  - Kết thúc dữ liệu bằng `<CRLF>.<CRLF>`.
  - `QUIT` và đóng kênh truyền tin (Mã 221).
- **Tính năng mở rộng**: Xem trước định dạng bản tin thô (Preview MIME RFC 2822) trước khi gửi.

### 2. Giao thức nhận tin POP3 (RFC 1939)
- **Kết nối Socket**:
  - `Plain TCP` (Port 110)
  - `SSL / TLS` (Port 995 - POP3S)
- **Quy trình tương tác**:
  - Đọc thông điệp chào mừng `+OK`.
  - Xác thực đăng nhập qua `USER <tài_khoản>` và `PASS <mật_khẩu>`.
  - `STAT`: Lấy tổng số lượng thư và tổng dung lượng hộp thư (octets).
  - `LIST`: Liệt kê số thứ tự từng thư và dung lượng.
  - `RETR <stt>`: Tải nội dung bản tin thô, xử lý byte un-stuffing (`..` thành `.`).
  - `DELE <stt>`: Đánh dấu xóa thư trên máy chủ.
  - `RSET`: Khôi phục các thư đã gắn cờ xóa.
  - `QUIT`: Xác nhận xóa và ngắt kết nối.
- **Bộ giải mã Email (EmailParser)**:
  - Phân tích cú pháp RFC 822 / RFC 2822: Header folding, Date, Subject, From, To, Cc.
  - Giải mã MIME Encoded-words (`=?UTF-8?B?...?=` và `=?UTF-8?Q?...?=`).
  - Tách nội dung văn bản (Plain text / HTML) và **trích xuất tệp đính kèm**.
  - Cho phép **lưu tệp đính kèm về máy tính**.

### 3. Giao diện người dùng Java Swing hiện đại
- Thiết kế theo phong cách hiện đại với thư viện **FlatLaf** (hỗ trợ chuyển đổi Light/Dark theme).
- Tích hợp **Nhật ký giao thức (Protocol Log)** màu sắc trực quan:
  - Màu xanh dương: Lệnh gửi đi từ Client (`-> C: ...`)
  - Màu xanh lá: Phản hồi từ Server (`<- S: ...`)
  - Màu xám/đỏ: Thông báo trạng thái và lỗi.
- Xử lý mạng bất đồng bộ qua `SwingWorker`, không làm treo đơ giao diện khi truyền tải.

---

## 📁 Cấu trúc thư mục dự án

```
Mail-Client/
├── pom.xml                               # Cấu hình Maven & dependencies (FlatLaf, JUnit 5)
├── README.md                             # Tài liệu hướng dẫn
└── src/
    ├── main/
    │   └── java/
    │       └── com/
    │           └── mailclient/
    │               ├── Main.java                         # Điểm khởi chạy chương trình
    │               ├── model/
    │               │   ├── EmailAttachment.java          # Đại diện tệp đính kèm (Base64)
    │               │   ├── EmailMessage.java             # Cấu trúc dữ liệu Email
    │               │   ├── SecurityType.java             # Enum: NONE, SSL_TLS, STARTTLS
    │               │   └── ServerConfig.java             # Cấu hình kết nối máy chủ
    │               ├── protocol/
    │               │   ├── common/
    │               │   │   ├── EmailParser.java          # Bộ phân tích cú pháp RFC 822/MIME
    │               │   │   └── ProtocolLogger.java       # Interface ghi log mạng
    │               │   ├── smtp/
    │               │   │   ├── SmtpClient.java           # Lập trình Socket SMTP RFC 5321
    │               │   │   └── SmtpException.java        # Ngoại lệ giao thức SMTP
    │               │   └── pop3/
    │               │       ├── Pop3Client.java           # Lập trình Socket POP3 RFC 1939
    │               │       ├── Pop3Exception.java        # Ngoại lệ giao thức POP3
    │               │       ├── Pop3MessageInfo.java      # Model kết quả LIST
    │               │       └── Pop3Stat.java             # Model kết quả STAT
    │               └── ui/
    │                   ├── HelpPanel.java                # Tab lý thuyết & hướng dẫn kiểm thử
    │                   ├── LogPanel.java                 # Bảng nhật ký giao tiếp Socket
    │                   ├── MainFrame.java                # Cửa sổ chính chứa các Tab
    │                   ├── Pop3Panel.java                # Giao diện nhận & quản lý thư POP3
    │                   └── SmtpPanel.java                # Giao diện soạn & gửi thư SMTP
    └── test/
        └── java/
            └── com/
                └── mailclient/
                    └── ProtocolIntegrationTest.java      # Kiểm thử Mock Server tự động
```

---

## 🛠️ Hướng dẫn Chạy ứng dụng

### 1. Chạy bằng Maven
Tại thư mục gốc dự án:
```powershell
mvn compile exec:java
```

### 2. Chạy tệp JAR sau khi đóng gói
```powershell
mvn clean package -DskipTests
java -jar target/Mail-Client-1.0-SNAPSHOT.jar
```

### 3. Chạy trong IntelliJ IDEA
- Mở thư mục `Mail-Client` trong IntelliJ IDEA.
- Tìm tới `src/main/java/com/mailclient/Main.java`.
- Chuột phải và chọn **Run 'Main.main()'**.

---

## 🧪 Hướng dẫn Kiểm thử

### Cách 1: Sử dụng tài khoản Gmail thật (Yêu cầu Mật khẩu ứng dụng)
1. Bật **Xác minh 2 bước** trên tài khoản Google.
2. Truy cập [Google App Passwords](https://myaccount.google.com/apppasswords) và tạo mật khẩu ứng dụng 16 ký tự.
3. Kích hoạt tính năng POP trong Gmail: *Cài đặt > Xem tất cả tùy chọn cài đặt > Chuyển tiếp và POP/IMAP > Bật POP cho tất cả thư*.
4. Trong ứng dụng Mail Client:
   - **Gửi thư (SMTP)**: Chọn mẫu `Gmail (smtp.gmail.com:465 - SSL/TLS)` hoặc `smtp.gmail.com:587 STARTTLS`, nhập email và mật khẩu ứng dụng 16 ký tự.
   - **Nhận thư (POP3)**: Chọn mẫu `Gmail (pop.gmail.com:995 - SSL/TLS)`, nhập email và mật khẩu ứng dụng, sau đó nhấn **Kết nối & Tải thư**.

### Cách 2: Sử dụng Máy chủ thư giả lập cục bộ (Localhost - Không cần Internet)
- Tải công cụ kiểm thử như **Papercut SMTP** (Windows) hoặc **MailHog**:
  - SMTP lắng nghe cổng: `localhost:25` hoặc `localhost:1025`
  - POP3 lắng nghe cổng: `localhost:110`
- Trong ứng dụng, chọn mẫu `Localhost (localhost:25)` và `Localhost (localhost:110)`.
