package ui;

import app.NetSendClient;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;

public class NetSendFrame extends JFrame {
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final Pattern IPV4 = Pattern.compile(
            "^((25[0-5]|2[0-4]\\d|1?\\d?\\d)\\.){3}(25[0-5]|2[0-4]\\d|1?\\d?\\d)$");

    private final NetSendClient client;

    private final JTextField txtCommand = new JTextField();
    private final JButton btnSend = new JButton("Gửi");
    private final JTextField txtGroup = new JTextField(10);
    private final JButton btnJoin = new JButton("Tham gia");
    private final JButton btnLeave = new JButton("Rời");
    private final JLabel lblGroups = new JLabel("Nhóm của tôi: (chưa tham gia nhóm nào)");
    private final JTextArea txtReceived = new JTextArea();
    private final JLabel lblStatus = new JLabel("Ví dụ: net send 192.168.1.5 xin chào");

    public NetSendFrame(NetSendClient client) {
        this.client = client;

        setTitle("NetSend");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(650, 480);
        setLocationRelativeTo(null);

        setLayout(new BorderLayout(8, 8));
        add(buildTopPanel(), BorderLayout.NORTH);
        add(buildReceivedPanel(), BorderLayout.CENTER);
        add(lblStatus, BorderLayout.SOUTH);
        lblStatus.setBorder(BorderFactory.createEmptyBorder(4, 10, 6, 10));

        btnSend.addActionListener(e -> onSend());
        txtCommand.addActionListener(e -> onSend()); // nhấn Enter cũng gửi
        btnJoin.addActionListener(e -> onJoin());
        btnLeave.addActionListener(e -> onLeave());

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                runSafely(client::bye);
            }
        });

        client.listen(message -> SwingUtilities.invokeLater(() -> onServerMessage(message)));
        runSafely(client::hello);
    }

    private JPanel buildTopPanel() {
        JPanel commandPanel = new JPanel(new BorderLayout(6, 0));
        commandPanel.add(new JLabel("Lệnh:"), BorderLayout.WEST);
        commandPanel.add(txtCommand, BorderLayout.CENTER);
        commandPanel.add(btnSend, BorderLayout.EAST);

        JPanel groupPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        groupPanel.add(new JLabel("Group:"));
        groupPanel.add(txtGroup);
        groupPanel.add(btnJoin);
        groupPanel.add(btnLeave);

        JPanel panel = new JPanel(new GridLayout(3, 1, 0, 6));
        panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 0, 8));
        panel.add(commandPanel);
        panel.add(groupPanel);
        panel.add(lblGroups);
        return panel;
    }

    private JScrollPane buildReceivedPanel() {
        txtReceived.setEditable(false);
        txtReceived.setLineWrap(true);
        txtReceived.setWrapStyleWord(true);
        txtReceived.setMargin(new Insets(6, 6, 6, 6));

        JScrollPane pane = new JScrollPane(txtReceived);
        pane.setBorder(BorderFactory.createTitledBorder("Tin nhắn"));
        return pane;
    }

    private void onSend() {
        String raw = txtCommand.getText().trim();
        String[] parts = raw.split("\\s+", 4);
        boolean valid = parts.length == 4
                && parts[0].equalsIgnoreCase("net")
                && parts[1].equalsIgnoreCase("send");

        if (!valid) {
            lblStatus.setText("Cú pháp: net send {IP | group | *} message");
            return;
        }

        try {
            client.sendCommand(raw);
            txtCommand.setText("");
            String now = LocalTime.now().format(TIME);
            txtReceived.append("[" + now + "] Bạn đã gửi → " + formatTarget(parts[2]) + "--------" + parts[3] + "\n");
            txtReceived.setCaretPosition(txtReceived.getDocument().getLength());
            lblStatus.setText("Đã gửi lệnh, đang chờ server trả kết quả...");
        } catch (IllegalArgumentException e) {
            lblStatus.setText(e.getMessage());
        } catch (IOException e) {
            lblStatus.setText("Không gửi được: " + e.getMessage());
        }
    }

    private void onJoin() {
        String group = txtGroup.getText().trim();
        runSafely(() -> client.join(group));
    }

    private void onLeave() {
        String group = txtGroup.getText().trim();
        runSafely(() -> client.leave(group));
    }

    // Xử lý gói tin server gửi về (đã chạy trên luồng giao diện)
    private void onServerMessage(String message) {
        String[] parts = message.split("\\|", 5);
        switch (parts[0]) {
            case "MSG":
                if (parts.length == 5) {
                    showIncoming(parts[1], parts[2], parts[3], parts[4]);
                } else if (parts.length == 3) {
                    showIncoming(parts[1], "N/A", LocalTime.now().format(TIME), parts[2]);
                }
                break;
            case "GROUPS":
                showGroups(parts.length > 1 ? parts[1] : "");
                break;
            case "RESULT":
            case "INFO":
                lblStatus.setText(parts.length > 1 ? parts[1] : "");
                break;
            default:
                break;
        }
    }

    // Hiện tin nhắn trong danh sách
    private void showIncoming(String senderIp, String target, String sendTime, String text) {
        String formatted = "[" + sendTime + "] Từ " + senderIp + " → " + formatTarget(target) + ": " + text;
        txtReceived.append(formatted + "\n");
        txtReceived.setCaretPosition(txtReceived.getDocument().getLength());
    }

    private String formatTarget(String target) {
        if ("*".equals(target)) {
            return "tất cả máy trong mạng";
        } else if (IPV4.matcher(target).matches()) {
            return "máy " + target;
        } else {
            return "nhóm " + target;
        }
    }

    private void showGroups(String groups) {
        lblGroups.setText(groups.isEmpty()
                ? "Nhóm của tôi: (chưa tham gia nhóm nào)"
                : "Nhóm của tôi: " + groups.replace(",", ", "));
    }

    private void runSafely(IoAction action) {
        try {
            action.run();
        } catch (IOException e) {
            lblStatus.setText("Lỗi mạng: " + e.getMessage());
        }
    }

    @FunctionalInterface
    private interface IoAction {
        void run() throws IOException;
    }
}