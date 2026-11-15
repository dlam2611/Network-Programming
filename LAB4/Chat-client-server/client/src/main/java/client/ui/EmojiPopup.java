package client.ui;

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;

/** Bảng chọn icon. */
public class EmojiPopup extends JPopupMenu {
    private static final String[] EMOJIS = {"😀", "😁", "😂", "🤣", "😊", "😍", "😘", "😎", "🤔", "😢", "😭", "😡",
            "👍", "👎", "👏", "🙏", "💪", "🎉", "❤️", "💔", "🔥", "⭐", "🌹", "🎂"};

    public EmojiPopup(Consumer<String> onPick) {
        JPanel p = new JPanel(new GridLayout(0, 8, 2, 2));
        p.setBackground(Color.WHITE);
        for (String emoji : EMOJIS) {
            JButton b = new JButton(emoji);
            b.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 20));
            b.setBorderPainted(false);
            b.setContentAreaFilled(false);
            b.setFocusPainted(false);
            b.addActionListener(ev -> {
                onPick.accept(emoji);
                setVisible(false);
            });
            p.add(b);
        }
        add(p);
    }

    public void showAbove(Component anchor) {
        show(anchor, 0, -getPreferredSize().height);
    }
}