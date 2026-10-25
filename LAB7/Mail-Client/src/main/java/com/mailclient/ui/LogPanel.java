package com.mailclient.ui;

import com.mailclient.protocol.common.ProtocolLogger;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Thành phần giao diện hiển thị nhật ký giao tiếp mạng (Protocol Log)
 * với định dạng màu sắc trực quan (Lệnh gửi, Phản hồi, Thông báo, Lỗi).
 */
public class LogPanel extends JPanel implements ProtocolLogger {
    private final JTextPane textPane;
    private final StyledDocument doc;
    private final JCheckBox chkAutoScroll;
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final Style styleSent;
    private final Style styleReceived;
    private final Style styleInfo;
    private final Style styleError;
    private final Style styleTime;

    public LogPanel(String title) {
        setLayout(new BorderLayout(5, 5));
        setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(200, 205, 215)),
                " " + title + " "
        ));

        textPane = new JTextPane();
        textPane.setEditable(false);
        textPane.setFont(new Font("Consolas", Font.PLAIN, 12));
        textPane.setBackground(new Color(248, 250, 252));
        doc = textPane.getStyledDocument();

        // Định nghĩa các kiểu màu sắc
        Style defaultStyle = StyleContext.getDefaultStyleContext().getStyle(StyleContext.DEFAULT_STYLE);

        styleTime = doc.addStyle("time", defaultStyle);
        StyleConstants.setForeground(styleTime, new Color(148, 163, 184)); // Gray
        StyleConstants.setFontSize(styleTime, 11);

        styleSent = doc.addStyle("sent", defaultStyle);
        StyleConstants.setForeground(styleSent, new Color(2, 132, 199)); // Blue
        StyleConstants.setBold(styleSent, true);

        styleReceived = doc.addStyle("received", defaultStyle);
        StyleConstants.setForeground(styleReceived, new Color(22, 163, 74)); // Green
        StyleConstants.setBold(styleReceived, true);

        styleInfo = doc.addStyle("info", defaultStyle);
        StyleConstants.setForeground(styleInfo, new Color(100, 116, 139)); // Slate
        StyleConstants.setItalic(styleInfo, true);

        styleError = doc.addStyle("error", defaultStyle);
        StyleConstants.setForeground(styleError, new Color(220, 38, 38)); // Red
        StyleConstants.setBold(styleError, true);

        JScrollPane scrollPane = new JScrollPane(textPane);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        add(scrollPane, BorderLayout.CENTER);

        // Thanh công cụ điều khiển Log (Clear, Copy, Auto-scroll)
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));

        chkAutoScroll = new JCheckBox("Tự cuộn xuống", true);
        chkAutoScroll.setFocusable(false);

        JButton btnCopy = new JButton("Sao chép Log");
        btnCopy.setFocusable(false);
        btnCopy.addActionListener(e -> copyAll());

        JButton btnClear = new JButton("Xóa Log");
        btnClear.setFocusable(false);
        btnClear.addActionListener(e -> clear());

        toolbar.add(chkAutoScroll);
        toolbar.add(btnCopy);
        toolbar.add(btnClear);

        add(toolbar, BorderLayout.SOUTH);
    }

    private void append(String prefix, Style prefixStyle, String message, Style contentStyle) {
        SwingUtilities.invokeLater(() -> {
            try {
                String timeStr = "[" + LocalTime.now().format(timeFormatter) + "] ";
                doc.insertString(doc.getLength(), timeStr, styleTime);
                doc.insertString(doc.getLength(), prefix + " ", prefixStyle);
                doc.insertString(doc.getLength(), message + "\n", contentStyle);

                if (chkAutoScroll.isSelected()) {
                    textPane.setCaretPosition(doc.getLength());
                }
            } catch (BadLocationException ignored) {}
        });
    }

    @Override
    public void logSent(String message) {
        append("-> CLIENT :", styleSent, message, styleSent);
    }

    @Override
    public void logReceived(String message) {
        append("<- SERVER :", styleReceived, message, styleReceived);
    }

    @Override
    public void logInfo(String message) {
        append("[*] THÔNG TIN :", styleInfo, message, styleInfo);
    }

    @Override
    public void logError(String message) {
        append("[!] LỖI :", styleError, message, styleError);
    }

    public void clear() {
        SwingUtilities.invokeLater(() -> {
            try {
                doc.remove(0, doc.getLength());
            } catch (BadLocationException ignored) {}
        });
    }

    public void copyAll() {
        String text = textPane.getText();
        if (text != null && !text.isEmpty()) {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text), null);
            JOptionPane.showMessageDialog(this, "Đã sao chép toàn bộ nhật ký vào Clipboard!", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
        }
    }
}
