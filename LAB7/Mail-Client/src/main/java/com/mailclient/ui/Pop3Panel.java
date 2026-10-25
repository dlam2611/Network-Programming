package com.mailclient.ui;

import com.mailclient.model.EmailAttachment;
import com.mailclient.model.EmailMessage;
import com.mailclient.model.SecurityType;
import com.mailclient.model.ServerConfig;
import com.mailclient.protocol.common.EmailParser;
import com.mailclient.protocol.pop3.Pop3Client;
import com.mailclient.protocol.pop3.Pop3MessageInfo;
import com.mailclient.protocol.pop3.Pop3Stat;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.io.FileOutputStream;
import java.util.*;
import java.util.List;

/**
 * Giao diện POP3 Client nâng cao:
 * - Hỗ trợ ĐẢO NGƯỢC THỨ TỰ (thư mới nhất hôm nay lên đầu).
 * - Hỗ trợ PHÂN TRANG (Pagination: 10, 20, 50, 100 thư/trang).
 * - Tự động tải trước tiêu đề (Header/TOP) cho trang hiện tại.
 * - Tải đầy đủ nội dung (RETR) khi nhấn chọn thư.
 */
public class Pop3Panel extends JPanel {
    // Cấu hình máy chủ
    private JComboBox<String> cboPresets;
    private JTextField txtHost;
    private JTextField txtPort;
    private JComboBox<SecurityType> cboSecurity;
    private JTextField txtUsername;
    private JPasswordField txtPassword;
    private JCheckBox chkShowPassword;
    private JCheckBox chkRecentMode;

    // Nút điều khiển
    private JButton btnConnect;
    private JButton btnDisconnect;
    private JButton btnRefresh;
    private JButton btnStat;
    private JButton btnDelete;
    private JLabel lblStatus;

    // Danh sách toàn bộ thư & Phân trang
    private final List<Pop3MessageInfo> allMessages = new ArrayList<>();
    private final Map<Integer, EmailMessage> emailCache = new HashMap<>();
    private int currentPage = 1;
    private int pageSize = 20;
    private boolean sortNewestFirst = true;
    private SwingWorker<Void, Object[]> currentHeaderWorker = null;

    // Điều khiển phân trang
    private JButton btnFirstPage;
    private JButton btnPrevPage;
    private JLabel lblPageInfo;
    private JButton btnNextPage;
    private JButton btnLastPage;
    private JComboBox<Integer> cboPageSize;
    private JComboBox<String> cboSortOrder;

    // Bảng hiển thị
    private JTable tblMessages;
    private DefaultTableModel tableModel;

    // Chi tiết thư
    private JLabel lblDetailSubject;
    private JLabel lblDetailFrom;
    private JLabel lblDetailTo;
    private JLabel lblDetailDate;
    private JTextArea txtDetailBody;
    private JPanel pnlAttachments;
    private JTextArea txtRawContent;
    private LogPanel logPanel;

    // Client đang kết nối
    private Pop3Client activeClient;
    private ServerConfig activeConfig;

