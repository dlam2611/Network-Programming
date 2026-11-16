package handler;

import service.MailException;
import service.MailService;

import java.io.IOException;
import java.util.List;

public class RequestHandler {
    private static final String OK = "OK";
    private static final String ERROR = "ERROR|";

    private final MailService service;

    public RequestHandler(MailService service) {
        this.service = service;
    }

    public String handle(String request) {
        return handle(request, "127.0.0.1");
    }

    public String handle(String request, String clientIp) {
        String[] parts = request.split("\\|", 7);
        try {
            switch (parts[0]) {
                case "REGISTER":
                    requireArgs(parts, 3);
                    service.createAccount(parts[1], parts[2], clientIp);
                    return OK;
                case "LOGIN":
                    requireArgs(parts, 3);
                    service.updateAccountIp(parts[1], clientIp);
                    List<String> files = service.listFiles(parts[1], parts[2]);
                    return OK + "|" + String.join("|", files);
                case "READ":
                    requireArgs(parts, 4);
                    return OK + "|" + service.readFile(parts[1], parts[2], parts[3]);
                case "SEND":
                    if (parts.length == 6) {
                        // SEND|from|password|to|subject|content
                        service.sendMail(parts[1], parts[2], parts[3], parts[4], parts[5], clientIp);
                    } else if (parts.length == 5) {
                        // SEND|from|password|to|content
                        service.sendMail(parts[1], parts[2], parts[3], "Không có tiêu đề", parts[4], clientIp);
                    } else if (parts.length == 7) {
                        // Hỗ trợ trường hợp có 7 tham số: SEND|from|password|to|receiverIp|subject|content
                        service.sendMail(parts[1], parts[2], parts[3], parts[5], parts[6], clientIp);
                    } else {
                        throw new MailException("Thiếu tham số cho lệnh SEND (yêu cầu người nhận, tiêu đề và nội dung)");
                    }
                    return OK;
                default:
                    return ERROR + "Lệnh không hợp lệ: " + parts[0];
            }
        } catch (MailException e) {
            return ERROR + e.getMessage();
        } catch (IOException e) {
            return ERROR + "Lỗi hệ thống trên server";
        }
    }

    private void requireArgs(String[] parts, int expected) throws MailException {
        if (parts.length != expected) {
            throw new MailException("Thiếu tham số cho lệnh " + parts[0]);
        }
    }
}