package com.multiserver.client.service;

import com.multiserver.client.config.ClientConfig;
import com.multiserver.common.model.Response;
import com.multiserver.common.model.ServerInfo;
import com.multiserver.common.model.User;
import com.multiserver.common.rmi.IBankService;
import com.multiserver.common.util.FormatUtil;

import java.math.BigDecimal;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Service phía Client xử lý:
 * 1. Quét danh sách Server đang hoạt động (active servers).
 * 2. Chọn ngẫu nhiên (random) 1 trong các server đang hoạt động để kết nối.
 * 3. Tự động chuyển đổi dự phòng (failover) nếu server đang dùng bị ngắt kết nối.
 */
public class ClientBankingService {

    private final ClientConfig config;
    private final String clientIp;

    private ServerInfo currentServer;
    private IBankService rmiService;
    private User currentUser;
    private List<ServerInfo> activeServers = new ArrayList<>();

    public ClientBankingService(ClientConfig config) {
        this.config = config;
        this.clientIp = FormatUtil.getLocalIpAddress();
    }

    /**
     * Quét và tìm các Server đang online, sau đó chọn RANDOM 1 Server để kết nối
     */
    public synchronized boolean connectRandomActiveServer() {
        activeServers = findActiveServers();
        if (activeServers.isEmpty()) {
            currentServer = null;
            rmiService = null;
            return false;
        }

        // Chọn ngẫu nhiên trong danh sách server đang hoạt động
        Collections.shuffle(activeServers);
        ServerInfo selected = activeServers.get(0);

        try {
            Registry registry = LocateRegistry.getRegistry(selected.getHost(), selected.getPort());
            IBankService service = (IBankService) registry.lookup("BankService");
            if (service != null && service.ping()) {
                this.currentServer = selected;
                this.rmiService = service;
                System.out.println("Client đã kết nối NGẪU NHIÊN tới: " + selected + " (trong số " + activeServers.size() + " server đang online)");
                return true;
            }
        } catch (Exception e) {
            System.err.println("Lỗi kết nối tới " + selected + ": " + e.getMessage());
        }

        return false;
    }

    /**
     * Tìm danh sách các Server đang chạy (active)
     */
    public List<ServerInfo> findActiveServers() {
        List<ServerInfo> active = new ArrayList<>();
        for (ServerInfo s : config.getServers()) {
            try {
                Registry registry = LocateRegistry.getRegistry(s.getHost(), s.getPort());
                IBankService service = (IBankService) registry.lookup("BankService");
                if (service != null && service.ping()) {
                    active.add(s);
                }
            } catch (Exception ignored) {
                // Server này đang offline
            }
        }
        return active;
    }

    public ServerInfo getCurrentServer() {
        return currentServer;
    }

    public List<ServerInfo> getActiveServers() {
        return activeServers;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
    }

    private void ensureConnected() throws RemoteException {
        if (rmiService == null) {
            boolean ok = connectRandomActiveServer();
            if (!ok || rmiService == null) {
                throw new RemoteException("Không thể kết nối tới hệ thống ngân hàng, vui lòng thử lại sau!");
            }
        }
    }

    // ==================== 5 CHỨC NĂNG ====================

    /**
     * 1. Đăng ký
     */
    public Response<User> register(String name, String email, String password) throws Exception {
        ensureConnected();
        try {
            return rmiService.register(name, email, password, clientIp);
        } catch (RemoteException e) {
            // Thử failover sang server khác nếu server hiện tại sập
            if (connectRandomActiveServer()) {
                return rmiService.register(name, email, password, clientIp);
            }
            throw e;
        }
    }

    /**
     * 2. Đăng nhập
     */
    public Response<User> login(String email, String password) throws Exception {
        ensureConnected();
        try {
            Response<User> resp = rmiService.login(email, password, clientIp);
            if (resp.isSuccess() && resp.getData() != null) {
                this.currentUser = resp.getData();
            }
            return resp;
        } catch (RemoteException e) {
            if (connectRandomActiveServer()) {
                Response<User> resp = rmiService.login(email, password, clientIp);
                if (resp.isSuccess() && resp.getData() != null) {
                    this.currentUser = resp.getData();
                }
                return resp;
            }
            throw e;
        }
    }

    /**
     * 3. Nạp tiền
     */
    public Response<BigDecimal> deposit(BigDecimal amount) throws Exception {
        ensureConnected();
        if (currentUser == null) {
            return Response.error("Bạn chưa đăng nhập!");
        }
        try {
            Response<BigDecimal> resp = rmiService.deposit(currentUser.getEmail(), amount, clientIp);
            if (resp.isSuccess() && resp.getData() != null) {
                currentUser.setBalance(resp.getData());
            }
            return resp;
        } catch (RemoteException e) {
            if (connectRandomActiveServer()) {
                Response<BigDecimal> resp = rmiService.deposit(currentUser.getEmail(), amount, clientIp);
                if (resp.isSuccess() && resp.getData() != null) {
                    currentUser.setBalance(resp.getData());
                }
                return resp;
            }
            throw e;
        }
    }

    /**
     * 4. Rút tiền
     */
    public Response<BigDecimal> withdraw(BigDecimal amount) throws Exception {
        ensureConnected();
        if (currentUser == null) {
            return Response.error("Bạn chưa đăng nhập!");
        }
        try {
            Response<BigDecimal> resp = rmiService.withdraw(currentUser.getEmail(), amount, clientIp);
            if (resp.isSuccess() && resp.getData() != null) {
                currentUser.setBalance(resp.getData());
            }
            return resp;
        } catch (RemoteException e) {
            if (connectRandomActiveServer()) {
                Response<BigDecimal> resp = rmiService.withdraw(currentUser.getEmail(), amount, clientIp);
                if (resp.isSuccess() && resp.getData() != null) {
                    currentUser.setBalance(resp.getData());
                }
                return resp;
            }
            throw e;
        }
    }

    /**
     * 5. Chuyển khoản
     */
    public Response<BigDecimal> transfer(String toEmail, BigDecimal amount) throws Exception {
        ensureConnected();
        if (currentUser == null) {
            return Response.error("Bạn chưa đăng nhập!");
        }
        try {
            Response<BigDecimal> resp = rmiService.transfer(currentUser.getEmail(), toEmail, amount, clientIp);
            if (resp.isSuccess() && resp.getData() != null) {
                currentUser.setBalance(resp.getData());
            }
            return resp;
        } catch (RemoteException e) {
            if (connectRandomActiveServer()) {
                Response<BigDecimal> resp = rmiService.transfer(currentUser.getEmail(), toEmail, amount, clientIp);
                if (resp.isSuccess() && resp.getData() != null) {
                    currentUser.setBalance(resp.getData());
                }
                return resp;
            }
            throw e;
        }
    }

    /**
     * Làm mới thông tin số dư từ server
     */
    public Response<User> refreshUserInfo() throws Exception {
        ensureConnected();
        if (currentUser == null) {
            return Response.error("Chưa đăng nhập!");
        }
        Response<User> resp = rmiService.getUserInfo(currentUser.getEmail(), clientIp);
        if (resp.isSuccess() && resp.getData() != null) {
            this.currentUser = resp.getData();
        }
        return resp;
    }

    public void logout() {
        this.currentUser = null;
    }
}

