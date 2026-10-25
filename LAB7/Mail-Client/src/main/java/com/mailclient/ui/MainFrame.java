package com.mailclient.ui;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.*;
import java.awt.*;

/**
 * Cửa sổ chính của ứng dụng Mail Client (SMTP & POP3).
 */
public class MainFrame extends JFrame {
    private JTabbedPane mainTabs;
    private SmtpPanel smtpPanel;
    private Pop3Panel pop3Panel;

    public MainFrame() {
        setTitle("Mail Client - Lập Trình Giao Thức Mạng SMTP & POP3 [LAB 7]");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1180, 830);
        setMinimumSize(new Dimension(950, 700));
        setLocationRelativeTo(null);

        initMenuBar();
        initUI();
    }

    private void initMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        // Menu Tệp
        JMenu mnuFile = new JMenu("Hệ thống");
        JMenuItem itemExit = new JMenuItem("Thoát chương trình");
        itemExit.addActionListener(e -> System.exit(0));
        mnuFile.add(itemExit);
        menuBar.add(mnuFile);

        // Menu Giao diện
        JMenu mnuTheme = new JMenu("Giao diện");
        JMenuItem itemLight = new JMenuItem("Chế độ Sáng (Light Theme)");
        itemLight.addActionListener(e -> switchTheme(false));
        JMenuItem itemDark = new JMenuItem("Chế độ Tối (Dark Theme)");
        itemDark.addActionListener(e -> switchTheme(true));
        mnuTheme.add(itemLight);
        mnuTheme.add(itemDark);
        menuBar.add(mnuTheme);

        setJMenuBar(menuBar);
    }

    private void initUI() {
        mainTabs = new JTabbedPane();
        mainTabs.setFont(new Font("SansSerif", Font.BOLD, 13));

        smtpPanel = new SmtpPanel();
        pop3Panel = new Pop3Panel();

        mainTabs.addTab("📤  Gửi Thư (SMTP Client)", smtpPanel);
        mainTabs.addTab("📥  Nhận & Quản Lý Thư (POP3 Client)", pop3Panel);

        add(mainTabs, BorderLayout.CENTER);

        // Thanh trạng thái phía dưới
        JPanel statusBar = new JPanel(new BorderLayout(5, 5));
        statusBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(220, 225, 230)),
                BorderFactory.createEmptyBorder(4, 12, 4, 12)
        ));

        JLabel lblLeft = new JLabel("Lập trình Mạng - Giao thức Socket: SMTP (RFC 5321) & POP3 (RFC 1939)");
        lblLeft.setFont(new Font("SansSerif", Font.PLAIN, 11));
        lblLeft.setForeground(new Color(100, 116, 139));

        JLabel lblRight = new JLabel("Hỗ trợ SSL / TLS / STARTTLS");
        lblRight.setFont(new Font("SansSerif", Font.BOLD, 11));
        lblRight.setForeground(new Color(16, 185, 129));

        statusBar.add(lblLeft, BorderLayout.WEST);
        statusBar.add(lblRight, BorderLayout.EAST);
        add(statusBar, BorderLayout.SOUTH);
    }

    private void switchTheme(boolean dark) {
        try {
            if (dark) {
                FlatDarkLaf.setup();
            } else {
                FlatLightLaf.setup();
            }
            SwingUtilities.updateComponentTreeUI(this);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Không thể chuyển giao diện: " + ex.getMessage());
        }
    }
}
