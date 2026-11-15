# ỨNG DỤNG NGÂN HÀNG PHÂN TÁN ĐA MÁY CHỦ (MULTI-SERVER BANKING SYSTEM)

Hệ thống ứng dụng Desktop Java thuần (Java Swing, FlatLaf Light Theme), chạy trên kiến trúc **2 Server phân tán** đồng bộ thời gian thực và kết nối ngẫu nhiên từ phía Client. Sử dụng cơ sở dữ liệu **PostgreSQL** (`RMIDATABASE`).

---

## 1. Cấu Trúc Bảng CSDL PostgreSQL

```sql
CREATE TABLE IF NOT EXISTS users (
    user_id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL, -- Mật khẩu hỗ trợ Đăng nhập / Đăng ký (băm SHA-256)
    balance NUMERIC(15, 2) DEFAULT 0.00 NOT NULL,
    last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);
```

> **Cơ chế tự động:** Hệ thống có `DatabaseInitializer` tự động kiểm tra CSDL và tạo bảng `users` ngay khi Server khởi động nếu bảng chưa tồn tại.

---

## 2. Kiến Trúc Cấu Hình (Độc Lập & Tinh Gọn)

Mỗi Server chỉ quản lý cơ sở dữ liệu của chính mình, **chỉ lưu địa chỉ IP và Port của Server khác** để gọi đồng bộ RMI.

### File cấu hình Server: `config/server.properties`
```properties
# ========================================================
# CẤU HÌNH SERVER
# ========================================================

# 1. Thông tin của chính Server này
server.id=1
server.name=Server 1
server.host=127.0.0.1
server.port=1099

# 2. Địa chỉ IP và Port của Server đối ứng (để đồng bộ)
# Server chỉ cần biết IP và Port của Server khác, KHÔNG lưu CSDL của họ
peer.name=Server 2
peer.host=127.0.0.1
peer.port=1098

# 3. Cơ sở dữ liệu PostgreSQL của CHÍNH SERVER NÀY
db.url=jdbc:postgresql://localhost:5432/RMIDATABASE
db.user=postgres
db.password=Lam2006@123
```

### File cấu hình Client: `config/client.properties`
```properties
# Danh sách các Server khả dụng để Client quét và kết nối ngẫu nhiên
servers=127.0.0.1:1099,127.0.0.1:1098
connect.timeout=3000
```

---

## 3. Các Tính Năng Nổi Bật

1. **5 Chức năng nghiệp vụ Client**:
   * **Đăng ký**: Mã hóa SHA-256, tự động đồng bộ sang Server đối ứng.
   * **Đăng nhập**: Xác thực cục bộ, **không đồng bộ sang server khác** (đúng yêu cầu).
   * **Nạp tiền**: Cập nhật số dư và đồng bộ sang Server đối ứng.
   * **Rút tiền**: Kiểm tra số dư và đồng bộ sang Server đối ứng.
   * **Chuyển khoản**: Giao dịch Atomic (Commit/Rollback) và đồng bộ số dư 2 tài khoản sang Server đối ứng.
2. **Client kết nối Random trong các Server đang hoạt động**:
   * Quét các server online (`ping()`).
   * Chọn ngẫu nhiên 1 server online để kết nối. Nút "Đổi Server (Random lại)" trên giao diện.
3. **Đồng bộ giữa các Server**:
   * Giao tiếp qua mạng RMI dựa trên `peer.host` và `peer.port`.
   * Có hàng đợi lưu trữ và cơ chế Heartbeat bù lệnh khi đối tác online trở lại.
4. **Giao diện Server (Theme sáng)**:
   * **Nhật ký truy cập Client**: Xem Client IP, thời gian, thao tác, trạng thái, chi tiết giao dịch theo thời gian thực.
   * **Quản lý dữ liệu CSDL**: Xem, thêm mới, sửa số dư, xóa tài khoản trực tiếp trên Server.

---

## 4. Hướng Dẫn Khởi Chạy

* Chạy **`run-server1.bat`**: Khởi chạy Server 1 (Lắng nghe: `1099`, Peer: `1098`).
* Chạy **`run-server2.bat`**: Khởi chạy Server 2 (Lắng nghe: `1098`, Peer: `1099`).
* Chạy **`run-client.bat`**: Khởi chạy Client ngẫu nhiên kết nối tới 1 trong 2 Server.
