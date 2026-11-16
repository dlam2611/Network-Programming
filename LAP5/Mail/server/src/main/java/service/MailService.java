package service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MailService {
    private static final Path MAIL_ROOT = Paths.get("mailbox");
    private static final Path ACCOUNT_ROOT = Paths.get("accounts");
    private static final String WELCOME_FILE = "new_email.txt";
    private static final String WELCOME_TEXT =
            "Thank you for using this service. We hope that you will feel comfortable...";
    private static final String PASSWORD_KEY = "password=";

    private static final Pattern VALID_NAME = Pattern.compile("^[a-zA-Z0-9_]{3,20}$");
    private static final DateTimeFormatter FILE_TIME = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS");

    public void createAccount(String username, String password) throws MailException, IOException {
        createAccount(username, password, "127.0.0.1");
    }

    public void createAccount(String username, String password, String clientIp) throws MailException, IOException {
        Path folder = resolveFolder(username);
        validatePassword(password);
        if (Files.exists(folder)) {
            throw new MailException("Tài khoản đã tồn tại");
        }
        Files.createDirectories(folder);
        Files.createDirectories(ACCOUNT_ROOT);
        saveAccount(username, password, clientIp);

        String effectiveIp = (clientIp != null && !clientIp.isBlank()) ? clientIp : "127.0.0.1";
        String welcomeContent = "Người gửi: System\n"
                + "IP người gửi: Server (" + effectiveIp + ")\n"
                + "Tiêu đề: Chào mừng bạn gia nhập hệ thống Mail\n"
                + "Thời gian: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + "\n"
                + "--------------------------------------------------\n"
                + WELCOME_TEXT;

        Files.write(folder.resolve(WELCOME_FILE), welcomeContent.getBytes(StandardCharsets.UTF_8));
    }

    public void sendMail(String from, String password, String to, String content)
            throws MailException, IOException {
        sendMail(from, password, to, "Không có tiêu đề", content, "127.0.0.1");
    }

    public void sendMail(String from, String password, String to, String subject, String content, String senderIp)
            throws MailException, IOException {
        authenticate(from, password);
        Path receiverFolder = requireExistingAccount(to);
        if (content == null || content.isBlank()) {
            throw new MailException("Nội dung email không được để trống");
        }
        if (subject == null || subject.isBlank()) {
            subject = "Không có tiêu đề";
        }

        String effectiveSenderIp = (senderIp != null && !senderIp.isBlank()) ? senderIp : "127.0.0.1";
        String fileName = LocalDateTime.now().format(FILE_TIME) + "_from_" + from + ".txt";

        String fileContent = "Người gửi: " + from + "\n"
                + "IP người gửi: " + effectiveSenderIp + "\n"
                + "Tiêu đề: " + subject + "\n"
                + "Thời gian: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + "\n"
                + "--------------------------------------------------\n"
                + content;

        Files.write(receiverFolder.resolve(fileName), fileContent.getBytes(StandardCharsets.UTF_8));
    }

    public List<String> listFiles(String username, String password) throws MailException, IOException {
        Path folder = authenticate(username, password);
        try (Stream<Path> files = Files.list(folder)) {
            return files.map(file -> file.getFileName().toString())
                    .sorted()
                    .collect(Collectors.toList());
        } catch (UncheckedIOException e) {
            throw e.getCause();
        }
    }

    public String readFile(String username, String password, String fileName)
            throws MailException, IOException {
        Path folder = authenticate(username, password);
        Path file = folder.resolve(fileName).normalize();

        if (!folder.equals(file.getParent()) || !Files.isRegularFile(file)) {
            throw new MailException("File không tồn tại: " + fileName);
        }
        return new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
    }

    private Path authenticate(String username, String password) throws MailException, IOException {
        Path folder = resolveFolder(username);
        Path accountFile = passwordFile(username);
        boolean valid = Files.isDirectory(folder)
                && Files.isRegularFile(accountFile)
                && readLines(accountFile).stream()
                .anyMatch(line -> line.equals(PASSWORD_KEY + password));
        if (!valid) {
            throw new MailException("Sai tài khoản hoặc mật khẩu");
        }
        return folder;
    }

    private Path requireExistingAccount(String username) throws MailException {
        Path folder = resolveFolder(username);
        if (!Files.isDirectory(folder)) {
            throw new MailException("Tài khoản không tồn tại: " + username);
        }
        return folder;
    }

    private void validatePassword(String password) throws MailException {
        if (password == null || password.isBlank()) {
            throw new MailException("Mật khẩu không được để trống");
        }
    }

    private Path resolveFolder(String username) throws MailException {
        if (username == null || !VALID_NAME.matcher(username).matches()) {
            throw new MailException("Tên tài khoản chỉ gồm chữ, số, dấu _ (3-20 ký tự)");
        }
        return MAIL_ROOT.resolve(username);
    }

    private Path passwordFile(String username) {
        return ACCOUNT_ROOT.resolve(username + ".txt");
    }

    private void saveAccount(String username, String password, String ip) throws IOException {
        List<String> lines = Arrays.asList(
                "username=" + username,
                PASSWORD_KEY + password,
                "ip=" + (ip != null && !ip.isBlank() ? ip : "127.0.0.1"),
                "createdAt=" + LocalDateTime.now());
        Files.write(passwordFile(username), lines, StandardCharsets.UTF_8);
    }

    public String getAccountIp(String username) {
        try {
            Path accountFile = passwordFile(username);
            if (!Files.isRegularFile(accountFile)) {
                return "";
            }
            return readLines(accountFile).stream()
                    .filter(line -> line.startsWith("ip="))
                    .map(line -> line.substring("ip=".length()).trim())
                    .findFirst()
                    .orElse("");
        } catch (IOException e) {
            return "";
        }
    }

    public void updateAccountIp(String username, String ip) {
        if (ip == null || ip.isBlank()) {
            return;
        }
        try {
            Path accountFile = passwordFile(username);
            if (Files.isRegularFile(accountFile)) {
                List<String> lines = readLines(accountFile);
                boolean found = false;
                List<String> updated = new java.util.ArrayList<>();
                for (String line : lines) {
                    if (line.startsWith("ip=")) {
                        updated.add("ip=" + ip);
                        found = true;
                    } else {
                        updated.add(line);
                    }
                }
                if (!found) {
                    updated.add("ip=" + ip);
                }
                Files.write(accountFile, updated, StandardCharsets.UTF_8);
            }
        } catch (IOException ignored) {
        }
    }

    private List<String> readLines(Path file) throws IOException {
        return Files.readAllLines(file, StandardCharsets.UTF_8);
    }
}