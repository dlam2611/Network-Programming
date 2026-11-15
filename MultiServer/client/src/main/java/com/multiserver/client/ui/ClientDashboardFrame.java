package com.multiserver.client.ui;

import com.multiserver.client.service.ClientBankingService;
import com.multiserver.common.model.Response;
import com.multiserver.common.model.User;
import com.multiserver.common.util.FormatUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Giao diện chính của Khách Hàng (Theme Sáng):
 * 1. Nạp tiền
 * 2. Rút tiền
 * 3. Chuyển khoản
 * 4. Xem số dư & Lịch sử giao dịch trong phiên làm việc
 * Giao diện thuần người dùng ngân hàng, không hiển thị hạ tầng/server/IP.
 */
public class ClientDashboardFrame extends JFrame {

    private final ClientBankingService service;

    // Header & User Card components
    private JLabel lblUserName;
    private JLabel lblUserEmail;
    private JLabel lblUserBalance;
    private JLabel lblLastUpdated;

    // Form fields
    private JTextField txtDepositAmount;
    private JTextField txtWithdrawAmount;
    private JTextField txtTransferReceiver;
    private JTextField txtTransferAmount;
    private JTextField txtTransferNote;

    // Activity log table
    private DefaultTableModel activityTableModel;
    private JTable activityTable;

    public ClientDashboardFrame(ClientBankingService service) {
        this.service = service;
        initComponents();
        refreshUserData();
    }

    private void initComponents() {
        setTitle("HỆ THỐNG NGÂN HÀNG ĐIỆN TỬ - TÀI KHOẢN KHÁCH HÀNG");
        setSize(920, 650);
        setMinimumSize(new Dimension(820, 560));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBackground(new Color(248, 249, 250));
        mainPanel.setBorder(new EmptyBorder(12, 16, 12, 16));

        // 1. Top Bar
        mainPanel.add(createTopBar(), BorderLayout.NORTH);

        // 2. Center Panel: Split into (User Card + Action Tabs) and (Activity Table)
        JPanel centerPanel = new JPanel(new BorderLayout(10, 10));
        centerPanel.setBackground(new Color(248, 249, 250));

        JPanel upperCenter = new JPanel(new BorderLayout(10, 10));
        upperCenter.setBackground(new Color(248, 249, 250));
        upperCenter.add(createAccountCard(), BorderLayout.WEST);
        upperCenter.add(createOperationsTabs(), BorderLayout.CENTER);

        centerPanel.add(upperCenter, BorderLayout.NORTH);
        centerPanel.add(createActivityLogPanel(), BorderLayout.CENTER);

        mainPanel.add(centerPanel, BorderLayout.CENTER);
        setContentPane(mainPanel);
    }