    public Pop3Panel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(12, 12, 12, 12));

        initUI();
    }

    private void initUI() {
        add(createServerControlPanel(), BorderLayout.NORTH);

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setResizeWeight(0.50);
        splitPane.setContinuousLayout(true);

        splitPane.setTopComponent(createMailListPanel());
        splitPane.setBottomComponent(createMailDetailPanel());
        splitPane.setDividerLocation(260); // Đảm bảo chiều cao đủ chứa thoải mái 6 - 8 dòng thư

        add(splitPane, BorderLayout.CENTER);

        applyPreset(0);
    }

    private JPanel createServerControlPanel() {
        JPanel container = new JPanel(new BorderLayout(6, 6));

        JPanel configPanel = new JPanel(new GridBagLayout());
        configPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(200, 205, 215)),
                " Cấu hình Máy chủ POP3 & Xác thực ",
                TitledBorder.LEFT,
                TitledBorder.TOP,
                new Font("SansSerif", Font.BOLD, 12),
                new Color(30, 41, 59)
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(3, 5, 3, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Dòng 0: Mẫu POP3
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.0;
        configPanel.add(new JLabel("Mẫu POP3:"), gbc);

        cboPresets = new JComboBox<>(new String[]{
                "Gmail (pop.gmail.com:995 - SSL/TLS)",
                "Outlook / Office 365 (outlook.office365.com:995 - SSL/TLS)",
                "Yahoo Mail (pop.mail.yahoo.com:995 - SSL/TLS)",
                "Localhost (localhost:110 - Plain TCP)",
                "Tùy chỉnh (Custom)..."
        });
        cboPresets.addActionListener(e -> applyPreset(cboPresets.getSelectedIndex()));
        gbc.gridx = 1; gbc.gridy = 0; gbc.gridwidth = 3; gbc.weightx = 1.0;
        configPanel.add(cboPresets, gbc);

        // Dòng 1: Host & Port
        gbc.gridwidth = 1;
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0.0;
        configPanel.add(new JLabel("Máy chủ:"), gbc);

        txtHost = new JTextField("pop.gmail.com", 16);
        gbc.gridx = 1; gbc.gridy = 1; gbc.weightx = 0.6;
        configPanel.add(txtHost, gbc);

        gbc.gridx = 2; gbc.gridy = 1; gbc.weightx = 0.0;
        configPanel.add(new JLabel("Cổng (Port):"), gbc);

        txtPort = new JTextField("995", 5);
        gbc.gridx = 3; gbc.gridy = 1; gbc.weightx = 0.4;
        configPanel.add(txtPort, gbc);

        // Dòng 2: Bảo mật & Tài khoản
        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0.0;
        configPanel.add(new JLabel("Bảo mật:"), gbc);

        cboSecurity = new JComboBox<>(new SecurityType[]{SecurityType.SSL_TLS, SecurityType.NONE});
        gbc.gridx = 1; gbc.gridy = 2; gbc.weightx = 0.6;
        configPanel.add(cboSecurity, gbc);

        gbc.gridx = 2; gbc.gridy = 2; gbc.weightx = 0.0;
        configPanel.add(new JLabel("Tài khoản:"), gbc);

        txtUsername = new JTextField("", 14);
        gbc.gridx = 3; gbc.gridy = 2; gbc.weightx = 0.4;
        configPanel.add(txtUsername, gbc);

        // Dòng 3: Mật khẩu
        gbc.gridx = 0; gbc.gridy = 3; gbc.weightx = 0.0;
        configPanel.add(new JLabel("Mật khẩu:"), gbc);

        txtPassword = new JPasswordField("", 16);
        gbc.gridx = 1; gbc.gridy = 3; gbc.weightx = 0.6;
        configPanel.add(txtPassword, gbc);

        chkShowPassword = new JCheckBox("Hiện");
        chkShowPassword.addActionListener(e -> txtPassword.setEchoChar(chkShowPassword.isSelected() ? (char) 0 : '•'));
        gbc.gridx = 2; gbc.gridy = 3; gbc.weightx = 0.2;
        configPanel.add(chkShowPassword, gbc);

        // Dòng 4: Tùy chọn Recent Mode (lấy thư 30 ngày gần nhất)
        gbc.gridx = 0; gbc.gridy = 4; gbc.weightx = 0.0;
        configPanel.add(new JLabel("Chế độ:"), gbc);

        chkRecentMode = new JCheckBox("Lấy thư mới nhất (Recent Mode - 30 ngày gần đây)", true);
        chkRecentMode.setFont(new Font("SansSerif", Font.BOLD, 12));
        chkRecentMode.setForeground(new Color(2, 132, 199));
        chkRecentMode.setToolTipText("Khuyên dùng cho Gmail: Tải các email mới nhất trong 30 ngày qua thay vì bị kẹt ở đợt tải các thư cũ từ những năm trước.");
        gbc.gridx = 1; gbc.gridy = 4; gbc.gridwidth = 3; gbc.weightx = 1.0;
        configPanel.add(chkRecentMode, gbc);

        container.add(configPanel, BorderLayout.CENTER);

        // Toolbar nút bấm
        JPanel actionToolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));

        btnConnect = new JButton("Kết nối & Tải thư (LIST)");
        btnConnect.setFont(new Font("SansSerif", Font.BOLD, 12));
        btnConnect.setBackground(new Color(16, 185, 129));
        btnConnect.setForeground(Color.WHITE);
        btnConnect.setFocusable(false);
        btnConnect.addActionListener(e -> connectAndFetchList());

        btnDisconnect = new JButton("Ngắt kết nối (QUIT)");
        btnDisconnect.setEnabled(false);
        btnDisconnect.setFocusable(false);
        btnDisconnect.addActionListener(e -> disconnect());

        btnRefresh = new JButton("Làm mới");
        btnRefresh.setEnabled(false);
        btnRefresh.setFocusable(false);
        btnRefresh.addActionListener(e -> fetchMailList());

        btnStat = new JButton("Thống kê (STAT)");
        btnStat.setEnabled(false);
        btnStat.setFocusable(false);
        btnStat.addActionListener(e -> executeStat());

        btnDelete = new JButton("Xóa thư (DELE)");
        btnDelete.setEnabled(false);
        btnDelete.setFocusable(false);
        btnDelete.setForeground(new Color(220, 38, 38));
        btnDelete.addActionListener(e -> deleteSelectedMail());

        lblStatus = new JLabel("Chưa kết nối");
        lblStatus.setFont(new Font("SansSerif", Font.ITALIC, 12));
        lblStatus.setForeground(new Color(100, 116, 139));

        actionToolbar.add(btnConnect);
        actionToolbar.add(btnDisconnect);
        actionToolbar.add(btnRefresh);
        actionToolbar.add(btnStat);
        actionToolbar.add(btnDelete);
        actionToolbar.add(Box.createHorizontalStrut(15));
        actionToolbar.add(lblStatus);

        container.add(actionToolbar, BorderLayout.SOUTH);

        return container;
    }

    private JPanel createMailListPanel() {
        JPanel panel = new JPanel(new BorderLayout(4, 4));
        panel.setPreferredSize(new Dimension(800, 260)); // Chiều cao 260px đảm bảo hiển thị ít nhất 7 - 8 dòng
        panel.setMinimumSize(new Dimension(500, 210));
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(200, 205, 215)),
                " Hộp thư đến (Inbox) ",
                TitledBorder.LEFT,
                TitledBorder.TOP,
                new Font("SansSerif", Font.BOLD, 12),
                new Color(30, 41, 59)
        ));

        String[] columns = {"STT (ID)", "Người gửi (From)", "Tiêu đề (Subject)", "Ngày gửi (Date)", "Kích thước"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        tblMessages = new JTable(tableModel);
        tblMessages.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tblMessages.setRowHeight(25);
        tblMessages.setFont(new Font("SansSerif", Font.PLAIN, 12));
        tblMessages.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));

        tblMessages.getColumnModel().getColumn(0).setPreferredWidth(65);
        tblMessages.getColumnModel().getColumn(0).setMaxWidth(85);
        tblMessages.getColumnModel().getColumn(1).setPreferredWidth(200);
        tblMessages.getColumnModel().getColumn(2).setPreferredWidth(340);
        tblMessages.getColumnModel().getColumn(3).setPreferredWidth(170);
        tblMessages.getColumnModel().getColumn(4).setPreferredWidth(90);

        tblMessages.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int row = tblMessages.getSelectedRow();
                if (row != -1) {
                    int msgNum = (int) tableModel.getValueAt(row, 0);
                    loadMailDetail(msgNum);
                    btnDelete.setEnabled(true);
                } else {
                    btnDelete.setEnabled(false);
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(tblMessages);
        panel.add(scrollPane, BorderLayout.CENTER);

        // Thanh phân trang và sắp xếp ở phía dưới bảng
        JPanel paginationBar = createPaginationBar();
        panel.add(paginationBar, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createPaginationBar() {
        JPanel bar = new JPanel(new BorderLayout(8, 4));
        bar.setBorder(new EmptyBorder(3, 4, 3, 4));

        // Bên trái: Tùy chọn sắp xếp thứ tự
        JPanel leftSort = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        leftSort.add(new JLabel("Thứ tự:"));
        cboSortOrder = new JComboBox<>(new String[]{
                "Mới nhất trước (Thư mới hôm nay)",
                "Cũ nhất trước"
        });
        cboSortOrder.setFocusable(false);
        cboSortOrder.addActionListener(e -> {
            sortNewestFirst = (cboSortOrder.getSelectedIndex() == 0);
            applySortingAndRender();
        });
        leftSort.add(cboSortOrder);

        // Bên phải: Điều hướng phân trang
        JPanel rightNav = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 2));

        rightNav.add(new JLabel("Số thư/trang:"));
        cboPageSize = new JComboBox<>(new Integer[]{10, 20, 50, 100});
        cboPageSize.setSelectedItem(20);
        cboPageSize.setFocusable(false);
        cboPageSize.addActionListener(e -> {
            pageSize = (Integer) cboPageSize.getSelectedItem();
            currentPage = 1;
            renderCurrentPage();
        });
        rightNav.add(cboPageSize);

        btnFirstPage = new JButton("⏮");
        btnFirstPage.setToolTipText("Trang đầu tiên");
        btnFirstPage.setFocusable(false);
        btnFirstPage.addActionListener(e -> goToPage(1));

        btnPrevPage = new JButton("◀");
        btnPrevPage.setToolTipText("Trang trước");
        btnPrevPage.setFocusable(false);
        btnPrevPage.addActionListener(e -> goToPage(currentPage - 1));

        lblPageInfo = new JLabel("Trang 0 / 0 (0 thư)");
        lblPageInfo.setFont(new Font("SansSerif", Font.BOLD, 12));

        btnNextPage = new JButton("▶");
        btnNextPage.setToolTipText("Trang sau");
        btnNextPage.setFocusable(false);
        btnNextPage.addActionListener(e -> goToPage(currentPage + 1));

        btnLastPage = new JButton("⏭");
        btnLastPage.setToolTipText("Trang cuối cùng");
        btnLastPage.setFocusable(false);
        btnLastPage.addActionListener(e -> goToPage(getTotalPages()));

        rightNav.add(Box.createHorizontalStrut(8));
        rightNav.add(btnFirstPage);
        rightNav.add(btnPrevPage);
        rightNav.add(lblPageInfo);
        rightNav.add(btnNextPage);
        rightNav.add(btnLastPage);

        bar.add(leftSort, BorderLayout.WEST);
        bar.add(rightNav, BorderLayout.EAST);

        updatePaginationControls();
        return bar;
    }

    private JTabbedPane createMailDetailPanel() {
        JTabbedPane tabbedPane = new JTabbedPane();

        // Tab 1: Nội dung thư
        JPanel viewPanel = new JPanel(new BorderLayout(6, 6));
        viewPanel.setBorder(new EmptyBorder(8, 8, 8, 8));

        JPanel headerCard = new JPanel(new GridBagLayout());
        headerCard.setBackground(new Color(241, 245, 249));
        headerCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226, 232, 240)),
                new EmptyBorder(8, 10, 8, 10)
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(2, 4, 2, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        lblDetailSubject = new JLabel("(Chưa chọn thư)");
        lblDetailSubject.setFont(new Font("SansSerif", Font.BOLD, 14));
        lblDetailSubject.setForeground(new Color(15, 23, 42));
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2; gbc.weightx = 1.0;
        headerCard.add(lblDetailSubject, gbc);

        gbc.gridwidth = 1;
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0.5;
        lblDetailFrom = new JLabel("Người gửi: -");
        lblDetailFrom.setFont(new Font("SansSerif", Font.PLAIN, 12));
        headerCard.add(lblDetailFrom, gbc);

        gbc.gridx = 1; gbc.gridy = 1; gbc.weightx = 0.5;
        lblDetailDate = new JLabel("Ngày gửi: -");
        lblDetailDate.setFont(new Font("SansSerif", Font.PLAIN, 12));
        headerCard.add(lblDetailDate, gbc);

        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2;
        lblDetailTo = new JLabel("Người nhận: -");
        lblDetailTo.setFont(new Font("SansSerif", Font.PLAIN, 12));
        headerCard.add(lblDetailTo, gbc);

        viewPanel.add(headerCard, BorderLayout.NORTH);

        txtDetailBody = new JTextArea();
        txtDetailBody.setEditable(false);
        txtDetailBody.setFont(new Font("SansSerif", Font.PLAIN, 13));
        txtDetailBody.setLineWrap(true);
        txtDetailBody.setWrapStyleWord(true);
        JScrollPane scrollBody = new JScrollPane(txtDetailBody);
        viewPanel.add(scrollBody, BorderLayout.CENTER);

        pnlAttachments = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        pnlAttachments.setBorder(BorderFactory.createTitledBorder("Tệp đính kèm"));
        pnlAttachments.setVisible(false);
        viewPanel.add(pnlAttachments, BorderLayout.SOUTH);

        tabbedPane.addTab("Nội dung thư", viewPanel);

        // Tab 2: Raw RFC 822
        JPanel rawPanel = new JPanel(new BorderLayout(4, 4));
        txtRawContent = new JTextArea();
        txtRawContent.setFont(new Font("Consolas", Font.PLAIN, 12));
        txtRawContent.setEditable(false);
        rawPanel.add(new JScrollPane(txtRawContent), BorderLayout.CENTER);
        tabbedPane.addTab("Mã nguồn Email (Raw RFC 822)", rawPanel);

        // Tab 3: Nhật ký socket
        logPanel = new LogPanel("Nhật ký Giao thức POP3 (Socket Log)");
        tabbedPane.addTab("Nhật ký Giao thức (Log)", logPanel);

        return tabbedPane;
    }

    private void applyPreset(int index) {
        switch (index) {
            case 0:
                txtHost.setText("pop.gmail.com");
                txtPort.setText("995");
                cboSecurity.setSelectedItem(SecurityType.SSL_TLS);
                if (chkRecentMode != null) chkRecentMode.setSelected(true);
                break;
            case 1:
                txtHost.setText("outlook.office365.com");
                txtPort.setText("995");
                cboSecurity.setSelectedItem(SecurityType.SSL_TLS);
                if (chkRecentMode != null) chkRecentMode.setSelected(false);
                break;
            case 2:
                txtHost.setText("pop.mail.yahoo.com");
                txtPort.setText("995");
                cboSecurity.setSelectedItem(SecurityType.SSL_TLS);
                if (chkRecentMode != null) chkRecentMode.setSelected(false);
                break;
            case 3:
                txtHost.setText("localhost");
                txtPort.setText("110");
                cboSecurity.setSelectedItem(SecurityType.NONE);
                if (chkRecentMode != null) chkRecentMode.setSelected(false);
                break;
            default:
                break;
        }
    }

    private ServerConfig buildConfigFromInputs() throws Exception {
        String host = txtHost.getText().trim();
        if (host.isEmpty()) throw new Exception("Vui lòng nhập địa chỉ máy chủ POP3.");

        int port;
        try {
            port = Integer.parseInt(txtPort.getText().trim());
        } catch (NumberFormatException e) {
            throw new Exception("Cổng máy chủ không hợp lệ.");
        }

        SecurityType secType = (SecurityType) cboSecurity.getSelectedItem();
        String username = txtUsername.getText().trim();
        String password = new String(txtPassword.getPassword());

        if (username.isEmpty()) throw new Exception("Vui lòng nhập tài khoản email.");
        if (password.isEmpty()) throw new Exception("Vui lòng nhập mật khẩu hoặc App Password.");

        // Nếu bật chế độ Recent Mode và là Gmail: thêm tiền tố recent: để lấy 30 ngày gần đây
        if (chkRecentMode != null && chkRecentMode.isSelected() && !username.startsWith("recent:") && host.toLowerCase().contains("gmail")) {
            username = "recent:" + username;
        }

        return new ServerConfig(host, port, username, password, secType);
    }

    private void connectAndFetchList() {
        try {
            ServerConfig config = buildConfigFromInputs();
            this.activeConfig = config;

            btnConnect.setEnabled(false);
            btnConnect.setText("Đang kết nối...");
            lblStatus.setText("Đang kết nối tới máy chủ POP3...");

            if (chkRecentMode != null && chkRecentMode.isSelected() && config.getHost().toLowerCase().contains("gmail")) {
                logPanel.logInfo("Chế độ Gmail Recent Mode: Tải các thư mới nhất trong 30 ngày qua.");
            }

            SwingWorker<List<Pop3MessageInfo>, Void> worker = new SwingWorker<>() {
                @Override
                protected List<Pop3MessageInfo> doInBackground() throws Exception {
                    if (activeClient != null) {
                        try { activeClient.close(); } catch (Exception ignored) {}
                    }
                    activeClient = new Pop3Client(config, logPanel);
                    activeClient.connect();
                    activeClient.login(config.getUsername(), config.getPassword());
                    return activeClient.list();
                }

                @Override
                protected void done() {
                    btnConnect.setEnabled(true);
                    btnConnect.setText("Kết nối & Tải thư (LIST)");
                    try {
                        List<Pop3MessageInfo> list = get();
                        setConnectedState(true);

                        allMessages.clear();
                        allMessages.addAll(list);
                        currentPage = 1;
                        applySortingAndRender();

                        lblStatus.setText("Đã kết nối (" + allMessages.size() + " thư trong hộp thư)");
                    } catch (Exception ex) {
                        setConnectedState(false);
                        Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                        logPanel.logError("Kết nối thất bại: " + cause.getMessage());
                        lblStatus.setText("Lỗi kết nối");
                        JOptionPane.showMessageDialog(Pop3Panel.this,
                                "Không thể kết nối hoặc đăng nhập POP3:\n" + cause.getMessage(),
                                "Lỗi POP3", JOptionPane.ERROR_MESSAGE);
                    }
                }
            };

            worker.execute();

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Thông tin chưa hợp lệ", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void fetchMailList() {
        if (activeClient == null || !activeClient.isConnected()) {
            connectAndFetchList();
            return;
        }

        lblStatus.setText("Đang làm mới danh sách thư...");
        SwingWorker<List<Pop3MessageInfo>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<Pop3MessageInfo> doInBackground() throws Exception {
                return activeClient.list();
            }

            @Override
            protected void done() {
                try {
                    List<Pop3MessageInfo> list = get();
                    allMessages.clear();
                    allMessages.addAll(list);
                    applySortingAndRender();
                    lblStatus.setText("Làm mới thành công (" + allMessages.size() + " thư)");
                } catch (Exception ex) {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    logPanel.logError("Làm mới danh sách thất bại: " + cause.getMessage());
                    lblStatus.setText("Lỗi làm mới");
                }
            }
        };
        worker.execute();
    }

    private void applySortingAndRender() {
        if (sortNewestFirst) {
            // Đảo ngược: Thư có số ID lớn nhất (mới nhất hôm nay) lên đầu
            allMessages.sort((a, b) -> Integer.compare(b.getMessageNumber(), a.getMessageNumber()));
        } else {
            // Thứ tự tự nhiên: Thư cũ nhất lên đầu
            allMessages.sort((a, b) -> Integer.compare(a.getMessageNumber(), b.getMessageNumber()));
        }
        renderCurrentPage();
    }

    private int getTotalPages() {
        if (allMessages.isEmpty()) return 1;
        return (int) Math.ceil((double) allMessages.size() / pageSize);
    }

    private void goToPage(int page) {
        int maxPages = getTotalPages();
        if (page < 1) page = 1;
        if (page > maxPages) page = maxPages;
        this.currentPage = page;
        renderCurrentPage();
    }

    private void updatePaginationControls() {
        int total = getTotalPages();
        btnFirstPage.setEnabled(currentPage > 1);
        btnPrevPage.setEnabled(currentPage > 1);
        btnNextPage.setEnabled(currentPage < total);
        btnLastPage.setEnabled(currentPage < total);

        lblPageInfo.setText("Trang " + (allMessages.isEmpty() ? 0 : currentPage) + " / " + (allMessages.isEmpty() ? 0 : total) + " (Tổng " + allMessages.size() + " thư)");
    }

    private void renderCurrentPage() {
        // Hủy worker nạp header của trang trước nếu đang chạy
        if (currentHeaderWorker != null && !currentHeaderWorker.isDone()) {
            currentHeaderWorker.cancel(true);
        }

        tableModel.setRowCount(0);
        clearDetailView();
        updatePaginationControls();

        if (allMessages.isEmpty()) {
            return;
        }

        int start = (currentPage - 1) * pageSize;
        int end = Math.min(start + pageSize, allMessages.size());
        List<Pop3MessageInfo> pageItems = allMessages.subList(start, end);

        // Hiển thị các dòng ban đầu lên bảng
        for (Pop3MessageInfo info : pageItems) {
            int msgNum = info.getMessageNumber();
            if (emailCache.containsKey(msgNum)) {
                EmailMessage cached = emailCache.get(msgNum);
                tableModel.addRow(new Object[]{
                        msgNum,
                        cached.getFrom(),
                        cached.getSubject(),
                        cached.getDate(),
                        info.getFormattedSize()
                });
            } else {
                tableModel.addRow(new Object[]{
                        msgNum,
                        "Đang nạp...",
                        "Đang nạp tiêu đề...",
                        "-",
                        info.getFormattedSize()
                });
            }
        }

        // Tự động tải trước Header (From, Subject, Date) cho các thư của trang này qua lệnh TOP <id> 0
        currentHeaderWorker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                for (Pop3MessageInfo info : pageItems) {
                    if (isCancelled()) break;
                    int msgNum = info.getMessageNumber();
                    if (!emailCache.containsKey(msgNum)) {
                        try {
                            // Lệnh TOP msgNum 0 chỉ tải header (0 dòng thân thư), cực nhanh
                            String headerRaw = activeClient.top(msgNum, 0);
                            EmailMessage parsed = EmailParser.parse(headerRaw, msgNum, info.getOctets());
                            emailCache.put(msgNum, parsed);
                            publish(new Object[]{msgNum, parsed.getFrom(), parsed.getSubject(), parsed.getDate()});
                        } catch (Exception ignored) {
                            // Nếu lệnh TOP lỗi, cho phép tải khi bấm vào
                        }
                    }
                }
                return null;
            }

            @Override
            protected void process(List<Object[]> chunks) {
                for (Object[] chunk : chunks) {
                    int targetNum = (int) chunk[0];
                    String from = (String) chunk[1];
                    String subject = (String) chunk[2];
                    String date = (String) chunk[3];

                    for (int i = 0; i < tableModel.getRowCount(); i++) {
                        if ((int) tableModel.getValueAt(i, 0) == targetNum) {
                            tableModel.setValueAt(from, i, 1);
                            tableModel.setValueAt(subject, i, 2);
                            tableModel.setValueAt(date, i, 3);
                            break;
                        }
                    }
                }
            }
        };

        currentHeaderWorker.execute();
    }

    private void loadMailDetail(int messageNumber) {
        EmailMessage cached = emailCache.get(messageNumber);
        // Nếu đã có thân thư đầy đủ trong cache thì hiển thị ngay
        if (cached != null && cached.getRawContent() != null && cached.getRawContent().length() > 500) {
            displayEmail(cached);
            return;
        }

        lblStatus.setText("Đang tải toàn bộ thư #" + messageNumber + "...");
        SwingWorker<EmailMessage, Void> worker = new SwingWorker<>() {
            @Override
            protected EmailMessage doInBackground() throws Exception {
                String raw = activeClient.retr(messageNumber);
                return EmailParser.parse(raw, messageNumber, raw.length());
            }

            @Override
            protected void done() {
                try {
                    EmailMessage email = get();
                    emailCache.put(messageNumber, email);
                    displayEmail(email);

                    // Cập nhật lại thông tin trên bảng nếu trước đó chưa cập nhật
                    for (int i = 0; i < tableModel.getRowCount(); i++) {
                        if ((int) tableModel.getValueAt(i, 0) == messageNumber) {
                            tableModel.setValueAt(email.getFrom(), i, 1);
                            tableModel.setValueAt(email.getSubject(), i, 2);
                            tableModel.setValueAt(email.getDate(), i, 3);
                            break;
                        }
                    }
                    lblStatus.setText("Đã tải xong thư #" + messageNumber);
                } catch (Exception ex) {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    logPanel.logError("Tải thư #" + messageNumber + " thất bại: " + cause.getMessage());
                    lblStatus.setText("Lỗi khi tải thư #" + messageNumber);
                }
            }
        };
        worker.execute();
    }

    private void displayEmail(EmailMessage email) {
        lblDetailSubject.setText(email.getSubject());
        lblDetailFrom.setText("Người gửi: " + email.getFrom());
        lblDetailDate.setText("Ngày: " + email.getDate());
        lblDetailTo.setText("Người nhận: " + String.join(", ", email.getTo()));

        txtDetailBody.setText(email.getBody());
        txtDetailBody.setCaretPosition(0);

        txtRawContent.setText(email.getRawContent());
        txtRawContent.setCaretPosition(0);

        pnlAttachments.removeAll();
        List<EmailAttachment> attachments = email.getAttachments();
        if (attachments != null && !attachments.isEmpty()) {
            pnlAttachments.setVisible(true);
            for (EmailAttachment att : attachments) {
                JButton btnAtt = new JButton("💾 " + att.getFileName() + " (" + att.getFormattedSize() + ")");
                btnAtt.setToolTipText("Nhấn để tải về máy tính");
                btnAtt.addActionListener(e -> saveAttachment(att));
                pnlAttachments.add(btnAtt);
            }
        } else {
            pnlAttachments.setVisible(false);
        }

        pnlAttachments.revalidate();
        pnlAttachments.repaint();
    }

    private void saveAttachment(EmailAttachment att) {
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File(att.getFileName()));
        int res = chooser.showSaveDialog(this);
        if (res == JFileChooser.APPROVE_OPTION) {
            File targetFile = chooser.getSelectedFile();
            try (FileOutputStream fos = new FileOutputStream(targetFile)) {
                fos.write(att.getData());
                JOptionPane.showMessageDialog(this, "Đã lưu tệp đính kèm thành công:\n" + targetFile.getAbsolutePath(),
                        "Thành công", JOptionPane.INFORMATION_MESSAGE);
                logPanel.logInfo("Đã lưu tệp đính kèm: " + targetFile.getName());
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Không thể lưu tệp: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void executeStat() {
        if (activeClient == null || !activeClient.isConnected()) return;

        SwingWorker<Pop3Stat, Void> worker = new SwingWorker<>() {
            @Override
            protected Pop3Stat doInBackground() throws Exception {
                return activeClient.stat();
            }

            @Override
            protected void done() {
                try {
                    Pop3Stat stat = get();
                    JOptionPane.showMessageDialog(Pop3Panel.this,
                            "Thống kê Hộp thư (STAT):\n" +
                                    "- Số lượng thư: " + stat.getMessageCount() + "\n" +
                                    "- Tổng dung lượng: " + stat.getFormattedSize(),
                            "Thông tin STAT", JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(Pop3Panel.this, "Lỗi lệnh STAT: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private void deleteSelectedMail() {
        int selectedRow = tblMessages.getSelectedRow();
        if (selectedRow == -1 || activeClient == null) return;

        int msgNum = (int) tableModel.getValueAt(selectedRow, 0);

        int confirm = JOptionPane.showConfirmDialog(this,
                "Bạn có chắc muốn đánh dấu xóa thư #" + msgNum + "?\n(Thư sẽ bị xóa vĩnh viễn trên máy chủ khi bạn Ngắt kết nối / QUIT)",
                "Xác nhận xóa thư", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            SwingWorker<Void, Void> worker = new SwingWorker<>() {
                @Override
                protected Void doInBackground() throws Exception {
                    activeClient.dele(msgNum);
                    return null;
                }

                @Override
                protected void done() {
                    try {
                        get();
                        // Xóa khỏi danh sách allMessages
                        allMessages.removeIf(m -> m.getMessageNumber() == msgNum);
                        emailCache.remove(msgNum);
                        renderCurrentPage();
                        btnDelete.setEnabled(false);
                        JOptionPane.showMessageDialog(Pop3Panel.this, "Đã đánh dấu xóa thư #" + msgNum + " thành công.");
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(Pop3Panel.this, "Lỗi khi xóa thư: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
                    }
                }
            };
            worker.execute();
        }
    }

    private void disconnect() {
        if (currentHeaderWorker != null && !currentHeaderWorker.isDone()) {
            currentHeaderWorker.cancel(true);
        }
        if (activeClient != null) {
            try {
                activeClient.quit();
            } catch (Exception ignored) {}
            activeClient = null;
        }
        setConnectedState(false);
        lblStatus.setText("Đã ngắt kết nối.");
    }

    private void setConnectedState(boolean connected) {
        btnDisconnect.setEnabled(connected);
        btnRefresh.setEnabled(connected);
        btnStat.setEnabled(connected);
        btnDelete.setEnabled(connected && tblMessages.getSelectedRow() != -1);
    }

    private void clearDetailView() {
        lblDetailSubject.setText("(Chưa chọn thư)");
        lblDetailFrom.setText("Người gửi: -");
        lblDetailTo.setText("Người nhận: -");
        lblDetailDate.setText("Ngày: -");
        txtDetailBody.setText("");
        txtRawContent.setText("");
        pnlAttachments.removeAll();
        pnlAttachments.setVisible(false);
    }
}
