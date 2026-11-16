package com.multiserver.server.service;

import com.multiserver.common.model.Response;
import com.multiserver.common.model.User;
import com.multiserver.common.rmi.IBankService;
import com.multiserver.server.config.ServerConfig;
import com.multiserver.server.dao.UserDAO;

import java.math.BigDecimal;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.sql.Timestamp;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.*;

/**
 * Quản lý đồng bộ thời gian thực giữa 2 Server:
 * - Tự động phát hiện trạng thái kết nối tới Peer Server qua Heartbeat.
 * - Gửi lệnh đồng bộ (Register, Deposit, Withdraw, Transfer, DB Edit).
 * - Tự động đồng bộ bù (Catch-up sync) khi Peer Server trực tuyến trở lại.
 */
public class PeerSyncManager {

    public interface PeerStatusListener {
        void onPeerStatusChanged(boolean connected, String message);
    }

    private final ServerConfig config;
    private final UserDAO userDAO;
    private PeerStatusListener statusListener;

    private volatile boolean connected = false;
    private IBankService peerService = null;

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final ExecutorService syncExecutor = Executors.newCachedThreadPool();

    // Hàng đợi lưu các lệnh chưa gửi được khi Peer Server bị offline
    private final Queue<Runnable> pendingSyncQueue = new ConcurrentLinkedQueue<>();

    public PeerSyncManager(ServerConfig config, UserDAO userDAO) {
        this.config = config;
        this.userDAO = userDAO;
    }

    public void setStatusListener(PeerStatusListener listener) {
        this.statusListener = listener;
    }

    public void start() {
        // Chạy heartbeat kiểm tra Peer Server mỗi 3 giây
        scheduler.scheduleWithFixedDelay(this::checkPeerConnection, 1, 3, TimeUnit.SECONDS);
    }

    public void stop() {
        scheduler.shutdownNow();
        syncExecutor.shutdown();
    }

    public boolean isConnected() {
        return connected;
    }

    private synchronized void checkPeerConnection() {
        try {
            Registry registry = LocateRegistry.getRegistry(config.getPeerHost(), config.getPeerPort());
            IBankService service = (IBankService) registry.lookup("BankService");
            if (service != null && service.ping()) {
                peerService = service;
                boolean wasDisconnected = !connected;
                connected = true;

                if (wasDisconnected) {
                    notifyStatus(true, "Đã kết nối với " + config.getPeerName() + " (" + config.getPeerHost() + ":" + config.getPeerPort() + ")");
                    // Xử lý hàng đợi bù và đồng bộ toàn bộ khi Peer online trở lại
                    syncExecutor.submit(this::onPeerReconnected);
                }
                return;
            }
        } catch (Exception e) {
            // In chi tiết lỗi để dễ chẩn đoán mạng giữa 2 server
            // (ví dụ: Connection timed out do tường lửa, hoặc sai IP RMI)
            // System.err.println("[" + config.getServerName() + "] Chưa kết nối được tới " + config.getPeerName() + " (" + config.getPeerHost() + ":" + config.getPeerPort() + "): " + e.getMessage());
        }

        peerService = null;
        if (connected) {
            connected = false;
            notifyStatus(false, "Mất kết nối với " + config.getPeerName() + " (" + config.getPeerHost() + ":" + config.getPeerPort() + ")");
        }
    }

    private void notifyStatus(boolean isConn, String msg) {
        if (statusListener != null) {
            statusListener.onPeerStatusChanged(isConn, msg);
        }
    }

    private void onPeerReconnected() {
        System.out.println("[" + config.getServerName() + "] " + config.getPeerName() + " đã trực tuyến. Đang đồng bộ hàng đợi bù...");
        // 1. Chạy các tác vụ tồn đọng
        while (!pendingSyncQueue.isEmpty()) {
            Runnable task = pendingSyncQueue.poll();
            if (task != null) {
                try {
                    task.run();
                } catch (Exception e) {
                    System.err.println("Lỗi khi xử lý hàng đợi đồng bộ: " + e.getMessage());
                }
            }
        }

        // 2. Thực hiện đồng bộ 2 chiều các bản ghi mới nhất
        doFullBiDirectionalSync();
    }

