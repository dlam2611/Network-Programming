package web.ui;

import web.core.WebListener;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.Date;

/** Giao diện server: bảng lịch sử truy cập theo thời gian thực. Không chứa logic HTTP. */
public class ServerFrame extends JFrame implements WebListener {
    private final DefaultTableModel model = new DefaultTableModel(
            new String[]{"Thời gian", "IP client", "Method", "URL", "Mã", "Ghi chú"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable table = new JTable(model);
    private final JLabel lblCount = new JLabel("Số request: 0");
    private int count;

    public ServerFrame(String info, String url) {
        super("Web Server");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1000, 540);
        setLocationRelativeTo(null);

        JLabel lblInfo = new JLabel(info);
        lblInfo.setFont(lblInfo.getFont().deriveFont(Font.BOLD, 13f));
        JButton btnOpen = new JButton("Mở trình duyệt");
        btnOpen.addActionListener(e -> {
            try { Desktop.getDesktop().browse(new URI(url)); } catch (Exception ex) { /* bỏ qua */ }
        });
        JButton btnClear = new JButton("Xóa log");
        btnClear.addActionListener(e -> model.setRowCount(0));

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        right.setOpaque(false);
        right.add(lblCount);
        right.add(btnOpen);
        right.add(btnClear);
        JPanel top = new JPanel(new BorderLayout(10, 0));
        top.setBorder(new EmptyBorder(10, 12, 10, 12));
        top.setBackground(Color.WHITE);
        top.add(lblInfo, BorderLayout.CENTER);
        top.add(right, BorderLayout.EAST);

        table.setRowHeight(24);
        table.setFillsViewportHeight(true);
        int[] w = {140, 110, 60, 260, 50, 360};
        for (int i = 0; i < w.length; i++) table.getColumnModel().getColumn(i).setPreferredWidth(w[i]);

        add(top, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
    }

    @Override public void onRequest(String ip, String method, String url, int status, String note) {
        addRow(ip, method, url, String.valueOf(status), note);
        SwingUtilities.invokeLater(() -> lblCount.setText("Số request: " + (++count)));
    }

    @Override public void onStatus(String text) {
        addRow("-", "-", "-", "-", text);
    }

    private void addRow(String ip, String method, String url, String status, String note) {
        String t = new SimpleDateFormat("HH:mm:ss dd/MM/yyyy").format(new Date());
        SwingUtilities.invokeLater(() -> {
            model.addRow(new Object[]{t, ip, method, url, status, note});
            table.scrollRectToVisible(table.getCellRect(model.getRowCount() - 1, 0, true));
        });
    }
}