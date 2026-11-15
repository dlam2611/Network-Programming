package com.multiserver.server.ui;

import com.formdev.flatlaf.FlatLightLaf;
import com.multiserver.common.model.AccessLog;
import com.multiserver.common.model.User;
import com.multiserver.common.util.FormatUtil;
import com.multiserver.common.util.PasswordUtil;
import com.multiserver.server.config.ServerConfig;
import com.multiserver.server.dao.UserDAO;
import com.multiserver.server.service.BankServiceImpl;
import com.multiserver.server.service.PeerSyncManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;

/**
 * Giao diện Server chuẩn Theme Sáng (FlatLaf Light):
 * 1. Hiển thị nhật ký truy cập từ Client (IP, thao tác, ngày giờ, trạng thái, chi tiết).
 * 2. Quản lý trực quan bảng dữ liệu 'users' trên chính Server (CRUD + Đồng bộ).
 * 3. Trạng thái kết nối Server và Peer Server theo thời gian thực.
 */
public class ServerGUI extends JFrame implements BankServiceImpl.ServerEventListener, PeerSyncManager.PeerStatusListener {

    private final ServerConfig config;
    private final UserDAO userDAO;
    private final PeerSyncManager peerSyncManager;

    // Header Components
    private JLabel lblServerStatus;
    private JLabel lblPeerStatus;
    private JLabel lblDbInfo;

    // Table Access Logs
    private DefaultTableModel logTableModel;
    private JTable logTable;
    private TableRowSorter<DefaultTableModel> logSorter;
    private JTextField txtSearchLog;
    private JComboBox<String> cbFilterOp;

    // Table Database Users
    private DefaultTableModel userTableModel;
    private JTable userTable;
    private TableRowSorter<DefaultTableModel> userSorter;
    private JTextField txtSearchUser;

    // Stats
    private JLabel lblTotalUsers;
    private JLabel lblTotalBalance;
    private JLabel lblTotalRequests;
    private long totalRequests = 0;

    public ServerGUI(ServerConfig config, UserDAO userDAO, PeerSyncManager peerSyncManager) {
        this.config = config;
        this.userDAO = userDAO;
        this.peerSyncManager = peerSyncManager;

        setupLookAndFeel();
        initComponents();
        loadUserData();
    }

    private void setupLookAndFeel() {
        try {
            UIManager.setLookAndFeel(new FlatLightLaf());
        } catch (Exception ignored) {
        }
    }

    private void initComponents() {
        setTitle("HỆ THỐNG MÁY CHỦ PHÂN TÁN - " + config.getServerName().toUpperCase());
        setSize(1100, 720);
        setMinimumSize(new Dimension(950, 600));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(new EmptyBorder(12, 12, 12, 12));
        mainPanel.setBackground(new Color(248, 249, 250));

        // 1. Header Panel
        mainPanel.add(createHeaderPanel(), BorderLayout.NORTH);

        // 2. Center Tabs: Tab 1 = Access Logs, Tab 2 = User Database Management
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Segoe UI", Font.BOLD, 13));
        tabbedPane.addTab("  Nhật Ký Truy Cập Client  ", createAccessLogPanel());
        tabbedPane.addTab("  Quản Lý Bảng Dữ Liệu Users  ", createDatabaseManagementPanel());
        mainPanel.add(tabbedPane, BorderLayout.CENTER);

        // 3. Bottom Stats Bar
        mainPanel.add(createStatsBar(), BorderLayout.SOUTH);

        setContentPane(mainPanel);
    }

