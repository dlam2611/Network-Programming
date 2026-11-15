package client.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/** Thanh nhập tin: file, icon, voice, ô nhập và nút gửi. */
public class InputBar extends JPanel {
    public interface Listener {
        void onSend(String text);
        void onPickFile();
        void onToggleRecord();
    }

    // Font riêng cho nút emoji (Segoe UI không có emoji)
    private static final Font EMOJI_FONT = new Font("Segoe UI Emoji", Font.PLAIN, 18);

    private final JTextField field = new JTextField();
    private final JButton mic = new JButton("🎤");

    public InputBar(Listener l) {
        super(new BorderLayout(6, 0));
        setBackground(Color.WHITE);
        setBorder(new EmptyBorder(8, 10, 8, 10));

        JButton file = new JButton("📎");
        JButton emoji = new JButton("😊");
        JButton send = new JButton("Gửi");
        for (JButton b : new JButton[]{file, emoji, mic}) {
            b.setFont(EMOJI_FONT);
            b.setFocusPainted(false);
        }
        send.setFont(Theme.FONT);
        send.setFocusPainted(false);
        send.setBackground(Theme.MINE_BG);
        send.setForeground(Color.WHITE);
        field.setFont(Theme.FONT);

        EmojiPopup popup = new EmojiPopup(em -> {
            field.setText(field.getText() + em);
            field.requestFocus();
        });
        Runnable doSend = () -> {
            l.onSend(field.getText());
            field.setText("");
        };

        file.addActionListener(e -> l.onPickFile());
        emoji.addActionListener(e -> popup.showAbove(emoji));
        mic.addActionListener(e -> l.onToggleRecord());
        send.addActionListener(e -> doSend.run());
        field.addActionListener(e -> doSend.run());

        JPanel tools = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        tools.setOpaque(false);
        tools.add(file);
        tools.add(emoji);
        tools.add(mic);
        add(tools, BorderLayout.WEST);
        add(field, BorderLayout.CENTER);
        add(send, BorderLayout.EAST);
    }

    /** Đang ghi âm: nút hiện ⏹ màu đỏ. Bấm lại để dừng và gửi. */
    public void setRecording(boolean recording) {
        mic.setText(recording ? "⏹" : "🎤");
        mic.setForeground(recording ? Color.RED : Color.BLACK);
    }
}