package com.mailclient.ui;

import com.mailclient.model.EmailAttachment;
import com.mailclient.model.EmailMessage;
import com.mailclient.model.SecurityType;
import com.mailclient.model.ServerConfig;
import com.mailclient.protocol.smtp.SmtpClient;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Giao diện SMTP Client: Cấu hình máy chủ, soạn thảo thư, đính kèm tệp,
 * xem trước MIME và gửi thư qua Socket TCP/SSL.
 */
public class SmtpPanel extends JPanel {
    // Cấu hình máy chủ
    private JComboBox<String> cboPresets;
    private JTextField txtHost;
    private JTextField txtPort;
    private JComboBox<SecurityType> cboSecurity;
    private JTextField txtUsername;
    private JPasswordField txtPassword;
    private JCheckBox chkShowPassword;

    // Soạn thư
    private JTextField txtFrom;
    private JTextField txtTo;
    private JTextField txtCc;
    private JTextField txtBcc;
    private JTextField txtSubject;
    private JTextArea txtBody;

    // Tệp đính kèm
    private DefaultListModel<EmailAttachment> attachmentListModel;
    private JList<EmailAttachment> listAttachments;
    private JLabel lblAttachmentSummary;

    // Nút chức năng & Nhật ký
    private JButton btnSend;
    private JButton btnPreviewMime;
    private JButton btnClear;
    private LogPanel logPanel;