    private JPanel createHeaderPanel() {
        JPanel header = new JPanel(new BorderLayout(15, 10));
        header.setBackground(Color.WHITE);
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(222, 226, 230), 1),
                new EmptyBorder(12, 16, 12, 16)
        ));

        // Left title & Server info
        JPanel left = new JPanel(new GridLayout(2, 1, 4, 4));
        left.setBackground(Color.WHITE);

        JLabel lblTitle = new JLabel(config.getServerName() + " (RMI Port: " + config.getServerPort() + ")");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblTitle.setForeground(new Color(13, 110, 253));

        lblServerStatus = new JLabel("● Đang lắng nghe trên Port: " + config.getServerPort());
        lblServerStatus.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblServerStatus.setForeground(new Color(25, 135, 84));

        left.add(lblTitle);
        left.add(lblServerStatus);

        // Right Peer & DB info
        JPanel right = new JPanel(new GridLayout(2, 1, 4, 4));
        right.setBackground(Color.WHITE);

        lblPeerStatus = new JLabel("○ Đang kết nối " + config.getPeerName() + " (" + config.getPeerHost() + ":" + config.getPeerPort() + ")...");
        lblPeerStatus.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblPeerStatus.setForeground(new Color(220, 53, 69));

        lblDbInfo = new JLabel("Database: " + config.getDbUrl());
        lblDbInfo.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblDbInfo.setForeground(new Color(108, 117, 125));

        right.add(lblPeerStatus);
        right.add(lblDbInfo);

        header.add(left, BorderLayout.WEST);
        header.add(right, BorderLayout.EAST);

        return header;
    }

    // ==================== TAB 1: NHẬT KÝ TRUY CẬP CLIENT ====================
    private JPanel createAccessLogPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(Color.WHITE);
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Filter / Tool bar
        JPanel toolBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        toolBar.setBackground(Color.WHITE);

        toolBar.add(new JLabel("Lọc thao tác:"));
        cbFilterOp = new JComboBox<>(new String[]{
                "Tất cả", "ĐĂNG KÝ", "ĐĂNG NHẬP", "NẠP TIỀN", "RÚT TIỀN", "CHUYỂN KHOẢN", "ĐỒNG BỘ"
        });
        cbFilterOp.addActionListener(e -> applyLogFilter());
        toolBar.add(cbFilterOp);

        toolBar.add(new JLabel("Tìm kiếm:"));
        txtSearchLog = new JTextField(15);
        txtSearchLog.putClientProperty("JTextField.placeholderText", "IP, chi tiết...");
        txtSearchLog.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyReleased(java.awt.event.KeyEvent e) {
                applyLogFilter();
            }
        });
        toolBar.add(txtSearchLog);

        JButton btnClearLog = new JButton("Xóa nhật ký");
        btnClearLog.addActionListener(e -> logTableModel.setRowCount(0));
        toolBar.add(btnClearLog);

        panel.add(toolBar, BorderLayout.NORTH);

        // Table
        String[] columns = {"STT", "Thời Gian", "IP Client", "Thao Tác", "Trạng Thái", "Chi Tiết Giao Dịch"};
        logTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        logTable = new JTable(logTableModel);
        logSorter = new TableRowSorter<>(logTableModel);
        logTable.setRowSorter(logSorter);
        logTable.setRowHeight(26);
        logTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        logTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));

        // Column widths
        logTable.getColumnModel().getColumn(0).setPreferredWidth(50);
        logTable.getColumnModel().getColumn(1).setPreferredWidth(140);
        logTable.getColumnModel().getColumn(2).setPreferredWidth(120);
        logTable.getColumnModel().getColumn(3).setPreferredWidth(140);
        logTable.getColumnModel().getColumn(4).setPreferredWidth(100);
        logTable.getColumnModel().getColumn(5).setPreferredWidth(450);

        // Color renderer for status & operation
        logTable.getColumnModel().getColumn(4).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
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

        JScrollPane scrollPane = new JScrollPane(logTable);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private void applyLogFilter() {
        String selectedOp = (String) cbFilterOp.getSelectedItem();
        String keyword = txtSearchLog.getText().trim();

        RowFilter<DefaultTableModel, Object> rf = new RowFilter<>() {
            @Override
            public boolean include(Entry<? extends DefaultTableModel, ?> entry) {
                if (selectedOp != null && !"Tất cả".equals(selectedOp)) {
                    String op = entry.getStringValue(3);
                    if (!op.toUpperCase().contains(selectedOp.toUpperCase())) {
                        return false;
                    }
                }
                if (!keyword.isEmpty()) {
                    boolean match = false;
                    for (int i = 0; i < entry.getValueCount(); i++) {
                        if (entry.getStringValue(i).toLowerCase().contains(keyword.toLowerCase())) {
                            match = true;
                            break;
                        }
                    }
                    return match;
                }
                return true;
            }
        };
        logSorter.setRowFilter(rf);
    }

    // ==================== TAB 2: QUẢN LÝ DỮ LIỆU USERS ====================
    private JPanel createDatabaseManagementPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(Color.WHITE);
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Tool bar
        JPanel toolBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        toolBar.setBackground(Color.WHITE);

        JButton btnRefresh = new JButton("Làm mới");
        btnRefresh.setIcon(UIManager.getIcon("FileView.floppyDriveIcon"));
        btnRefresh.addActionListener(e -> loadUserData());
        toolBar.add(btnRefresh);

        JButton btnAdd = new JButton("Thêm tài khoản");
        btnAdd.addActionListener(e -> showAddUserDialog());
        toolBar.add(btnAdd);

        JButton btnEdit = new JButton("Sửa số dư / thông tin");
        btnEdit.addActionListener(e -> showEditUserDialog());
        toolBar.add(btnEdit);

        JButton btnDelete = new JButton("Xóa tài khoản");
        btnDelete.addActionListener(e -> deleteSelectedUser());
        toolBar.add(btnDelete);

        JButton btnSyncAll = new JButton("Đồng bộ toàn bộ với Peer");
        btnSyncAll.setBackground(new Color(13, 110, 253));
        btnSyncAll.setForeground(Color.WHITE);
        btnSyncAll.addActionListener(e -> {
            boolean ok = peerSyncManager.triggerManualFullSync();
            if (ok) {
                JOptionPane.showMessageDialog(this, "Đã gửi lệnh đồng bộ toàn bộ dữ liệu tới " + config.getPeerName() + "!", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, "Peer Server hiện chưa kết nối. Hãy khởi động Server đối ứng trước!", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
            }
        });
        toolBar.add(btnSyncAll);

        toolBar.add(Box.createHorizontalStrut(20));
        toolBar.add(new JLabel("Tìm kiếm:"));
        txtSearchUser = new JTextField(15);
        txtSearchUser.putClientProperty("JTextField.placeholderText", "Tên hoặc Email...");
        txtSearchUser.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyReleased(java.awt.event.KeyEvent e) {
                String kw = txtSearchUser.getText().trim();
                if (kw.isEmpty()) {
                    userSorter.setRowFilter(null);
                } else {
                    userSorter.setRowFilter(RowFilter.regexFilter("(?i)" + kw, 1, 2));
                }
            }
        });
        toolBar.add(txtSearchUser);

        panel.add(toolBar, BorderLayout.NORTH);

        // User Table
        String[] columns = {"ID", "Họ Tên", "Email", "Số Dư (VNĐ)", "Cập Nhật Lần Cuối"};
        userTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        userTable = new JTable(userTableModel);
        userSorter = new TableRowSorter<>(userTableModel);
        userTable.setRowSorter(userSorter);
        userTable.setRowHeight(28);
        userTable.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        userTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));

        userTable.getColumnModel().getColumn(0).setPreferredWidth(60);
        userTable.getColumnModel().getColumn(1).setPreferredWidth(180);
        userTable.getColumnModel().getColumn(2).setPreferredWidth(220);
        userTable.getColumnModel().getColumn(3).setPreferredWidth(160);
        userTable.getColumnModel().getColumn(4).setPreferredWidth(180);

        // Highlight balance column
        userTable.getColumnModel().getColumn(3).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setForeground(new Color(13, 110, 253));
                setFont(getFont().deriveFont(Font.BOLD));
                return c;
            }
        });

        panel.add(new JScrollPane(userTable), BorderLayout.CENTER);
        return panel;
    }

    private JPanel createStatsBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 25, 6));
        bar.setBackground(new Color(233, 236, 239));

        lblTotalUsers = new JLabel("Tổng tài khoản: 0");
        lblTotalUsers.setFont(new Font("Segoe UI", Font.BOLD, 12));

        lblTotalBalance = new JLabel("Tổng tiền trên Server: 0.00 đ");
        lblTotalBalance.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblTotalBalance.setForeground(new Color(25, 135, 84));

        lblTotalRequests = new JLabel("Tổng số yêu cầu phục vụ: 0");
        lblTotalRequests.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        bar.add(lblTotalUsers);
        bar.add(lblTotalBalance);
        bar.add(lblTotalRequests);

        return bar;
    }

    public synchronized void loadUserData() {
        SwingUtilities.invokeLater(() -> {
            userTableModel.setRowCount(0);
            List<User> users = userDAO.findAll();
            for (User u : users) {
                userTableModel.addRow(new Object[]{
                        u.getUserId(),
                        u.getName(),
                        u.getEmail(),
                        FormatUtil.formatCurrency(u.getBalance()),
                        u.getLastUpdated() != null ? u.getLastUpdated().toString() : ""
                });
            }

            lblTotalUsers.setText("Tổng tài khoản: " + users.size());
            lblTotalBalance.setText("Tổng tiền trên Server: " + FormatUtil.formatCurrency(userDAO.getTotalBalance()));
        });
    }

    private void showAddUserDialog() {
        JTextField txtName = new JTextField();
        JTextField txtEmail = new JTextField();
        JPasswordField txtPass = new JPasswordField();
        JTextField txtBalance = new JTextField("0");

        JPanel form = new JPanel(new GridLayout(4, 2, 8, 8));
        form.add(new JLabel("Họ và tên:"));
        form.add(txtName);
        form.add(new JLabel("Email:"));
        form.add(txtEmail);
        form.add(new JLabel("Mật khẩu:"));
        form.add(txtPass);
        form.add(new JLabel("Số dư ban đầu (VNĐ):"));
        form.add(txtBalance);

        int result = JOptionPane.showConfirmDialog(this, form, "Thêm tài khoản mới (Tự động đồng bộ sang Peer)", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            String name = txtName.getText().trim();
            String email = txtEmail.getText().trim();
            String pass = new String(txtPass.getPassword()).trim();
            String balStr = txtBalance.getText().trim();

            if (name.isEmpty() || email.isEmpty() || pass.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Vui lòng nhập đầy đủ họ tên, email và mật khẩu!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }

            try {
                BigDecimal bal = new BigDecimal(balStr);
                User user = new User(name, email.toLowerCase(), PasswordUtil.hashPassword(pass), bal);
                boolean ok = userDAO.insertUser(user);
                if (ok) {
                    // Đồng bộ sang Peer
                    peerSyncManager.syncRegister(user);
                    loadUserData();
                    JOptionPane.showMessageDialog(this, "Đã thêm tài khoản và gửi lệnh đồng bộ sang " + config.getPeerName() + "!", "Thành công", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(this, "Không thể thêm tài khoản (Email có thể đã tồn tại)!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Số dư không hợp lệ!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void showEditUserDialog() {
        int selectedRow = userTable.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn một người dùng trong bảng để sửa!", "Chưa chọn người dùng", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int modelRow = userTable.convertRowIndexToModel(selectedRow);
        String email = (String) userTableModel.getValueAt(modelRow, 2);
        User user = userDAO.findByEmail(email);
        if (user == null) return;

        JTextField txtName = new JTextField(user.getName());
        JTextField txtBalance = new JTextField(user.getBalance().toString());

        JPanel form = new JPanel(new GridLayout(3, 2, 8, 8));
        form.add(new JLabel("Email (không đổi):"));
        form.add(new JLabel(user.getEmail()));
        form.add(new JLabel("Họ và tên:"));
        form.add(txtName);
        form.add(new JLabel("Số dư mới (VNĐ):"));
        form.add(txtBalance);

        int result = JOptionPane.showConfirmDialog(this, form, "Cập nhật tài khoản " + user.getEmail(), JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            try {
                user.setName(txtName.getText().trim());
                user.setBalance(new BigDecimal(txtBalance.getText().trim()));
                user.setLastUpdated(new Timestamp(System.currentTimeMillis()));

                boolean ok = userDAO.updateUser(user);
                if (ok) {
                    peerSyncManager.syncUpdateUser(user);
                    loadUserData();
                    JOptionPane.showMessageDialog(this, "Cập nhật thành công và đã đồng bộ sang " + config.getPeerName() + "!", "Thành công", JOptionPane.INFORMATION_MESSAGE);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Số dư không hợp lệ!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void deleteSelectedUser() {
        int selectedRow = userTable.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn một người dùng trong bảng để xóa!", "Chưa chọn", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int modelRow = userTable.convertRowIndexToModel(selectedRow);
        String email = (String) userTableModel.getValueAt(modelRow, 2);

        int confirm = JOptionPane.showConfirmDialog(this,
                "Bạn có chắc muốn xóa tài khoản " + email + "?\nThao tác này cũng sẽ đồng bộ xóa trên " + config.getPeerName() + "!",
                "Xác nhận xóa", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            boolean ok = userDAO.deleteUser(email);
            if (ok) {
                peerSyncManager.syncDeleteUser(email);
                loadUserData();
                JOptionPane.showMessageDialog(this, "Đã xóa tài khoản " + email + "!", "Thành công", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, "Lỗi khi xóa tài khoản!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // ==================== EVENT LISTENERS ====================

    @Override
    public void onAccessLogged(AccessLog log) {
        SwingUtilities.invokeLater(() -> {
            totalRequests++;
            lblTotalRequests.setText("Tổng số yêu cầu phục vụ: " + totalRequests);

            logTableModel.insertRow(0, new Object[]{
                    log.getId(),
                    log.getFormattedTimestamp(),
                    log.getClientIp(),
                    log.getOperation(),
                    log.isSuccess() ? "Thành công" : "Thất bại",
                    log.getDetails()
            });

            // Giới hạn 500 dòng log để tránh tràn RAM giao diện
            if (logTableModel.getRowCount() > 500) {
                logTableModel.removeRow(logTableModel.getRowCount() - 1);
            }
        });
    }

    @Override
    public void onDataChanged() {
        loadUserData();
    }

    @Override
    public void onPeerStatusChanged(boolean connected, String message) {
        SwingUtilities.invokeLater(() -> {
            if (connected) {
                lblPeerStatus.setText("● " + config.getPeerName() + ": Đã kết nối (" + config.getPeerHost() + ":" + config.getPeerPort() + ")");
                lblPeerStatus.setForeground(new Color(25, 135, 84));
            } else {
                lblPeerStatus.setText("○ " + config.getPeerName() + ": Mất kết nối");
                lblPeerStatus.setForeground(new Color(220, 53, 69));
            }
        });
    }
}

