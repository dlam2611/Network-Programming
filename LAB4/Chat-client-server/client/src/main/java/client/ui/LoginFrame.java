package client.ui;

import client.controller.ChatController;
import client.model.ChatModel;
import client.net.ServerConnection;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/** Màn hình đăng nhập bằng username. */
public class LoginFrame extends JFrame {
    private final JTextField host = new JTextField("localhost", 14);
    private final JTextField port = new JTextField("5555", 6);
    private final JTextField user = new JTextField(14);

    public LoginFrame() {
        super("Chat - Đăng nhập");
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(Color.WHITE);
        p.setBorder(new EmptyBorder(24, 32, 24, 32));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 4, 6, 4);
        c.fill = GridBagConstraints.HORIZONTAL;

        JLabel title = new JLabel("💬 Chat LAN", SwingConstants.CENTER);
        title.setFont(Theme.fontFor(title.getText(), Font.BOLD, 24f));
        title.setForeground(Theme.MINE_BG);
        c.gridx = 0; c.gridy = 0; c.gridwidth = 2;
        p.add(title, c);

        c.gridwidth = 1;
        addRow(p, c, 1, "IP server:", host);
        addRow(p, c, 2, "Cổng:", port);
        addRow(p, c, 3, "Username:", user);

        JButton btn = new JButton("Đăng nhập");
        btn.setBackground(Theme.MINE_BG);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.addActionListener(e -> login());
        c.gridx = 0; c.gridy = 4; c.gridwidth = 2;
        p.add(btn, c);

        getRootPane().setDefaultButton(btn);
        setContentPane(p);
        pack();
        setResizable(false);
        setLocationRelativeTo(null);
    }

    private void addRow(JPanel p, GridBagConstraints c, int row, String label, JComponent f) {
        c.gridy = row;
        c.gridx = 0; p.add(new JLabel(label), c);
        c.gridx = 1; p.add(f, c);
    }

    private void login() {
        String name = user.getText().trim();
        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập username!");
            return;
        }
        try {
            ServerConnection conn = new ServerConnection();
            if (!conn.login(host.getText().trim(), Integer.parseInt(port.getText().trim()), name)) {
                JOptionPane.showMessageDialog(this, "Username đã được sử dụng hoặc không hợp lệ!");
                return;
            }
            ChatModel model = new ChatModel(name);
            ChatController controller = new ChatController(model, conn, SwingUtilities::invokeLater);
            new ChatFrame(model, controller).setVisible(true);
            dispose();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Không kết nối được server: " + ex.getMessage());
        }
    }
}