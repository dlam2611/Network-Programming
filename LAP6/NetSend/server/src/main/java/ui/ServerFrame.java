package ui;

import javax.swing.*;
import java.awt.*;

public class ServerFrame extends JFrame {
    private final JTextArea txtLog = new JTextArea();

    public ServerFrame() {
        setTitle("NetSend Server");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(700, 420);
        setLocationRelativeTo(null);

        txtLog.setEditable(false);
        txtLog.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        txtLog.setMargin(new Insets(6, 6, 6, 6));

        JScrollPane scrollPane = new JScrollPane(txtLog);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Log server"));
        add(scrollPane, BorderLayout.CENTER);
    }

    // Gọi được từ luồng bất kỳ: tự chuyển về luồng giao diện
    public void log(String message) {
        SwingUtilities.invokeLater(() -> {
            txtLog.append(message + "\n");
            txtLog.setCaretPosition(txtLog.getDocument().getLength());
        });
    }
}