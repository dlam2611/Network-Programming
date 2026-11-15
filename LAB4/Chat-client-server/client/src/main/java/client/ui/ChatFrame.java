package client.ui;

import client.controller.ChatController;
import client.model.ChatModel;
import common.Message;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.File;

/** Cửa sổ chat chính: chỉ lắp ghép các thành phần giao diện, mọi xử lý giao cho ChatController. */
public class ChatFrame extends JFrame
        implements ChatController.View, MessageBubble.Actions, InputBar.Listener {

    private final ChatController ctl;
    private final ChatModel model;
    private final UserListPanel users;
    private final MessageListPanel messages;
    private final InputBar input;
    private final JLabel lblTitle = new JLabel();

    public ChatFrame(ChatModel model, ChatController ctl) {
        super("Chat - " + model.getName());
        this.model = model;
        this.ctl = ctl;
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(900, 620);
        setLocationRelativeTo(null);

        users = new UserListPanel(model.getName(), model::isUnread, this::selectChat, this::refreshUsers);
        messages = new MessageListPanel(model.getName(), this);
        input = new InputBar(this);

        lblTitle.setFont(Theme.bold(16f));
        lblTitle.setBorder(new EmptyBorder(12, 14, 12, 14));

        JPanel center = new JPanel(new BorderLayout());
        center.add(lblTitle, BorderLayout.NORTH);
        center.add(messages, BorderLayout.CENTER);
        center.add(input, BorderLayout.SOUTH);
        add(users, BorderLayout.WEST);
        add(center, BorderLayout.CENTER);

        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override public void windowClosing(java.awt.event.WindowEvent e) { ctl.disconnect(); }
        });

        refreshUsers();
        renderChat();
        ctl.attach(this);
    }

    // ---- thao tác trên giao diện ----
    private void selectChat(String key) {
        ctl.selectChat(key);
        renderChat();
    }

    private void refreshUsers() {
        users.refresh(model.searchUsers(users.getQuery()), model.getCurrent());
        updateTitle();
    }

    private void renderChat() {
        updateTitle();
        messages.show(model.history(model.getCurrent()), model.isGroup());
    }

    private void updateTitle() {
        String cur = model.getCurrent();
        if (model.isGroup()) lblTitle.setText("👥 Nhóm chung");
        else lblTitle.setText("👤 " + cur + (model.isOnline(cur) ? "" : "  (đã offline)"));
        lblTitle.setFont(Theme.fontFor(lblTitle.getText(), Font.BOLD, 16f));
    }

    // ---- ChatController.View ----
    @Override public void onUsersChanged() { refreshUsers(); }
    @Override public void onMessageForCurrentChat(Message m) { messages.append(m, model.isGroup()); }
    @Override public void onUnreadChanged() { users.repaintList(); }
    @Override public void onRecordingChanged(boolean recording) { input.setRecording(recording); }
    @Override public void onError(String text) { JOptionPane.showMessageDialog(this, text); }
    @Override public void onDisconnected() {
        JOptionPane.showMessageDialog(this, "Mất kết nối tới server!");
        System.exit(0);
    }

    // ---- MessageBubble.Actions ----
    @Override public void saveFile(Message m) {
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File(m.fileName));
        if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) ctl.saveFile(m, fc.getSelectedFile());
    }

    @Override public void playVoice(Message m) { ctl.playVoice(m); }

    // ---- InputBar.Listener ----
    @Override public void onSend(String text) { ctl.sendText(text); }

    @Override public void onPickFile() {
        JFileChooser fc = new JFileChooser();
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) ctl.sendFile(fc.getSelectedFile());
    }

    @Override public void onToggleRecord() { ctl.toggleRecording(); }
}