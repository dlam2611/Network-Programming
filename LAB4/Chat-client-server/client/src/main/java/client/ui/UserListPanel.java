package client.ui;

import common.Message;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

/** Cột bên trái: ô tìm người, mục "Nhóm chung" và danh sách người online. */
public class UserListPanel extends JPanel {
    private final DefaultListModel<String> model = new DefaultListModel<>();
    private final JList<String> list = new JList<>(model);
    private final JTextField search = new JTextField();
    private boolean adjusting;

    public UserListPanel(String me, Predicate<String> isUnread, Consumer<String> onSelect, Runnable onSearch) {
        super(new BorderLayout());
        setBackground(Theme.SIDE_BG);
        setPreferredSize(new Dimension(240, 0));
        setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, Theme.BORDER));

        JLabel hello = new JLabel("👤 " + me);
        hello.setFont(Theme.fontFor(hello.getText(), Font.BOLD, 16f));
        search.setBorder(BorderFactory.createTitledBorder("🔍 Tìm người muốn nhắn"));
        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        head.setBorder(new EmptyBorder(12, 10, 4, 10));
        head.add(hello, BorderLayout.NORTH);
        head.add(search, BorderLayout.SOUTH);

        list.setBackground(Theme.SIDE_BG);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setCellRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(JList<?> l, Object v, int i, boolean sel, boolean f) {
                super.getListCellRendererComponent(l, v, i, sel, f);
                String key = (String) v;
                String text = Message.GROUP.equals(key) ? "👥  Nhóm chung" : "👤  " + key;
                boolean unread = isUnread.test(key);
                setText(unread ? "● " + text : text);
                setFont(Theme.fontFor(getText(), unread ? Font.BOLD : Font.PLAIN, 14f));                setBorder(new EmptyBorder(9, 12, 9, 12));
                return this;
            }
        });
        list.addListSelectionListener(e -> {
            String v = list.getSelectedValue();
            if (!e.getValueIsAdjusting() && !adjusting && v != null) onSelect.accept(v);
        });
        search.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { onSearch.run(); }
            public void removeUpdate(DocumentEvent e) { onSearch.run(); }
            public void changedUpdate(DocumentEvent e) { onSearch.run(); }
        });

        add(head, BorderLayout.NORTH);
        add(new JScrollPane(list, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER), BorderLayout.CENTER);
    }

    public String getQuery() { return search.getText(); }

    /** Nạp lại danh sách (luôn có "Nhóm chung" ở đầu) và giữ lựa chọn hiện tại. */
    public void refresh(List<String> users, String current) {
        adjusting = true;
        model.clear();
        model.addElement(Message.GROUP);
        for (String u : users) model.addElement(u);
        int idx = model.indexOf(current);
        if (idx >= 0) list.setSelectedIndex(idx); else list.clearSelection();
        adjusting = false;
    }

    public void repaintList() { list.repaint(); }
}