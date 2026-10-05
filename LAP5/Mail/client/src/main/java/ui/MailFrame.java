package ui;

import app.MailClient;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

public class MailFrame extends JFrame {
    private final MailClient client;
    private String currentUser;

    private final JTextField txtUsername = new JTextField(15);
    private final JButton btnRegister = new JButton("Tạo tài khoản");
    private final JButton btnLogin = new JButton("Đăng nhập");
    private final JButton btnReload = new JButton("Tải lại");

    private final DefaultListModel<String> inboxModel = new DefaultListModel<>();
    private final JList<String> lstInbox = new JList<>(inboxModel);
    private final JTextArea txtViewer = new JTextArea();

    private final JTextField txtTo = new JTextField();
    private final JTextArea txtContent = new JTextArea();
    private final JButton btnSend = new JButton("Gửi email");

    private final JLabel lblStatus = new JLabel("Hãy tạo tài khoản hoặc đăng nhập");

    public MailFrame(MailClient client) {
        this.client = client;

        setTitle("Mail Client");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(850, 520);
        setLocationRelativeTo(null);

        setLayout(new BorderLayout(8, 8));
        add(buildAccountPanel(), BorderLayout.NORTH);
        add(buildMainPanel(), BorderLayout.CENTER);
        add(lblStatus, BorderLayout.SOUTH);
        lblStatus.setBorder(BorderFactory.createEmptyBorder(4, 10, 6, 10));

        btnRegister.addActionListener(e -> onRegister());
        btnLogin.addActionListener(e -> onLogin());
        btnReload.addActionListener(e -> onReload());
        btnSend.addActionListener(e -> onSend());
        lstInbox.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                onSelectFile();
            }
        });
    }

    private JPanel buildAccountPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.add(new JLabel("Tài khoản:"));
        panel.add(txtUsername);
        panel.add(btnRegister);
        panel.add(btnLogin);
        panel.add(btnReload);
        return panel;
    }

    private JSplitPane buildMainPanel() {
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, buildInboxPanel(), buildComposePanel());
        split.setDividerLocation(330);
        return split;
    }

    // Bên trái: danh sách file (trên) + nội dung file đang chọn (dưới)
    private JSplitPane buildInboxPanel() {
        lstInbox.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane listPane = new JScrollPane(lstInbox);
        listPane.setBorder(BorderFactory.createTitledBorder("Hộp thư (tên file)"));

        txtViewer.setEditable(false);
        txtViewer.setLineWrap(true);
        txtViewer.setWrapStyleWord(true);
        txtViewer.setMargin(new Insets(6, 6, 6, 6));
        JScrollPane viewerPane = new JScrollPane(txtViewer);
        viewerPane.setBorder(BorderFactory.createTitledBorder("Nội dung file"));

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, listPane, viewerPane);
        split.setDividerLocation(180);
        return split;
    }

    // Bên phải: soạn và gửi email
    private JPanel buildComposePanel() {
        JPanel toPanel = new JPanel(new BorderLayout(6, 0));
        toPanel.add(new JLabel("Gửi đến:"), BorderLayout.WEST);
        toPanel.add(txtTo, BorderLayout.CENTER);

        txtContent.setLineWrap(true);
        txtContent.setWrapStyleWord(true);
        JScrollPane contentPane = new JScrollPane(txtContent);
        contentPane.setBorder(BorderFactory.createTitledBorder("Nội dung"));

        JPanel panel = new JPanel(new BorderLayout(6, 6));
        panel.setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 6));
        panel.add(toPanel, BorderLayout.NORTH);
        panel.add(contentPane, BorderLayout.CENTER);
        panel.add(btnSend, BorderLayout.SOUTH);
        return panel;
    }

    private void onRegister() {
        String username = txtUsername.getText().trim();
        runAsync(() -> {
            client.register(username);
            return username;
        }, name -> lblStatus.setText("Đã tạo tài khoản: " + name));
    }

    private void onLogin() {
        String username = txtUsername.getText().trim();
        runAsync(() -> client.login(username), files -> {
            currentUser = username;
            showFiles(files);
            lblStatus.setText("Đã đăng nhập: " + username + " (" + files.size() + " file)");
        });
    }

    // Lấy lại danh sách file của tài khoản đang đăng nhập
    private void onReload() {
        if (currentUser == null) {
            lblStatus.setText("Hãy đăng nhập trước");
            return;
        }
        runAsync(() -> client.login(currentUser), files -> {
            showFiles(files);
            lblStatus.setText("Đã tải lại: " + files.size() + " file");
        });
    }

    private void onSend() {
        if (currentUser == null) {
            lblStatus.setText("Hãy đăng nhập trước khi gửi email");
            return;
        }
        String to = txtTo.getText().trim();
        String content = txtContent.getText();
        runAsync(() -> {
            client.send(currentUser, to, content);
            return null;
        }, result -> {
            txtContent.setText("");
            lblStatus.setText("Đã gửi email đến " + to);
            JOptionPane.showMessageDialog(this, "Gửi email thành công đến " + to,
                    "Thành công", JOptionPane.INFORMATION_MESSAGE);
        });
    }

    // Bấm vào một file trong danh sách thì lấy nội dung từ server
    private void onSelectFile() {
        String fileName = lstInbox.getSelectedValue();
        if (fileName == null || currentUser == null) {
            return;
        }
        runAsync(() -> client.read(currentUser, fileName), content -> {
            txtViewer.setText(content);
            txtViewer.setCaretPosition(0);
            lblStatus.setText("Đang xem: " + fileName);
        });
    }

    private void showFiles(java.util.List<String> files) {
        inboxModel.clear();
        files.forEach(inboxModel::addElement);
        txtViewer.setText("");
    }

    // Chạy tác vụ gọi server ở luồng nền, xong thì cập nhật giao diện
    private <T> void runAsync(Callable<T> task, Consumer<T> onSuccess) {
        setBusy(true);
        lblStatus.setText("Đang xử lý...");

        new SwingWorker<T, Void>() {
            @Override
            protected T doInBackground() throws Exception {
                return task.call();
            }

            @Override
            protected void done() {
                try {
                    onSuccess.accept(get());
                } catch (Exception ex) {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    lblStatus.setText("Lỗi: " + cause.getMessage());
                } finally {
                    setBusy(false);
                }
            }
        }.execute();
    }

    private void setBusy(boolean busy) {
        btnRegister.setEnabled(!busy);
        btnLogin.setEnabled(!busy);
        btnReload.setEnabled(!busy);
        btnSend.setEnabled(!busy);
        lstInbox.setEnabled(!busy);
    }
}