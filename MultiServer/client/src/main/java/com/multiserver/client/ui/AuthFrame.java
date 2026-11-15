package com.multiserver.client.ui;

import com.multiserver.client.service.ClientBankingService;
import com.multiserver.common.model.Response;
import com.multiserver.common.model.User;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Giao diện Đăng Nhập và Đăng Ký phía Khách Hàng (Theme Sáng):
 * Giao diện ngân hàng chuẩn, bảo mật, không để lộ thông tin hạ tầng/server.
 */
public class AuthFrame extends JFrame {

    private final ClientBankingService service;

    // Login fields
    private JTextField txtLoginEmail;
    private JPasswordField txtLoginPassword;

    // Register fields
    private JTextField txtRegName;
    private JTextField txtRegEmail;
    private JPasswordField txtRegPassword;
    private JPasswordField txtRegConfirm;

    public AuthFrame(ClientBankingService service) {
        this.service = service;
        initComponents();
    }

    private void initComponents() {
        setTitle("HỆ THỐNG NGÂN HÀNG ĐIỆN TỬ - ĐĂNG NHẬP & ĐĂNG KÝ");
        setSize(460, 530);
        setMinimumSize(new Dimension(420, 500));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBackground(new Color(248, 249, 250));
        mainPanel.setBorder(new EmptyBorder(20, 25, 15, 25));

        // 1. Top Logo & Title
        JPanel topPanel = new JPanel(new GridLayout(2, 1, 4, 4));
        topPanel.setBackground(new Color(248, 249, 250));

        JLabel lblAppTitle = new JLabel("NGÂN HÀNG ĐIỆN TỬ", SwingConstants.CENTER);
        lblAppTitle.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lblAppTitle.setForeground(new Color(13, 110, 253));

        JLabel lblSubTitle = new JLabel("Dịch vụ ngân hàng số an toàn và tiện lợi", SwingConstants.CENTER);
        lblSubTitle.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblSubTitle.setForeground(new Color(108, 117, 125));

        topPanel.add(lblAppTitle);
        topPanel.add(lblSubTitle);
        mainPanel.add(topPanel, BorderLayout.NORTH);

        // 2. Center Tabs: Đăng Nhập & Đăng Ký
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Segoe UI", Font.BOLD, 13));
        tabbedPane.addTab("  Đăng Nhập  ", createLoginPanel());
        tabbedPane.addTab("  Đăng Ký  ", createRegisterPanel());
        mainPanel.add(tabbedPane, BorderLayout.CENTER);

        // 3. Bottom Security Footer
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        bottomPanel.setBackground(new Color(248, 249, 250));
        bottomPanel.setBorder(new EmptyBorder(8, 0, 0, 0));

        JLabel lblSecure = new JLabel("🔒 Kết nối an toàn 256-bit SSL | Hỗ trợ CSKH 24/7");
        lblSecure.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblSecure.setForeground(new Color(108, 117, 125));
        bottomPanel.add(lblSecure);

        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        setContentPane(mainPanel);
    }

    private JPanel createLoginPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(Color.WHITE);
        panel.setBorder(new EmptyBorder(25, 20, 20, 20));

        JLabel lblEmail = new JLabel("Địa chỉ Email:");
        lblEmail.setFont(new Font("Segoe UI", Font.BOLD, 12));
        txtLoginEmail = new JTextField();
        txtLoginEmail.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        txtLoginEmail.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        txtLoginEmail.putClientProperty("JTextField.placeholderText", "ví dụ: user@gmail.com");

        JLabel lblPass = new JLabel("Mật khẩu:");
        lblPass.setFont(new Font("Segoe UI", Font.BOLD, 12));
        txtLoginPassword = new JPasswordField();
        txtLoginPassword.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        txtLoginPassword.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        JButton btnLogin = new JButton("ĐĂNG NHẬP");
        btnLogin.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        btnLogin.setBackground(new Color(13, 110, 253));
        btnLogin.setForeground(Color.WHITE);
        btnLogin.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnLogin.setFocusable(false);
        btnLogin.addActionListener(e -> handleLogin());

        // Nhấn Enter để đăng nhập
        txtLoginPassword.addActionListener(e -> handleLogin());

        panel.add(lblEmail);
        panel.add(Box.createVerticalStrut(5));
        panel.add(txtLoginEmail);
        panel.add(Box.createVerticalStrut(15));
        panel.add(lblPass);
        panel.add(Box.createVerticalStrut(5));
        panel.add(txtLoginPassword);
        panel.add(Box.createVerticalStrut(25));
        panel.add(btnLogin);
        panel.add(Box.createVerticalGlue());

