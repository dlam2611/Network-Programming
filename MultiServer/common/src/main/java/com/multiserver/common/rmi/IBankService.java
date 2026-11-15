package com.multiserver.common.rmi;

import com.multiserver.common.model.Response;
import com.multiserver.common.model.User;

import java.math.BigDecimal;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.sql.Timestamp;
import java.util.List;

/**
 * Remote Interface cho dịch vụ Ngân Hàng phân tán (Java RMI).
 * Chứa 5 chức năng chính cho Client và các phương thức đồng bộ giữa 2 Server.
 */
public interface IBankService extends Remote {

    // ==================== 5 CHỨC NĂNG DÀNH CHO CLIENT ====================

    /**
     * Chức năng 1: Đăng ký tài khoản
     */
    Response<User> register(String name, String email, String password, String clientIp) throws RemoteException;

    /**
     * Chức năng 2: Đăng nhập (KHÔNG đồng bộ sang server khác theo yêu cầu)
     */
    Response<User> login(String email, String password, String clientIp) throws RemoteException;

    /**
     * Chức năng 3: Nạp tiền vào tài khoản
     */
    Response<BigDecimal> deposit(String email, BigDecimal amount, String clientIp) throws RemoteException;

    /**
     * Chức năng 4: Rút tiền từ tài khoản
     */
    Response<BigDecimal> withdraw(String email, BigDecimal amount, String clientIp) throws RemoteException;

    /**
     * Chức năng 5: Chuyển khoản giữa 2 tài khoản
     */
    Response<BigDecimal> transfer(String fromEmail, String toEmail, BigDecimal amount, String clientIp) throws RemoteException;

    /**
     * Lấy thông tin tài khoản mới nhất
     */
    Response<User> getUserInfo(String email, String clientIp) throws RemoteException;

    /**
     * Kiểm tra trạng thái hoạt động của Server (Heartbeat / Health check)
     */
    boolean ping() throws RemoteException;


    // ==================== ĐỒNG BỘ GIỮA CÁC SERVER ====================

    /**
     * Đồng bộ khi có người dùng đăng ký mới trên server bạn
     */
    Response<Void> syncRegister(User user) throws RemoteException;

    /**
     * Đồng bộ số dư (sau khi nạp hoặc rút tiền)
     */
    Response<Void> syncBalance(String email, BigDecimal newBalance, Timestamp timestamp, String sourceAction) throws RemoteException;

    /**
     * Đồng bộ giao dịch chuyển khoản
     */
    Response<Void> syncTransfer(String fromEmail, BigDecimal fromBalance, String toEmail, BigDecimal toBalance, Timestamp timestamp) throws RemoteException;

    /**
     * Đồng bộ cập nhật thông tin người dùng từ giao diện quản lý
     */
    Response<Void> syncUpdateUser(User user) throws RemoteException;

    /**
     * Đồng bộ xóa tài khoản từ giao diện quản lý
     */
    Response<Void> syncDeleteUser(String email) throws RemoteException;

    /**
     * Đồng bộ toàn bộ dữ liệu (khi server khởi động lại hoặc reconnect)
     */
    Response<List<User>> syncGetAllUsers() throws RemoteException;
}