    public SmtpPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(12, 12, 12, 12));

        initUI();
    }

    private void initUI() {
        // Chia giao diện làm 2 phần trái - phải:
        // Bên trái: Cấu hình máy chủ + Soạn thảo thư
        // Bên phải: Nhật ký giao tiếp giao thức SMTP (Protocol Log)
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setResizeWeight(0.55);
        splitPane.setContinuousLayout(true);

        JPanel leftPanel = new JPanel(new BorderLayout(8, 8));
        leftPanel.add(createServerConfigPanel(), BorderLayout.NORTH);
        leftPanel.add(createComposerPanel(), BorderLayout.CENTER);
        leftPanel.add(createActionBar(), BorderLayout.SOUTH);

        logPanel = new LogPanel("Nhật ký Giao thức SMTP (Socket Log)");

        splitPane.setLeftComponent(leftPanel);
        splitPane.setRightComponent(logPanel);

        add(splitPane, BorderLayout.CENTER);

        // Áp dụng cấu hình mẫu mặc định (Gmail 465 SSL)
        applyPreset(0);
    }

    private JPanel createServerConfigPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(200, 205, 215)),
                " 1. Cấu hình Máy chủ SMTP ",
                TitledBorder.LEFT,
                TitledBorder.TOP,
                new Font("SansSerif", Font.BOLD, 12),
                new Color(30, 41, 59)
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 6, 4, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Dòng 0: Cấu hình mẫu (Presets)
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.0;
        panel.add(new JLabel("Mẫu cấu hình:"), gbc);

        cboPresets = new JComboBox<>(new String[]{
                "Gmail (smtp.gmail.com:465 - SSL/TLS)",
                "Gmail (smtp.gmail.com:587 - STARTTLS)",
                "Outlook / Office 365 (smtp-mail.outlook.com:587 - STARTTLS)",
                "Yahoo Mail (smtp.mail.yahoo.com:465 - SSL/TLS)",
                "Mailtrap Sandbox (sandbox.smtp.mailtrap.io:2525)",
                "Localhost (localhost:25 - Plain TCP)",
                "Localhost MailHog / Papercut (localhost:1025)",
                "Tùy chỉnh (Custom)..."
        });
        cboPresets.addActionListener(e -> applyPreset(cboPresets.getSelectedIndex()));
        gbc.gridx = 1; gbc.gridy = 0; gbc.gridwidth = 3; gbc.weightx = 1.0;
        panel.add(cboPresets, gbc);

        // Dòng 1: Máy chủ (Host) và Cổng (Port)
        gbc.gridwidth = 1;
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0.0;
        panel.add(new JLabel("Máy chủ (Host):"), gbc);

        txtHost = new JTextField("smtp.gmail.com", 18);
        gbc.gridx = 1; gbc.gridy = 1; gbc.weightx = 0.6;
        panel.add(txtHost, gbc);

        gbc.gridx = 2; gbc.gridy = 1; gbc.weightx = 0.0;
        panel.add(new JLabel("Cổng (Port):"), gbc);

        txtPort = new JTextField("465", 6);
        gbc.gridx = 3; gbc.gridy = 1; gbc.weightx = 0.4;
        panel.add(txtPort, gbc);

        // Dòng 2: Loại bảo mật
        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0.0;
        panel.add(new JLabel("Bảo mật:"), gbc);

        cboSecurity = new JComboBox<>(SecurityType.values());
        cboSecurity.setSelectedItem(SecurityType.SSL_TLS);
        gbc.gridx = 1; gbc.gridy = 2; gbc.gridwidth = 3; gbc.weightx = 1.0;
        panel.add(cboSecurity, gbc);

        // Dòng 3: Tài khoản (Username)
        gbc.gridwidth = 1;
        gbc.gridx = 0; gbc.gridy = 3; gbc.weightx = 0.0;
        panel.add(new JLabel("Tài khoản (Email):"), gbc);

        txtUsername = new JTextField("", 18);
        txtUsername.addCaretListener(e -> {
            if (txtFrom.getText().isEmpty() || txtFrom.getText().equals(txtUsername.getText())) {
                txtFrom.setText(txtUsername.getText());
            }
        });
        gbc.gridx = 1; gbc.gridy = 3; gbc.gridwidth = 3; gbc.weightx = 1.0;
        panel.add(txtUsername, gbc);

        // Dòng 4: Mật khẩu (Password)
        gbc.gridwidth = 1;
        gbc.gridx = 0; gbc.gridy = 4; gbc.weightx = 0.0;
        panel.add(new JLabel("Mật khẩu:"), gbc);

        txtPassword = new JPasswordField("", 18);
        gbc.gridx = 1; gbc.gridy = 4; gbc.weightx = 0.6;
        panel.add(txtPassword, gbc);

        chkShowPassword = new JCheckBox("Hiện");
        chkShowPassword.addActionListener(e -> {
            txtPassword.setEchoChar(chkShowPassword.isSelected() ? (char) 0 : '•');
        });
        gbc.gridx = 2; gbc.gridy = 4; gbc.gridwidth = 2; gbc.weightx = 0.4;
        panel.add(chkShowPassword, gbc);

        return panel;
    }

    private JPanel createComposerPanel() {
        JPanel panel = new JPanel(new BorderLayout(6, 6));
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(200, 205, 215)),
                " 2. Soạn Thảo Thư Điện Tử ",
                TitledBorder.LEFT,
                TitledBorder.TOP,
                new Font("SansSerif", Font.BOLD, 12),
                new Color(30, 41, 59)
        ));

        // Phần Header thư (From, To, Cc, Bcc, Subject)
        JPanel fieldsPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(3, 4, 3, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Từ (From)
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.0;
        fieldsPanel.add(new JLabel("Từ (From):"), gbc);
        txtFrom = new JTextField();
        gbc.gridx = 1; gbc.gridy = 0; gbc.weightx = 1.0;
        fieldsPanel.add(txtFrom, gbc);

        // Đến (To)
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0.0;
        fieldsPanel.add(new JLabel("Đến (To):"), gbc);
        txtTo = new JTextField();
        txtTo.setToolTipText("Nhiều người nhận cách nhau bằng dấu phẩy");
        gbc.gridx = 1; gbc.gridy = 1; gbc.weightx = 1.0;
        fieldsPanel.add(txtTo, gbc);

        // Cc & Bcc
        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0.0;
        fieldsPanel.add(new JLabel("Cc:"), gbc);
        txtCc = new JTextField();
        gbc.gridx = 1; gbc.gridy = 2; gbc.weightx = 1.0;
        fieldsPanel.add(txtCc, gbc);

        gbc.gridx = 0; gbc.gridy = 3; gbc.weightx = 0.0;
        fieldsPanel.add(new JLabel("Bcc:"), gbc);
        txtBcc = new JTextField();
        gbc.gridx = 1; gbc.gridy = 3; gbc.weightx = 1.0;
        fieldsPanel.add(txtBcc, gbc);

        // Tiêu đề (Subject)
        gbc.gridx = 0; gbc.gridy = 4; gbc.weightx = 0.0;
        fieldsPanel.add(new JLabel("Tiêu đề:"), gbc);
        txtSubject = new JTextField();
        gbc.gridx = 1; gbc.gridy = 4; gbc.weightx = 1.0;
        fieldsPanel.add(txtSubject, gbc);

        panel.add(fieldsPanel, BorderLayout.NORTH);

        // Thân thư (Body) & Tệp đính kèm (Attachments)
        JPanel centerPanel = new JPanel(new BorderLayout(4, 4));

        txtBody = new JTextArea(8, 20);
        txtBody.setFont(new Font("SansSerif", Font.PLAIN, 13));
        txtBody.setLineWrap(true);
        txtBody.setWrapStyleWord(true);
        JScrollPane scrollBody = new JScrollPane(txtBody);
        scrollBody.setBorder(BorderFactory.createTitledBorder("Nội dung thư"));
        centerPanel.add(scrollBody, BorderLayout.CENTER);

        // Quản lý tệp đính kèm
        JPanel attachPanel = new JPanel(new BorderLayout(4, 4));
        attachPanel.setBorder(BorderFactory.createTitledBorder("Tệp đính kèm (MIME Multipart)"));
        attachPanel.setPreferredSize(new Dimension(100, 100));

        attachmentListModel = new DefaultListModel<>();
        listAttachments = new JList<>(attachmentListModel);
        listAttachments.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane scrollAttach = new JScrollPane(listAttachments);
        attachPanel.add(scrollAttach, BorderLayout.CENTER);

        JPanel attachBtnBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        JButton btnAddAttach = new JButton("Thêm tệp (+)");
        btnAddAttach.setFocusable(false);
        btnAddAttach.addActionListener(e -> chooseAttachment());

        JButton btnRemoveAttach = new JButton("Xóa tệp (-)");
        btnRemoveAttach.setFocusable(false);
        btnRemoveAttach.addActionListener(e -> removeAttachment());

        lblAttachmentSummary = new JLabel("0 tệp đính kèm");
        lblAttachmentSummary.setForeground(new Color(100, 116, 139));

        attachBtnBar.add(btnAddAttach);
        attachBtnBar.add(btnRemoveAttach);
        attachBtnBar.add(Box.createHorizontalStrut(10));
        attachBtnBar.add(lblAttachmentSummary);

        attachPanel.add(attachBtnBar, BorderLayout.SOUTH);
        centerPanel.add(attachPanel, BorderLayout.SOUTH);

        panel.add(centerPanel, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createActionBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 6));

        btnPreviewMime = new JButton("Xem trước MIME");
        btnPreviewMime.setFocusable(false);
        btnPreviewMime.addActionListener(e -> previewMime());

        btnClear = new JButton("Xóa trắng");
        btnClear.setFocusable(false);
        btnClear.addActionListener(e -> clearForm());

        btnSend = new JButton("Gửi Email (SMTP)");
        btnSend.setFont(new Font("SansSerif", Font.BOLD, 13));
        btnSend.setBackground(new Color(37, 99, 235));
        btnSend.setForeground(Color.WHITE);
        btnSend.setFocusable(false);
        btnSend.addActionListener(e -> sendEmailAsync());

        bar.add(btnPreviewMime);
        bar.add(btnClear);
        bar.add(btnSend);

        return bar;
    }

    private void applyPreset(int index) {
        switch (index) {
            case 0: // Gmail SSL 465
                txtHost.setText("smtp.gmail.com");
                txtPort.setText("465");
                cboSecurity.setSelectedItem(SecurityType.SSL_TLS);
                break;
            case 1: // Gmail STARTTLS 587
                txtHost.setText("smtp.gmail.com");
                txtPort.setText("587");
                cboSecurity.setSelectedItem(SecurityType.STARTTLS);
                break;
            case 2: // Outlook 587
                txtHost.setText("smtp-mail.outlook.com");
                txtPort.setText("587");
                cboSecurity.setSelectedItem(SecurityType.STARTTLS);
                break;
            case 3: // Yahoo 465
                txtHost.setText("smtp.mail.yahoo.com");
                txtPort.setText("465");
                cboSecurity.setSelectedItem(SecurityType.SSL_TLS);
                break;
            case 4: // Mailtrap 2525
                txtHost.setText("sandbox.smtp.mailtrap.io");
                txtPort.setText("2525");
                cboSecurity.setSelectedItem(SecurityType.NONE);
                break;
            case 5: // Localhost 25
                txtHost.setText("localhost");
                txtPort.setText("25");
                cboSecurity.setSelectedItem(SecurityType.NONE);
                break;
            case 6: // Localhost MailHog 1025
                txtHost.setText("localhost");
                txtPort.setText("1025");
                cboSecurity.setSelectedItem(SecurityType.NONE);
                break;
            default:
                break;
        }
    }

    private void chooseAttachment() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Chọn tệp đính kèm");
        int res = chooser.showOpenDialog(this);
        if (res == JFileChooser.APPROVE_OPTION) {
            File selectedFile = chooser.getSelectedFile();
            try {
                EmailAttachment attachment = EmailAttachment.fromFile(selectedFile);
                attachmentListModel.addElement(attachment);
                updateAttachmentSummary();
                logPanel.logInfo("Đã thêm tệp đính kèm: " + attachment);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Không thể đọc tệp: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void removeAttachment() {
        int idx = listAttachments.getSelectedIndex();
        if (idx != -1) {
            EmailAttachment att = attachmentListModel.remove(idx);
            updateAttachmentSummary();
            logPanel.logInfo("Đã bỏ tệp đính kèm: " + att.getFileName());
        }
    }

    private void updateAttachmentSummary() {
        int count = attachmentListModel.size();
        long totalSize = 0;
        for (int i = 0; i < count; i++) {
            totalSize += attachmentListModel.get(i).getSize();
        }
        String sizeStr;
        if (totalSize < 1024) sizeStr = totalSize + " B";
        else {
            int exp = (int) (Math.log(totalSize) / Math.log(1024));
            char pre = "KMGTPE".charAt(exp - 1);
            sizeStr = String.format("%.1f %sB", totalSize / Math.pow(1024, exp), pre);
        }
        lblAttachmentSummary.setText(count + " tệp (" + sizeStr + ")");
    }

    private EmailMessage buildEmailMessageFromInputs() throws Exception {
        String from = txtFrom.getText().trim();
        if (from.isEmpty()) from = txtUsername.getText().trim();
        if (from.isEmpty()) throw new Exception("Vui lòng nhập địa chỉ người gửi (From) hoặc tài khoản đăng nhập.");

        String toText = txtTo.getText().trim();
        if (toText.isEmpty()) throw new Exception("Vui lòng nhập ít nhất một địa chỉ người nhận (To).");

        EmailMessage message = new EmailMessage();
        message.setFrom(from);

        for (String t : toText.split(",")) {
            if (!t.trim().isEmpty()) message.addTo(t.trim());
        }

        String ccText = txtCc.getText().trim();
        if (!ccText.isEmpty()) {
            for (String c : ccText.split(",")) {
                if (!c.trim().isEmpty()) message.addCc(c.trim());
            }
        }

        String bccText = txtBcc.getText().trim();
        if (!bccText.isEmpty()) {
            for (String b : bccText.split(",")) {
                if (!b.trim().isEmpty()) message.addBcc(b.trim());
            }
        }

        message.setSubject(txtSubject.getText());
        message.setBody(txtBody.getText());

        List<EmailAttachment> attachments = new ArrayList<>();
        for (int i = 0; i < attachmentListModel.size(); i++) {
            attachments.add(attachmentListModel.get(i));
        }
        message.setAttachments(attachments);

        return message;
    }

    private void previewMime() {
        try {
            EmailMessage msg = buildEmailMessageFromInputs();
            String mime = SmtpClient.buildMimePayload(msg, msg.getFrom());

            JTextArea area = new JTextArea(mime, 22, 60);
            area.setFont(new Font("Consolas", Font.PLAIN, 12));
            area.setEditable(false);
            area.setCaretPosition(0);

            JScrollPane scroll = new JScrollPane(area);
            JOptionPane.showMessageDialog(this, scroll, "Xem trước MIME (RFC 2822 / RFC 2045)", JOptionPane.PLAIN_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Lỗi", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void clearForm() {
        txtTo.setText("");
        txtCc.setText("");
        txtBcc.setText("");
        txtSubject.setText("");
        txtBody.setText("");
        attachmentListModel.clear();
        updateAttachmentSummary();
        logPanel.logInfo("Đã xóa trắng biểu mẫu soạn thư.");
    }

    private void sendEmailAsync() {
        try {
            EmailMessage message = buildEmailMessageFromInputs();

            String host = txtHost.getText().trim();
            if (host.isEmpty()) throw new Exception("Vui lòng nhập địa chỉ máy chủ SMTP.");

            int port;
            try {
                port = Integer.parseInt(txtPort.getText().trim());
            } catch (NumberFormatException e) {
                throw new Exception("Cổng máy chủ không hợp lệ.");
            }

            SecurityType secType = (SecurityType) cboSecurity.getSelectedItem();
            String username = txtUsername.getText().trim();
            String password = new String(txtPassword.getPassword());

            ServerConfig config = new ServerConfig(host, port, username, password, secType);

            btnSend.setEnabled(false);
            btnSend.setText("Đang gửi...");

            SwingWorker<Void, Void> worker = new SwingWorker<>() {
                @Override
                protected Void doInBackground() throws Exception {
                    try (SmtpClient client = new SmtpClient(config, logPanel)) {
                        client.connect();
                        client.sendEmail(message);
                        client.quit();
                    }
                    return null;
                }

                @Override
                protected void done() {
                    btnSend.setEnabled(true);
                    btnSend.setText("Gửi Email (SMTP)");
                    try {
                        get();
                        JOptionPane.showMessageDialog(SmtpPanel.this,
                                "Gửi thư thành công qua giao thức SMTP!",
                                "Thành công", JOptionPane.INFORMATION_MESSAGE);
                    } catch (Exception ex) {
                        Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                        logPanel.logError("Gửi thư thất bại: " + cause.getMessage());
                        JOptionPane.showMessageDialog(SmtpPanel.this,
                                "Gửi thư thất bại:\n" + cause.getMessage(),
                                "Lỗi gửi thư", JOptionPane.ERROR_MESSAGE);
                    }
                }
            };

            worker.execute();

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Thông tin chưa hợp lệ", JOptionPane.WARNING_MESSAGE);
        }
    }
}
