package ui;

import app.ExchangeRateClient;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.Callable;

public class ExchangeRateFrame extends JFrame {
    private final ExchangeRateClient client;
    private final JButton btnRate = new JButton("Lấy tỷ giá");
    private final JTextArea txtResult = new JTextArea();

    public ExchangeRateFrame(ExchangeRateClient client) {
        this.client = client;

        setTitle("Tỷ giá hối đoái");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(450, 300);
        setLocationRelativeTo(null);

        txtResult.setEditable(false);
        txtResult.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        txtResult.setMargin(new Insets(10, 10, 10, 10));

        JPanel top = new JPanel();
        top.add(btnRate);

        setLayout(new BorderLayout());
        add(top, BorderLayout.NORTH);
        add(new JScrollPane(txtResult), BorderLayout.CENTER);

        btnRate.addActionListener(e -> runTask(() -> {
            var rate = client.fetchRate();
            return "Thời gian : " + rate.getTime() + "\n" +
                    "Tokyo     : " + rate.getTokyo() + "\n" +
                    "New York  : " + rate.getNewYork() + "\n" +
                    "Hong Kong : " + rate.getHongKong();
        }));
    }

    private void runTask(Callable<String> task) {
        btnRate.setEnabled(false);
        txtResult.setText("Đang lấy dữ liệu...");

        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                return task.call();
            }

            @Override
            protected void done() {
                try {
                    txtResult.setText(get());
                } catch (Exception ex) {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    txtResult.setText("Lỗi: " + cause.getMessage());
                } finally {
                    btnRate.setEnabled(true);
                }
            }
        }.execute();
    }
}