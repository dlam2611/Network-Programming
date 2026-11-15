package com.multiserver.server.service;

import com.multiserver.common.model.AccessLog;
import com.multiserver.common.model.OperationType;
import com.multiserver.common.model.Response;
import com.multiserver.common.model.User;
import com.multiserver.common.rmi.IBankService;
import com.multiserver.common.util.FormatUtil;
import com.multiserver.common.util.PasswordUtil;
import com.multiserver.server.config.ServerConfig;
import com.multiserver.server.dao.UserDAO;

import java.math.BigDecimal;
import java.rmi.RemoteException;
import java.rmi.server.RemoteServer;
import java.rmi.server.ServerNotActiveException;
import java.rmi.server.UnicastRemoteObject;
import java.sql.Timestamp;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Cài đặt Remote Interface IBankService.
 * Xử lý toàn bộ logic ngân hàng, bảo đảm tính toàn vẹn giao dịch và đồng bộ 2 server.
 */
public class BankServiceImpl extends UnicastRemoteObject implements IBankService {

    public interface ServerEventListener {
        void onAccessLogged(AccessLog log);
        void onDataChanged();
    }

    private final ServerConfig config;
    private final UserDAO userDAO;
    private final PeerSyncManager peerSyncManager;
    private final AtomicLong logCounter = new AtomicLong(0);
    private final List<ServerEventListener> listeners = new CopyOnWriteArrayList<>();

    public BankServiceImpl(ServerConfig config, UserDAO userDAO, PeerSyncManager peerSyncManager) throws RemoteException {
        super(config.getServerPort());
        this.config = config;
        this.userDAO = userDAO;
        this.peerSyncManager = peerSyncManager;
    }

    public void addEventListener(ServerEventListener listener) {
        this.listeners.add(listener);
    }

    private String resolveClientIp(String fallbackIp) {
        try {
            return RemoteServer.getClientHost();
        } catch (ServerNotActiveException e) {
            return (fallbackIp != null && !fallbackIp.isBlank()) ? fallbackIp : "127.0.0.1";
        }
    }

    private void recordLog(String clientIp, OperationType op, boolean success, String details) {
        AccessLog log = new AccessLog(logCounter.incrementAndGet(), clientIp, op.getDisplayName(), success, details);
        for (ServerEventListener listener : listeners) {
            listener.onAccessLogged(log);
        }
    }

    private void notifyDataChanged() {
        for (ServerEventListener listener : listeners) {
            listener.onDataChanged();
        }
    }

    // ==================== 5 CHỨC NĂNG DÀNH CHO CLIENT ====================

    @Override
    public Response<User> register(String name, String email, String password, String clientIp) throws RemoteException {
        String ip = resolveClientIp(clientIp);

        if (name == null || name.isBlank() || email == null || email.isBlank() || password == null || password.isBlank()) {
            recordLog(ip, OperationType.REGISTER, false, "Thất bại: Thông tin không hợp lệ (" + email + ")");
            return Response.error("Vui lòng điền đầy đủ họ tên, email và mật khẩu!", config.getServerName());
        }

        if (userDAO.findByEmail(email) != null) {
            recordLog(ip, OperationType.REGISTER, false, "Thất bại: Email đã tồn tại (" + email + ")");
            return Response.error("Email đã tồn tại trong hệ thống!", config.getServerName());
        }

        String hashedPassword = PasswordUtil.hashPassword(password);
        User newUser = new User(name.trim(), email.trim().toLowerCase(), hashedPassword, BigDecimal.ZERO);

        boolean ok = userDAO.insertUser(newUser);
        if (ok) {
            recordLog(ip, OperationType.REGISTER, true, "Đăng ký thành công tài khoản: " + email);
            notifyDataChanged();

            // ĐỒNG BỘ: Chuyển dữ liệu đăng ký sang Peer Server
            peerSyncManager.syncRegister(newUser);

            return Response.ok("Đăng ký tài khoản thành công!", newUser, config.getServerName());
        } else {
            recordLog(ip, OperationType.REGISTER, false, "Thất bại khi lưu CSDL: " + email);
            return Response.error("Lỗi cơ sở dữ liệu khi đăng ký tài khoản!", config.getServerName());
        }
    }

