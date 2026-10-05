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
        String[] parts = request.split("\\|", 4);
        try {
            switch (parts[0]) {
                case "REGISTER":
                    requireArgs(parts, 2);
                    service.createAccount(parts[1]);
                    return OK;
                case "SEND":
                    requireArgs(parts, 4);
                    service.sendMail(parts[1], parts[2], parts[3]);
                    return OK;
                case "LOGIN":
                    requireArgs(parts, 2);
                    List<String> files = service.listFiles(parts[1]);
                    return OK + "|" + String.join("|", files);
                case "READ":
                    requireArgs(parts, 3);
                    return OK + "|" + service.readFile(parts[1], parts[2]);
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