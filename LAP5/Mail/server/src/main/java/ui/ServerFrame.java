package ui;

import javax.swing.*;
import java.awt.*;

public class ServerFrame extends JFrame {
    private final JTextArea txtLog = new JTextArea();

    public ServerFrame() {
        setTitle("Mail Server");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(650, 400);
        setLocationRelativeTo(null);

        txtLog.setEditable(false);
        txtLog.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        txtLog.setMargin(new Insets(6, 6, 6, 6));

        JScrollPane scrollPane = new JScrollPane(txtLog);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Log server"));
        add(scrollPane, BorderLayout.CENTER);
    }

    // Có thể gọi từ luồng bất kỳ: tự chuyển về luồng giao diện
    public void log(String message) {
        SwingUtilities.invokeLater(() -> {
            txtLog.append(message + "\n");
            txtLog.setCaretPosition(txtLog.getDocument().getLength()); // tự cuộn xuống dòng mới nhất
        });
    }
}