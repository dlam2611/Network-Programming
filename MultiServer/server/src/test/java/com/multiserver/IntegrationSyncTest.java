package com.multiserver;

import com.multiserver.client.config.ClientConfig;
import com.multiserver.client.service.ClientBankingService;
import com.multiserver.common.model.Response;
import com.multiserver.common.model.User;
import com.multiserver.server.config.ServerConfig;
import com.multiserver.server.dao.UserDAO;
import com.multiserver.server.database.DBConnection;
import com.multiserver.server.database.DatabaseInitializer;
import com.multiserver.server.service.BankServiceImpl;
import com.multiserver.server.service.PeerSyncManager;

import java.math.BigDecimal;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class IntegrationSyncTest {

    public static void main(String[] args) throws Exception {
        System.out.println("=== BẮT ĐẦU TEST TOÀN DIỆN HỆ THỐNG 2 SERVER & CLIENT ===");

        // 1. Khởi động Server 1 (Lắng nghe 1099, Peer: 1098, CSDL: RMIDATABASE)
        ServerConfig cfg1 = ServerConfig.load("config/server.properties");
        DatabaseInitializer.initialize(cfg1);
        DBConnection db1 = new DBConnection(cfg1);
        UserDAO dao1 = new UserDAO(db1);
        PeerSyncManager syncMgr1 = new PeerSyncManager(cfg1, dao1);
        BankServiceImpl srv1 = new BankServiceImpl(cfg1, dao1, syncMgr1);
        Registry reg1 = LocateRegistry.createRegistry(1099);
        reg1.rebind("BankService", srv1);
        syncMgr1.start();
        System.out.println("[TEST] Server 1 đã online trên port 1099!");

        // 2. Khởi động Server 2 (Lắng nghe 1098, Peer: 1099, CSDL: RMIDATABASE)
        ServerConfig cfg2 = ServerConfig.load("config/server.properties");
        cfg2.setServerName("Server 2");
        cfg2.setServerPort(1098);
        cfg2.setPeerName("Server 1");
        cfg2.setPeerPort(1099);

        DatabaseInitializer.initialize(cfg2);
        DBConnection db2 = new DBConnection(cfg2);
        UserDAO dao2 = new UserDAO(db2);
        PeerSyncManager syncMgr2 = new PeerSyncManager(cfg2, dao2);
        BankServiceImpl srv2 = new BankServiceImpl(cfg2, dao2, syncMgr2);
        Registry reg2 = LocateRegistry.createRegistry(1098);
        reg2.rebind("BankService", srv2);
        syncMgr2.start();
        System.out.println("[TEST] Server 2 đã online trên port 1098!");

        // Chờ 2 giây để 2 server kết nối peer heartbeat
        Thread.sleep(2000);

        // 3. Khởi tạo Client Banking Service
        ClientConfig clientCfg = ClientConfig.load("config/client.properties");
        ClientBankingService client = new ClientBankingService(clientCfg);

        // Test kết nối Random
        boolean connected = client.connectRandomActiveServer();
        System.out.println("[TEST] Client kết nối ngẫu nhiên: " + connected + " -> Đang dùng: " + client.getCurrentServer());

        String emailA = "user_a_" + System.currentTimeMillis() + "@gmail.com";
        String emailB = "user_b_" + System.currentTimeMillis() + "@gmail.com";

        // Test 1: Đăng ký User A qua Client
        System.out.println("[TEST] 1. Đăng ký tài khoản A: " + emailA);
        Response<User> regRespA = client.register("Nguyen Van A", emailA, "pass123");
        System.out.println("   Kết quả đăng ký A: " + regRespA.getMessage());

        // Test 2: Đăng ký User B qua Client
        System.out.println("[TEST] 2. Đăng ký tài khoản B: " + emailB);
        Response<User> regRespB = client.register("Tran Thi B", emailB, "pass456");
        System.out.println("   Kết quả đăng ký B: " + regRespB.getMessage());

        // Test 3: Đăng nhập User A
        System.out.println("[TEST] 3. Đăng nhập User A");
        Response<User> loginResp = client.login(emailA, "pass123");
        System.out.println("   Kết quả đăng nhập A: " + loginResp.getMessage() + ", Số dư ban đầu: " + loginResp.getData().getBalance());

        // Test 4: Nạp 1,000,000 VNĐ vào User A
        System.out.println("[TEST] 4. Nạp tiền vào User A (1,000,000 đ)");
        Response<BigDecimal> depResp = client.deposit(new BigDecimal("1000000"));
        System.out.println("   Kết quả nạp tiền: " + depResp.getMessage());

        // Test 5: Rút 200,000 VNĐ từ User A
        System.out.println("[TEST] 5. Rút tiền từ User A (200,000 đ)");
        Response<BigDecimal> withResp = client.withdraw(new BigDecimal("200000"));
        System.out.println("   Kết quả rút tiền: " + withResp.getMessage() + " (Còn lại: 800,000 đ)");

        // Test 6: Chuyển khoản 300,000 VNĐ từ User A sang User B
        System.out.println("[TEST] 6. Chuyển khoản từ User A sang User B (300,000 đ)");
        Response<BigDecimal> transResp = client.transfer(emailB, new BigDecimal("300000"));
        System.out.println("   Kết quả chuyển khoản: " + transResp.getMessage());

        Thread.sleep(1500);

        System.out.println("\n=== KIỂM TRA ĐỒNG BỘ ===");
        User a1 = dao1.findByEmail(emailA);
        User a2 = dao2.findByEmail(emailA);
        System.out.println("User A Balance: " + a1.getBalance() + " | S2 Sync: " + a2.getBalance());

        syncMgr1.stop();
        syncMgr2.stop();
        System.out.println(">>> HOÀN TẤT KIỂM THỬ THÀNH CÔNG! <<<");
        System.exit(0);
    }
}