    private JPanel createTopBar() {
        JPanel bar = new JPanel(new BorderLayout(10, 5));
        bar.setBackground(Color.WHITE);
        bar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(222, 226, 230), 1),
                new EmptyBorder(10, 15, 10, 15)
        ));

        // Left App Name
        JLabel lblApp = new JLabel("NGÂN HÀNG ĐIỆN TỬ");
        lblApp.setFont(new Font("Segoe UI", Font.BOLD, 17));
        lblApp.setForeground(new Color(13, 110, 253));
        bar.add(lblApp, BorderLayout.WEST);

        // Right Security badge & Logout button
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        right.setBackground(Color.WHITE);

        JLabel lblSec = new JLabel("● Đã kết nối an toàn (SSL)");
        lblSec.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblSec.setForeground(new Color(25, 135, 84));
        right.add(lblSec);

        JButton btnLogout = new JButton("Đăng xuất");
        btnLogout.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnLogout.setBackground(new Color(220, 53, 69));
        btnLogout.setForeground(Color.WHITE);
        btnLogout.setFocusable(false);
        btnLogout.addActionListener(e -> handleLogout());
        right.add(btnLogout);

        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    private JPanel createAccountCard() {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setPreferredSize(new Dimension(290, 240));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(222, 226, 230), 1),
                new EmptyBorder(16, 16, 16, 16)
        ));

        JLabel lblCardTitle = new JLabel("THÔNG TIN TÀI KHOẢN");
        lblCardTitle.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblCardTitle.setForeground(new Color(108, 117, 125));

        lblUserName = new JLabel("Chủ TK: ...");
        lblUserName.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblUserName.setForeground(new Color(33, 37, 41));

        lblUserEmail = new JLabel("Email: ...");
        lblUserEmail.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblUserEmail.setForeground(new Color(108, 117, 125));

        JLabel lblBalTitle = new JLabel("SỐ DƯ HIỆN TẠI:");
        lblBalTitle.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblBalTitle.setForeground(new Color(108, 117, 125));

        lblUserBalance = new JLabel("0.00 đ");
        lblUserBalance.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lblUserBalance.setForeground(new Color(25, 135, 84));

        lblLastUpdated = new JLabel("Cập nhật: ...");
        lblLastUpdated.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        lblLastUpdated.setForeground(new Color(173, 181, 189));

        JButton btnRefresh = new JButton("Làm mới số dư");
        btnRefresh.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnRefresh.setFocusable(false);
        btnRefresh.addActionListener(e -> refreshUserData());

        card.add(lblCardTitle);
        card.add(Box.createVerticalStrut(8));
        card.add(lblUserName);
        card.add(lblUserEmail);
        card.add(Box.createVerticalStrut(15));
        card.add(lblBalTitle);
        card.add(Box.createVerticalStrut(4));
        card.add(lblUserBalance);
        card.add(Box.createVerticalStrut(4));
        card.add(lblLastUpdated);
        card.add(Box.createVerticalStrut(15));
        card.add(btnRefresh);

        return card;
    }

    private JTabbedPane createOperationsTabs() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(new Font("Segoe UI", Font.BOLD, 13));
        tabs.addTab("  Nạp Tiền  ", createDepositTab());
        tabs.addTab("  Rút Tiền  ", createWithdrawTab());
        tabs.addTab("  Chuyển Khoản  ", createTransferTab());
        return tabs;
    }

    // ==================== TAB NẠP TIỀN ====================
    private JPanel createDepositTab() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(Color.WHITE);
        panel.setBorder(new EmptyBorder(16, 20, 16, 20));

        JLabel lblAmount = new JLabel("Số tiền muốn nạp (VNĐ):");
        lblAmount.setFont(new Font("Segoe UI", Font.BOLD, 13));
        txtDepositAmount = new JTextField();
        txtDepositAmount.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        txtDepositAmount.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        txtDepositAmount.putClientProperty("JTextField.placeholderText", "Ví dụ: 500000");

        // Quick amount chips
        JPanel quickChips = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        quickChips.setBackground(Color.WHITE);
        long[] amounts = {50000, 100000, 200000, 500000, 1000000, 5000000};
        for (long a : amounts) {
            JButton chip = new JButton("+" + (a >= 1000000 ? (a / 1000000) + "M" : (a / 1000) + "k"));
            chip.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            chip.setFocusable(false);
            chip.addActionListener(e -> txtDepositAmount.setText(String.valueOf(a)));
            quickChips.add(chip);
        }

        JButton btnConfirmDeposit = new JButton("XÁC NHẬN NẠP TIỀN");
        btnConfirmDeposit.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        btnConfirmDeposit.setBackground(new Color(25, 135, 84));
        btnConfirmDeposit.setForeground(Color.WHITE);
        btnConfirmDeposit.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnConfirmDeposit.setFocusable(false);
        btnConfirmDeposit.addActionListener(e -> handleDeposit());

        panel.add(lblAmount);
        panel.add(Box.createVerticalStrut(6));
        panel.add(txtDepositAmount);
        panel.add(Box.createVerticalStrut(6));
        panel.add(quickChips);
        panel.add(Box.createVerticalStrut(15));
        panel.add(btnConfirmDeposit);
        panel.add(Box.createVerticalGlue());

        return panel;
    }

    // ==================== TAB RÚT TIỀN ====================
    private JPanel createWithdrawTab() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(Color.WHITE);
        panel.setBorder(new EmptyBorder(16, 20, 16, 20));

        JLabel lblAmount = new JLabel("Số tiền muốn rút (VNĐ):");
        lblAmount.setFont(new Font("Segoe UI", Font.BOLD, 13));
        txtWithdrawAmount = new JTextField();
        txtWithdrawAmount.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        txtWithdrawAmount.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        txtWithdrawAmount.putClientProperty("JTextField.placeholderText", "Ví dụ: 200000");

        JPanel quickChips = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        quickChips.setBackground(Color.WHITE);
        long[] amounts = {50000, 100000, 200000, 500000, 1000000};
        for (long a : amounts) {
            JButton chip = new JButton("-" + (a >= 1000000 ? (a / 1000000) + "M" : (a / 1000) + "k"));
            chip.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            chip.setFocusable(false);
            chip.addActionListener(e -> txtWithdrawAmount.setText(String.valueOf(a)));
            quickChips.add(chip);
        }

        JButton btnConfirmWithdraw = new JButton("XÁC NHẬN RÚT TIỀN");
        btnConfirmWithdraw.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        btnConfirmWithdraw.setBackground(new Color(253, 126, 20));
        btnConfirmWithdraw.setForeground(Color.WHITE);
        btnConfirmWithdraw.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnConfirmWithdraw.setFocusable(false);
        btnConfirmWithdraw.addActionListener(e -> handleWithdraw());

        panel.add(lblAmount);
        panel.add(Box.createVerticalStrut(6));
        panel.add(txtWithdrawAmount);
        panel.add(Box.createVerticalStrut(6));
        panel.add(quickChips);
        panel.add(Box.createVerticalStrut(15));
        panel.add(btnConfirmWithdraw);
        panel.add(Box.createVerticalGlue());

        return panel;
    }

    // ==================== TAB CHUYỂN KHOẢN ====================
    private JPanel createTransferTab() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(Color.WHITE);
        panel.setBorder(new EmptyBorder(12, 20, 12, 20));

        JLabel lblReceiver = new JLabel("Email người nhận:");
        lblReceiver.setFont(new Font("Segoe UI", Font.BOLD, 12));
        txtTransferReceiver = new JTextField();
        txtTransferReceiver.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        txtTransferReceiver.putClientProperty("JTextField.placeholderText", "nhanvien@gmail.com");

        JLabel lblAmount = new JLabel("Số tiền chuyển (VNĐ):");
        lblAmount.setFont(new Font("Segoe UI", Font.BOLD, 12));
        txtTransferAmount = new JTextField();
        txtTransferAmount.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        txtTransferAmount.putClientProperty("JTextField.placeholderText", "Ví dụ: 100000");

        JLabel lblNote = new JLabel("Lời nhắn:");
        lblNote.setFont(new Font("Segoe UI", Font.BOLD, 12));
        txtTransferNote = new JTextField();
        txtTransferNote.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        txtTransferNote.putClientProperty("JTextField.placeholderText", "Chuyển tiền...");

        JButton btnConfirmTransfer = new JButton("XÁC NHẬN CHUYỂN KHOẢN");
        btnConfirmTransfer.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        btnConfirmTransfer.setBackground(new Color(13, 110, 253));
        btnConfirmTransfer.setForeground(Color.WHITE);
        btnConfirmTransfer.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnConfirmTransfer.setFocusable(false);
        btnConfirmTransfer.addActionListener(e -> handleTransfer());

        panel.add(lblReceiver);
        panel.add(Box.createVerticalStrut(4));
        panel.add(txtTransferReceiver);
        panel.add(Box.createVerticalStrut(8));
        panel.add(lblAmount);
        panel.add(Box.createVerticalStrut(4));
        panel.add(txtTransferAmount);
        panel.add(Box.createVerticalStrut(8));
        panel.add(lblNote);
        panel.add(Box.createVerticalStrut(4));
        panel.add(txtTransferNote);
        panel.add(Box.createVerticalStrut(14));
        panel.add(btnConfirmTransfer);
        panel.add(Box.createVerticalGlue());

        return panel;
    }

    // ==================== BẢNG LỊCH SỬ GIAO DỊCH TRONG PHIÊN ====================
    private JPanel createActivityLogPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(222, 226, 230), 1),
                new EmptyBorder(10, 12, 10, 12)
        ));

        JLabel lblLogTitle = new JLabel("Lịch Sử Giao Dịch Trong Phiên Làm Việc");
        lblLogTitle.setFont(new Font("Segoe UI", Font.BOLD, 13));
        panel.add(lblLogTitle, BorderLayout.NORTH);

        String[] columns = {"Thời Gian", "Loại Giao Dịch", "Số Tiền", "Trạng Thái", "Nội Dung"};
        activityTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };
        activityTable = new JTable(activityTableModel);
        activityTable.setRowHeight(25);
        activityTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        activityTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));

        // Color renderer for status
        activityTable.getColumnModel().getColumn(3).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
                if ("Thành công".equals(value)) {
                    setForeground(new Color(25, 135, 84));
                    setFont(getFont().deriveFont(Font.BOLD));
                } else {
                    setForeground(new Color(220, 53, 69));
                    setFont(getFont().deriveFont(Font.BOLD));
                }
                return c;
            }
        });

        panel.add(new JScrollPane(activityTable), BorderLayout.CENTER);
        return panel;
    }

    private void refreshUserData() {
        try {
            Response<User> resp = service.refreshUserInfo();
            if (resp.isSuccess() && resp.getData() != null) {
                User u = resp.getData();
                lblUserName.setText("Chủ TK: " + u.getName());
                lblUserEmail.setText("Email: " + u.getEmail());
                lblUserBalance.setText(FormatUtil.formatCurrency(u.getBalance()));
                lblLastUpdated.setText("Cập nhật: " + (u.getLastUpdated() != null ? u.getLastUpdated().toString() : "Vừa xong"));
            }
        } catch (Exception e) {
            System.err.println("Lỗi refreshUserData: " + e.getMessage());
        }
    }

    private void addActivityRow(String op, BigDecimal amount, boolean success, String details) {
        activityTableModel.insertRow(0, new Object[]{
                FormatUtil.formatDateTime(LocalDateTime.now()),
                op,
                amount != null ? FormatUtil.formatCurrency(amount) : "-",
                success ? "Thành công" : "Thất bại",
                details
        });
    }

    // ==================== XỬ LÝ SỰ KIỆN GIAO DỊCH ====================

    private void handleDeposit() {
        String amtStr = txtDepositAmount.getText().trim();
        if (amtStr.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập số tiền muốn nạp!", "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            BigDecimal amt = new BigDecimal(amtStr);
            Response<BigDecimal> resp = service.deposit(amt);
            if (resp.isSuccess()) {
                refreshUserData();
                addActivityRow("Nạp tiền", amt, true, "Nạp +" + FormatUtil.formatCurrency(amt) + " thành công");
                txtDepositAmount.setText("");
                JOptionPane.showMessageDialog(this,
                        "Nạp tiền thành công!\nSố dư mới: " + FormatUtil.formatCurrency(resp.getData()),
                        "Thông báo", JOptionPane.INFORMATION_MESSAGE);
            } else {
                addActivityRow("Nạp tiền", amt, false, resp.getMessage());
                JOptionPane.showMessageDialog(this, resp.getMessage(), "Giao dịch thất bại", JOptionPane.ERROR_MESSAGE);
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Số tiền nạp không hợp lệ!", "Lỗi nhập liệu", JOptionPane.ERROR_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Không thể kết nối đến hệ thống ngân hàng, vui lòng thử lại sau!", "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleWithdraw() {
        String amtStr = txtWithdrawAmount.getText().trim();
        if (amtStr.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập số tiền muốn rút!", "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            BigDecimal amt = new BigDecimal(amtStr);
            Response<BigDecimal> resp = service.withdraw(amt);
            if (resp.isSuccess()) {
                refreshUserData();
                addActivityRow("Rút tiền", amt, true, "Rút -" + FormatUtil.formatCurrency(amt) + " thành công");
                txtWithdrawAmount.setText("");
                JOptionPane.showMessageDialog(this,
                        "Rút tiền thành công!\nSố dư mới: " + FormatUtil.formatCurrency(resp.getData()),
                        "Thông báo", JOptionPane.INFORMATION_MESSAGE);
            } else {
                addActivityRow("Rút tiền", amt, false, resp.getMessage());
                JOptionPane.showMessageDialog(this, resp.getMessage(), "Giao dịch thất bại", JOptionPane.ERROR_MESSAGE);
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Số tiền rút không hợp lệ!", "Lỗi nhập liệu", JOptionPane.ERROR_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Không thể kết nối đến hệ thống ngân hàng, vui lòng thử lại sau!", "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleTransfer() {
        String receiver = txtTransferReceiver.getText().trim();
        String amtStr = txtTransferAmount.getText().trim();
        String note = txtTransferNote.getText().trim();

        if (receiver.isEmpty() || amtStr.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập email người nhận và số tiền chuyển!", "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            BigDecimal amt = new BigDecimal(amtStr);
            Response<BigDecimal> resp = service.transfer(receiver, amt);
            if (resp.isSuccess()) {
                refreshUserData();
                String desc = "Chuyển tới " + receiver + (note.isEmpty() ? "" : " (" + note + ")");
                addActivityRow("Chuyển khoản", amt, true, desc);
                txtTransferReceiver.setText("");
                txtTransferAmount.setText("");
                txtTransferNote.setText("");
                JOptionPane.showMessageDialog(this,
                        "Chuyển khoản thành công tới " + receiver + "!\nSố dư mới: " + FormatUtil.formatCurrency(resp.getData()),
                        "Thông báo", JOptionPane.INFORMATION_MESSAGE);
            } else {
                addActivityRow("Chuyển khoản", amt, false, resp.getMessage());
                JOptionPane.showMessageDialog(this, resp.getMessage(), "Giao dịch thất bại", JOptionPane.ERROR_MESSAGE);
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Số tiền chuyển không hợp lệ!", "Lỗi nhập liệu", JOptionPane.ERROR_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Không thể kết nối đến hệ thống ngân hàng, vui lòng thử lại sau!", "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleLogout() {
        int confirm = JOptionPane.showConfirmDialog(this, "Bạn có chắc chắn muốn đăng xuất?", "Đăng xuất", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            service.logout();
            AuthFrame auth = new AuthFrame(service);
            auth.setVisible(true);
            this.dispose();
        }
    }
}