        return panel;
    }

    private JPanel createRegisterPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(Color.WHITE);
        panel.setBorder(new EmptyBorder(15, 20, 15, 20));

        JLabel lblName = new JLabel("Họ và tên:");
        lblName.setFont(new Font("Segoe UI", Font.BOLD, 12));
        txtRegName = new JTextField();
        txtRegName.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        txtRegName.putClientProperty("JTextField.placeholderText", "Nguyễn Văn A");

        JLabel lblEmail = new JLabel("Địa chỉ Email:");
        lblEmail.setFont(new Font("Segoe UI", Font.BOLD, 12));
        txtRegEmail = new JTextField();
        txtRegEmail.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        txtRegEmail.putClientProperty("JTextField.placeholderText", "nguyenvana@gmail.com");

        JLabel lblPass = new JLabel("Mật khẩu:");
        lblPass.setFont(new Font("Segoe UI", Font.BOLD, 12));
        txtRegPassword = new JPasswordField();
        txtRegPassword.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));

        JLabel lblConfirm = new JLabel("Xác nhận mật khẩu:");
        lblConfirm.setFont(new Font("Segoe UI", Font.BOLD, 12));
        txtRegConfirm = new JPasswordField();
        txtRegConfirm.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));

        JButton btnRegister = new JButton("ĐĂNG KÝ TÀI KHOẢN");
        btnRegister.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        btnRegister.setBackground(new Color(25, 135, 84));
        btnRegister.setForeground(Color.WHITE);
        btnRegister.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnRegister.setFocusable(false);
        btnRegister.addActionListener(e -> handleRegister());

        panel.add(lblName);
        panel.add(Box.createVerticalStrut(4));
        panel.add(txtRegName);
        panel.add(Box.createVerticalStrut(10));
        panel.add(lblEmail);
        panel.add(Box.createVerticalStrut(4));
        panel.add(txtRegEmail);
        panel.add(Box.createVerticalStrut(10));
        panel.add(lblPass);
        panel.add(Box.createVerticalStrut(4));
        panel.add(txtRegPassword);
        panel.add(Box.createVerticalStrut(10));
        panel.add(lblConfirm);
        panel.add(Box.createVerticalStrut(4));
        panel.add(txtRegConfirm);
        panel.add(Box.createVerticalStrut(18));
        panel.add(btnRegister);
        panel.add(Box.createVerticalGlue());

        return panel;
    }

    private void handleLogin() {
        String email = txtLoginEmail.getText().trim();
        String pass = new String(txtLoginPassword.getPassword()).trim();

        if (email.isEmpty() || pass.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập đầy đủ Email và Mật khẩu!", "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            Response<User> resp = service.login(email, pass);
            if (resp.isSuccess() && resp.getData() != null) {
                JOptionPane.showMessageDialog(this,
                        "Đăng nhập thành công!",
                        "Thông báo", JOptionPane.INFORMATION_MESSAGE);

                ClientDashboardFrame dashboard = new ClientDashboardFrame(service);
                dashboard.setVisible(true);
                this.dispose();
            } else {
                JOptionPane.showMessageDialog(this, resp.getMessage(), "Đăng nhập thất bại", JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Không thể kết nối đến hệ thống ngân hàng!\nVui lòng thử lại sau.",
                    "Lỗi kết nối", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleRegister() {
        String name = txtRegName.getText().trim();
        String email = txtRegEmail.getText().trim();
        String pass = new String(txtRegPassword.getPassword()).trim();
        String confirm = new String(txtRegConfirm.getPassword()).trim();

        if (name.isEmpty() || email.isEmpty() || pass.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng điền đầy đủ các trường!", "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (!pass.equals(confirm)) {
            JOptionPane.showMessageDialog(this, "Mật khẩu xác nhận không trùng khớp!", "Lỗi xác thực", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            Response<User> resp = service.register(name, email, pass);
            if (resp.isSuccess()) {
                JOptionPane.showMessageDialog(this,
                        "Đăng ký tài khoản thành công!\nBạn có thể đăng nhập ngay bây giờ.",
                        "Thành công", JOptionPane.INFORMATION_MESSAGE);
                txtLoginEmail.setText(email);
                txtLoginPassword.setText(pass);
            } else {
                JOptionPane.showMessageDialog(this, resp.getMessage(), "Đăng ký thất bại", JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Không thể kết nối đến hệ thống ngân hàng!\nVui lòng thử lại sau.",
                    "Lỗi kết nối", JOptionPane.ERROR_MESSAGE);
        }
    }
}