    /**
     * Đồng bộ đăng ký tài khoản sang Peer Server
     */
    public void syncRegister(User user) {
        dispatchSync(() -> {
            try {
                if (peerService != null) {
                    peerService.syncRegister(user);
                    System.out.println("[" + config.getServerName() + "] Đã đồng bộ đăng ký tài khoản " + user.getEmail() + " sang " + config.getPeerName());
                }
            } catch (Exception e) {
                System.err.println("[" + config.getServerName() + "] Lỗi đồng bộ đăng ký tới peer: " + e.getMessage());
                pendingSyncQueue.add(() -> syncRegister(user));
            }
        });
    }

    /**
     * Đồng bộ số dư (sau Nạp tiền hoặc Rút tiền) sang Peer Server
     */
    public void syncBalance(String email, BigDecimal newBalance, Timestamp timestamp, String sourceAction) {
        dispatchSync(() -> {
            try {
                if (peerService != null) {
                    peerService.syncBalance(email, newBalance, timestamp, sourceAction);
                    System.out.println("[" + config.getServerName() + "] Đã đồng bộ số dư (" + sourceAction + ") tài khoản " + email + " sang " + config.getPeerName());
                }
            } catch (Exception e) {
                System.err.println("[" + config.getServerName() + "] Lỗi đồng bộ số dư tới peer: " + e.getMessage());
                pendingSyncQueue.add(() -> syncBalance(email, newBalance, timestamp, sourceAction));
            }
        });
    }

    /**
     * Đồng bộ giao dịch Chuyển khoản sang Peer Server
     */
    public void syncTransfer(String fromEmail, BigDecimal fromBalance, String toEmail, BigDecimal toBalance, Timestamp timestamp) {
        dispatchSync(() -> {
            try {
                if (peerService != null) {
                    peerService.syncTransfer(fromEmail, fromBalance, toEmail, toBalance, timestamp);
                    System.out.println("[" + config.getServerName() + "] Đã đồng bộ chuyển khoản (" + fromEmail + " -> " + toEmail + ") sang " + config.getPeerName());
                }
            } catch (Exception e) {
                System.err.println("[" + config.getServerName() + "] Lỗi đồng bộ chuyển khoản tới peer: " + e.getMessage());
                pendingSyncQueue.add(() -> syncTransfer(fromEmail, fromBalance, toEmail, toBalance, timestamp));
            }
        });
    }

    /**
     * Đồng bộ cập nhật thông tin người dùng từ GUI Server
     */
    public void syncUpdateUser(User user) {
        dispatchSync(() -> {
            try {
                if (peerService != null) {
                    peerService.syncUpdateUser(user);
                }
            } catch (Exception e) {
                pendingSyncQueue.add(() -> syncUpdateUser(user));
            }
        });
    }

    /**
     * Đồng bộ xóa người dùng từ GUI Server
     */
    public void syncDeleteUser(String email) {
        dispatchSync(() -> {
            try {
                if (peerService != null) {
                    peerService.syncDeleteUser(email);
                }
            } catch (Exception e) {
                pendingSyncQueue.add(() -> syncDeleteUser(email));
            }
        });
    }

    /**
     * Nút bấm trên GUI: Đồng bộ toàn bộ dữ liệu thủ công
     */
    public boolean triggerManualFullSync() {
        if (!connected || peerService == null) {
            checkPeerConnection();
        }
        if (peerService == null) {
            return false;
        }
        syncExecutor.submit(this::doFullBiDirectionalSync);
        return true;
    }

    private void doFullBiDirectionalSync() {
        try {
            if (peerService == null) return;
            // 1. Gửi toàn bộ người dùng local sang peer
            List<User> myUsers = userDAO.findAll();
            for (User u : myUsers) {
                try {
                    peerService.syncRegister(u);
                } catch (Exception ignored) {}
            }

            // 2. Nhận toàn bộ người dùng từ peer về lưu vào local nếu có tài khoản mới/cập nhật hơn
            Response<List<User>> peerUsersResp = peerService.syncGetAllUsers();
            if (peerUsersResp != null && peerUsersResp.isSuccess() && peerUsersResp.getData() != null) {
                for (User peerUser : peerUsersResp.getData()) {
                    userDAO.saveOrUpdateUser(peerUser);
                }
            }
            System.out.println("[" + config.getServerName() + "] Hoàn tất đồng bộ 2 chiều với " + config.getPeerName());
        } catch (Exception e) {
            System.err.println("[" + config.getServerName() + "] Lỗi trong quá trình đồng bộ toàn bộ: " + e.getMessage());
        }
    }

    private void dispatchSync(Runnable action) {
        if (connected && peerService != null) {
            syncExecutor.submit(action);
        } else {
            pendingSyncQueue.add(action);
        }
    }
}

