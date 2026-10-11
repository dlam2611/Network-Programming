package handler;

import service.ClientRegistry;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class RequestHandler {
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final Pattern IPV4 = Pattern.compile(
            "^((25[0-5]|2[0-4]\\d|1?\\d?\\d)\\.){3}(25[0-5]|2[0-4]\\d|1?\\d?\\d)$");
    private static final Pattern VALID_GROUP = Pattern.compile("^[a-zA-Z0-9_]{1,20}$");

    private final ClientRegistry registry;

    public RequestHandler(ClientRegistry registry) {
        this.registry = registry;
    }

    public Outcome handle(String request, InetSocketAddress sender) {
        // limit 3: nội dung tin nhắn được giữ nguyên dù có chứa dấu '|'
        String[] parts = request.split("\\|", 3);

        if ("BYE".equals(parts[0])) {
            registry.remove(sender);
            return new Outcome("Client thoát", Collections.emptyList());
        }

        registry.register(sender);
        switch (parts[0]) {
            case "HELLO":
                return reply(sender, "Client kết nối vào server", "INFO|Đã kết nối server");
            case "JOIN":
                return changeGroup(sender, parts, true);
            case "LEAVE":
                return changeGroup(sender, parts, false);
            case "SEND":
                return send(sender, parts);
            default:
                return reply(sender, "Lệnh không xác định (" + parts[0] + ") - THẤT BẠI",
                        "RESULT|Lệnh không hợp lệ");
        }
    }

    private Outcome changeGroup(InetSocketAddress sender, String[] parts, boolean join) {
        String action = join ? "vào" : "rời";
        if (parts.length < 2 || !VALID_GROUP.matcher(parts[1]).matches()) {
            return reply(sender, "Client " + action + " group - THẤT BẠI (tên group không hợp lệ)",
                    "RESULT|Tên group chỉ gồm chữ, số, dấu _ (tối đa 20 ký tự)");
        }
        String group = parts[1];
        if (join) {
            registry.join(sender, group);
        } else {
            registry.leave(sender, group);
        }
        List<Reply> replies = Arrays.asList(
                new Reply(sender, "INFO|Đã " + action + " group " + group),
                new Reply(sender, "GROUPS|" + String.join(",", registry.groupsOf(sender))));
        return new Outcome("Client " + action + " group '" + group + "' - THÀNH CÔNG", replies);
    }

    private Outcome send(InetSocketAddress sender, String[] parts) {
        if (parts.length < 3 || parts[2].isBlank()) {
            return reply(sender, "Gửi tin - THẤT BẠI (thiếu đích hoặc nội dung)",
                    "RESULT|Thiếu đích hoặc nội dung tin nhắn");
        }
        String target = parts[1];
        String message = parts[2];
        List<SocketAddress> receivers = resolveReceivers(target, sender);

        if (receivers.isEmpty()) {
            return reply(sender, "Gửi tin đến '" + target + "' - THẤT BẠI (không có máy nào nhận)",
                    "RESULT|THẤT BẠI: không có máy nào đang online ở đích " + target);
        }

        String senderIp = sender.getAddress().getHostAddress();
        String sendTime = LocalTime.now().format(TIME_FORMAT);
        List<Reply> replies = new ArrayList<>();
        for (SocketAddress receiver : receivers) {
            replies.add(new Reply(receiver, "MSG|" + senderIp + "|" + target + "|" + sendTime + "|" + message));
        }
        replies.add(new Reply(sender, "RESULT|THÀNH CÔNG: đã gửi đến " + receivers.size() + " máy"));
        return new Outcome("Gửi tin đến '" + target + "' (" + kindOf(target) + ") lúc " + sendTime
                + " - THÀNH CÔNG, " + receivers.size() + " máy nhận", replies);
    }

    private List<SocketAddress> resolveReceivers(String target, SocketAddress sender) {
        List<SocketAddress> found;
        if ("*".equals(target)) {
            found = registry.all();
        } else if (IPV4.matcher(target).matches()) {
            found = registry.byIp(target);
        } else {
            found = registry.byGroup(target);
        }
        // Không gửi lại cho chính người gửi
        return found.stream().filter(address -> !address.equals(sender)).collect(Collectors.toList());
    }

    private String kindOf(String target) {
        if ("*".equals(target)) {
            return "tất cả máy";
        }
        return IPV4.matcher(target).matches() ? "một máy" : "group";
    }

    private Outcome reply(SocketAddress to, String description, String text) {
        return new Outcome(description, Collections.singletonList(new Reply(to, text)));
    }
}