    @Override
    public Response<User> login(String email, String password, String clientIp) throws RemoteException {
        String ip = resolveClientIp(clientIp);

        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            recordLog(ip, OperationType.LOGIN, false, "Thất bại: Thiếu thông tin đăng nhập");
            return Response.error("Vui lòng nhập email và mật khẩu!", config.getServerName());
        }

        User user = userDAO.findByEmail(email.trim().toLowerCase());
        if (user == null) {
            recordLog(ip, OperationType.LOGIN, false, "Thất bại: Email không tồn tại (" + email + ")");
            return Response.error("Email không tồn tại trong hệ thống!", config.getServerName());
        }

        if (!PasswordUtil.checkPassword(password, user.getPassword())) {
            recordLog(ip, OperationType.LOGIN, false, "Thất bại: Sai mật khẩu (" + email + ")");
            return Response.error("Sai mật khẩu, vui lòng kiểm tra lại!", config.getServerName());
        }

        // ĐĂNG NHẬP THÀNH CÔNG: KHÔNG ĐỒNG BỘ SANG SERVER KHÁC (Theo yêu cầu đề bài)
        recordLog(ip, OperationType.LOGIN, true, "Đăng nhập thành công: " + email);
        return Response.ok("Đăng nhập thành công!", user, config.getServerName());
    }

    @Override
    public Response<BigDecimal> deposit(String email, BigDecimal amount, String clientIp) throws RemoteException {
        String ip = resolveClientIp(clientIp);

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            recordLog(ip, OperationType.DEPOSIT, false, "Thất bại: Số tiền nạp không hợp lệ");
            return Response.error("Số tiền nạp phải lớn hơn 0!", config.getServerName());
        }

        User user = userDAO.findByEmail(email);
        if (user == null) {
            recordLog(ip, OperationType.DEPOSIT, false, "Thất bại: Tài khoản không tồn tại (" + email + ")");
            return Response.error("Không tìm thấy thông tin tài khoản!", config.getServerName());
        }

        BigDecimal newBalance = user.getBalance().add(amount);
        Timestamp now = new Timestamp(System.currentTimeMillis());
        boolean ok = userDAO.updateBalance(email, newBalance, now);

        if (ok) {
            recordLog(ip, OperationType.DEPOSIT, true, "Nạp +" + FormatUtil.formatCurrency(amount) + " vào " + email + " | Số dư mới: " + FormatUtil.formatCurrency(newBalance));
            notifyDataChanged();

            // ĐỒNG BỘ: Đồng bộ số dư sang Peer Server
            peerSyncManager.syncBalance(email, newBalance, now, "NẠP TIỀN");

            return Response.ok("Nạp tiền thành công! Số dư mới: " + FormatUtil.formatCurrency(newBalance), newBalance, config.getServerName());
        } else {
            recordLog(ip, OperationType.DEPOSIT, false, "Thất bại CSDL khi nạp tiền (" + email + ")");
            return Response.error("Lỗi cập nhật cơ sở dữ liệu!", config.getServerName());
        }
    }

    @Override
    public Response<BigDecimal> withdraw(String email, BigDecimal amount, String clientIp) throws RemoteException {
        String ip = resolveClientIp(clientIp);

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            recordLog(ip, OperationType.WITHDRAW, false, "Thất bại: Số tiền rút không hợp lệ");
            return Response.error("Số tiền rút phải lớn hơn 0!", config.getServerName());
        }

        User user = userDAO.findByEmail(email);
        if (user == null) {
            recordLog(ip, OperationType.WITHDRAW, false, "Thất bại: Tài khoản không tồn tại (" + email + ")");
            return Response.error("Không tìm thấy thông tin tài khoản!", config.getServerName());
        }

        if (user.getBalance().compareTo(amount) < 0) {
            recordLog(ip, OperationType.WITHDRAW, false, "Thất bại: Không đủ số dư (" + email + " cần " + FormatUtil.formatCurrency(amount) + ", hiện có " + FormatUtil.formatCurrency(user.getBalance()) + ")");
            return Response.error("Số dư hiện tại không đủ để thực hiện rút tiền!", config.getServerName());
        }

        BigDecimal newBalance = user.getBalance().subtract(amount);
        Timestamp now = new Timestamp(System.currentTimeMillis());
        boolean ok = userDAO.updateBalance(email, newBalance, now);

        if (ok) {
            recordLog(ip, OperationType.WITHDRAW, true, "Rút -" + FormatUtil.formatCurrency(amount) + " từ " + email + " | Số dư mới: " + FormatUtil.formatCurrency(newBalance));
            notifyDataChanged();

            // ĐỒNG BỘ: Đồng bộ số dư sang Peer Server
            peerSyncManager.syncBalance(email, newBalance, now, "RÚT TIỀN");

            return Response.ok("Rút tiền thành công! Số dư mới: " + FormatUtil.formatCurrency(newBalance), newBalance, config.getServerName());
        } else {
            recordLog(ip, OperationType.WITHDRAW, false, "Thất bại CSDL khi rút tiền (" + email + ")");
            return Response.error("Lỗi cập nhật cơ sở dữ liệu!", config.getServerName());
        }
    }

    @Override
    public Response<BigDecimal> transfer(String fromEmail, String toEmail, BigDecimal amount, String clientIp) throws RemoteException {
        String ip = resolveClientIp(clientIp);

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            recordLog(ip, OperationType.TRANSFER, false, "Thất bại: Số tiền chuyển không hợp lệ");
            return Response.error("Số tiền chuyển phải lớn hơn 0!", config.getServerName());
        }

        if (fromEmail.equalsIgnoreCase(toEmail)) {
            recordLog(ip, OperationType.TRANSFER, false, "Thất bại: Người nhận trùng người gửi (" + fromEmail + ")");
            return Response.error("Không thể tự chuyển tiền cho chính mình!", config.getServerName());
        }

        User sender = userDAO.findByEmail(fromEmail);
        if (sender == null) {
            recordLog(ip, OperationType.TRANSFER, false, "Thất bại: Người gửi không tồn tại (" + fromEmail + ")");
            return Response.error("Tài khoản người gửi không tồn tại!", config.getServerName());
        }

        if (sender.getBalance().compareTo(amount) < 0) {
            recordLog(ip, OperationType.TRANSFER, false, "Thất bại: Người gửi không đủ tiền (" + fromEmail + ")");
            return Response.error("Số dư không đủ để thực hiện chuyển khoản!", config.getServerName());
        }

        User receiver = userDAO.findByEmail(toEmail);
        if (receiver == null) {
            recordLog(ip, OperationType.TRANSFER, false, "Thất bại: Người nhận không tồn tại (" + toEmail + ")");
            return Response.error("Tài khoản người nhận không tồn tại trong hệ thống!", config.getServerName());
        }

        Timestamp now = new Timestamp(System.currentTimeMillis());
        boolean ok = userDAO.executeTransfer(fromEmail, toEmail, amount, now);

        if (ok) {
            BigDecimal newSenderBalance = sender.getBalance().subtract(amount);
            BigDecimal newReceiverBalance = receiver.getBalance().add(amount);

            recordLog(ip, OperationType.TRANSFER, true, "Chuyển " + FormatUtil.formatCurrency(amount) + " từ " + fromEmail + " -> " + toEmail);
            notifyDataChanged();

            // ĐỒNG BỘ: Đồng bộ giao dịch chuyển khoản sang Peer Server
            peerSyncManager.syncTransfer(fromEmail, newSenderBalance, toEmail, newReceiverBalance, now);

            return Response.ok("Chuyển khoản thành công! Số dư mới: " + FormatUtil.formatCurrency(newSenderBalance), newSenderBalance, config.getServerName());
        } else {
            recordLog(ip, OperationType.TRANSFER, false, "Lỗi giao dịch chuyển tiền: " + fromEmail + " -> " + toEmail);
            return Response.error("Giao dịch chuyển tiền thất bại, vui lòng thử lại!", config.getServerName());
        }
    }

    @Override
    public Response<User> getUserInfo(String email, String clientIp) throws RemoteException {
        User user = userDAO.findByEmail(email);
        if (user != null) {
            return Response.ok("Lấy thông tin thành công", user, config.getServerName());
        }
        return Response.error("Không tìm thấy tài khoản: " + email, config.getServerName());
    }

    @Override
    public boolean ping() throws RemoteException {
        return true;
    }

    // ==================== ENDPOINTS ĐỒNG BỘ TỪ PEER SERVER ====================

    @Override
    public Response<Void> syncRegister(User user) throws RemoteException {
        String peerIp = resolveClientIp(config.getPeerHost());
        boolean ok = userDAO.saveOrUpdateUser(user);
        if (ok) {
            recordLog(peerIp, OperationType.SYNC_REGISTER, true, "Đã đồng bộ tài khoản mới từ " + config.getPeerName() + ": " + user.getEmail());
            notifyDataChanged();
            return Response.ok("Đồng bộ đăng ký thành công", null, config.getServerName());
        }
        return Response.error("Thất bại khi lưu bản ghi đồng bộ", config.getServerName());
    }

    @Override
    public Response<Void> syncBalance(String email, BigDecimal newBalance, Timestamp timestamp, String sourceAction) throws RemoteException {
        String peerIp = resolveClientIp(config.getPeerHost());
        boolean ok = userDAO.updateBalance(email, newBalance, timestamp);
        if (ok) {
            recordLog(peerIp, OperationType.SYNC_BALANCE, true, "Đã đồng bộ số dư (" + sourceAction + ") từ " + config.getPeerName() + ": " + email + " = " + FormatUtil.formatCurrency(newBalance));
            notifyDataChanged();
            return Response.ok("Đồng bộ số dư thành công", null, config.getServerName());
        }
        return Response.error("Thất bại khi đồng bộ số dư", config.getServerName());
    }

    @Override
    public Response<Void> syncTransfer(String fromEmail, BigDecimal fromBalance, String toEmail, BigDecimal toBalance, Timestamp timestamp) throws RemoteException {
        String peerIp = resolveClientIp(config.getPeerHost());
        userDAO.updateBalance(fromEmail, fromBalance, timestamp);
        userDAO.updateBalance(toEmail, toBalance, timestamp);

        recordLog(peerIp, OperationType.SYNC_TRANSFER, true, "Đã đồng bộ chuyển khoản từ " + config.getPeerName() + ": " + fromEmail + " -> " + toEmail);
        notifyDataChanged();
        return Response.ok("Đồng bộ chuyển khoản thành công", null, config.getServerName());
    }

    @Override
    public Response<Void> syncUpdateUser(User user) throws RemoteException {
        String peerIp = resolveClientIp(config.getPeerHost());
        userDAO.updateUser(user);
        recordLog(peerIp, OperationType.SYNC_FULL, true, "Đã đồng bộ sửa đổi thông tin người dùng " + user.getEmail() + " từ " + config.getPeerName());
        notifyDataChanged();
        return Response.ok("OK", null, config.getServerName());
    }

    @Override
    public Response<Void> syncDeleteUser(String email) throws RemoteException {
        String peerIp = resolveClientIp(config.getPeerHost());
        userDAO.deleteUser(email);
        recordLog(peerIp, OperationType.SYNC_DELETE, true, "Đã đồng bộ xóa người dùng " + email + " từ " + config.getPeerName());
        notifyDataChanged();
        return Response.ok("OK", null, config.getServerName());
    }

    @Override
    public Response<List<User>> syncGetAllUsers() throws RemoteException {
        List<User> list = userDAO.findAll();
        return Response.ok("All users", list, config.getServerName());
    }
}

