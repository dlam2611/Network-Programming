package client.ui;

import common.Message;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;

/** Vùng cuộn hiển thị danh sách tin nhắn: tin của mình bên phải, tin người khác bên trái. */
public class MessageListPanel extends JPanel {
    private final JPanel list = new JPanel();
    private final JScrollPane scroll;
    private final String me;
    private final MessageBubble.Actions actions;

    public MessageListPanel(String me, MessageBubble.Actions actions) {
        super(new BorderLayout());
        this.me = me;
        this.actions = actions;

        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        list.setBackground(Color.WHITE);
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setBackground(Color.WHITE);
        wrap.add(list, BorderLayout.NORTH);

        scroll = new JScrollPane(wrap, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(BorderFactory.createMatteBorder(1, 0, 1, 0, new Color(0xEEEEEE)));
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        add(scroll, BorderLayout.CENTER);
    }

    /** Vẽ lại toàn bộ cuộc trò chuyện. */
    public void show(List<Message> messages, boolean group) {
        list.removeAll();
        for (Message m : messages) addRow(m, group);
        list.revalidate();
        list.repaint();
        scrollToBottom();
    }

    public void append(Message m, boolean group) {
        addRow(m, group);
        list.revalidate();
        scrollToBottom();
    }

    private void addRow(Message m, boolean group) {
        boolean mine = me.equals(m.from);
        MessageBubble bubble = new MessageBubble(m, mine, group && !mine, actions);
        JPanel row = new JPanel(new BorderLayout()) {
            @Override public Dimension getMaximumSize() {
                return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
            }
        };
        row.setOpaque(false);
        row.setBorder(new EmptyBorder(4, 10, 4, 10));
        row.add(bubble, mine ? BorderLayout.EAST : BorderLayout.WEST);
        list.add(row);
    }

    private void scrollToBottom() {
        SwingUtilities.invokeLater(() -> {
            JScrollBar sb = scroll.getVerticalScrollBar();
            sb.setValue(sb.getMaximum());
        });
    }
}