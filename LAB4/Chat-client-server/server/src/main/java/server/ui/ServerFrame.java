package server.ui;

import server.core.ServerListener;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;

/** Giao diện server: chỉ hiển thị log, không chứa logic mạng. */
public class ServerFrame extends JFrame implements ServerListener {
    private final DefaultTableModel model = new DefaultTableModel(
            new String[]{"Thời gian", "Địa chỉ IP client", "Username", "Hành động"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable table = new JTable(model);
    private final JLabel lblOnline = new JLabel("Đang online: 0");

    public ServerFrame(String info) {
        super("Chat Server");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(900, 520);
        setLocationRelativeTo(null);

        JLabel lblInfo = new JLabel(info);
        lblInfo.setFont(lblInfo.getFont().deriveFont(Font.BOLD, 15f));
        JButton btnClear = new JButton("Xóa log");
        btnClear.addActionListener(e -> model.setRowCount(0));

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        right.setOpaque(false);
        right.add(lblOnline);
        right.add(btnClear);
        JPanel top = new JPanel(new BorderLayout(10, 0));
        top.setBorder(new EmptyBorder(10, 12, 10, 12));
        top.setBackground(Color.WHITE);
        top.add(lblInfo, BorderLayout.WEST);
        top.add(right, BorderLayout.EAST);

        table.setRowHeight(24);
        table.setFillsViewportHeight(true);
        int[] w = {150, 150, 110, 480};
        for (int i = 0; i < w.length; i++) table.getColumnModel().getColumn(i).setPreferredWidth(w[i]);

        add(top, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
    }

    @Override public void onLog(String ip, String user, String action) {
        String t = new SimpleDateFormat("HH:mm:ss dd/MM/yyyy").format(new Date());
        SwingUtilities.invokeLater(() -> {
            model.addRow(new Object[]{t, ip, user, action});
            table.scrollRectToVisible(table.getCellRect(model.getRowCount() - 1, 0, true));
        });
    }

    @Override public void onOnlineChanged(int count) {
        SwingUtilities.invokeLater(() -> lblOnline.setText("Đang online: " + count));
    }
}