package client.ui;

import common.Message;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;

/** Một bong bóng tin nhắn (text, file hoặc voice). Màu khác nhau giữa tin của mình và của người khác. */
public class MessageBubble extends JPanel {
    public interface Actions {
        void saveFile(Message m);
        void playVoice(Message m);
    }

    private final Color bg;

    public MessageBubble(Message m, boolean mine, boolean showSender, Actions actions) {
        this.bg = mine ? Theme.MINE_BG : Theme.OTHER_BG;
        Color fg = mine ? Theme.MINE_TEXT : Theme.OTHER_TEXT;
        setOpaque(false);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(new EmptyBorder(8, 12, 6, 12));

        if (showSender) {
            JLabel who = new JLabel(m.from);
            who.setFont(Theme.fontFor(m.from, Font.BOLD, 11f));
            who.setForeground(new Color(0x0066CC));
            addLeft(who);
        }

        switch (m.type) {
            case TEXT:
                addLeft(textLabel(m.text, fg));
                break;
            case FILE:
                String fileText = "📎 " + m.fileName + " (" + Math.max(1, m.data.length / 1024) + " KB)";
                JLabel name = new JLabel(fileText);
                name.setFont(Theme.fontFor(fileText, Font.PLAIN, 14f));
                name.setForeground(fg);
                JButton save = new JButton("💾 Lưu file");
                save.setFont(Theme.fontFor(save.getText(), Font.PLAIN, 14f));
                save.addActionListener(e -> actions.saveFile(m));
                addLeft(name);
                add(Box.createVerticalStrut(4));
                addLeft(save);
                break;
            case VOICE:
                JButton play = new JButton("▶ Tin nhắn thoại (" + m.text + "s)");
                play.setFont(Theme.fontFor(play.getText(), Font.PLAIN, 14f));
                play.addActionListener(e -> actions.playVoice(m));
                addLeft(play);
                break;
            default:
                break;
        }

        JLabel time = new JLabel(new SimpleDateFormat("HH:mm").format(new Date(m.time)));
        time.setFont(Theme.size(10f));
        time.setForeground(mine ? Theme.MINE_TIME : Theme.OTHER_TIME);
        add(Box.createVerticalStrut(2));
        addLeft(time);
    }

    private void addLeft(JComponent c) {
        c.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(c);
    }

    private static JLabel textLabel(String t, Color fg) {
        String body = t.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\n", "<br>");
        boolean longText = t.length() > 30 || t.contains("\n");
        JLabel l = new JLabel(longText ? "<html><body style='width:260px'>" + body : "<html>" + body);
        l.setFont(Theme.fontFor(t, Font.PLAIN, 14f));
        l.setForeground(fg);
        return l;
    }

    @Override protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(bg);
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 18, 18);
        g2.dispose();
        super.paintComponent(g);
    }
}