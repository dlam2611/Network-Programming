package service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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

    private static final Pattern VALID_NAME = Pattern.compile("^[a-zA-Z0-9_]{3,20}$");
    private static final Pattern VALID_PASSWORD = Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d)[^\\s|]{6,20}$");
    private static final DateTimeFormatter FILE_TIME = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS");

    public void createAccount(String username, String password) throws MailException, IOException {
        Path folder = resolveFolder(username);
//        validatePassword(password);
        if (Files.exists(folder)) {
            throw new MailException("Tài khoản đã tồn tại");
        }
        Files.createDirectories(folder);
        Files.createDirectories(ACCOUNT_ROOT);
        Files.write(passwordFile(username), password.getBytes(StandardCharsets.UTF_8));
        Files.write(folder.resolve(WELCOME_FILE), WELCOME_TEXT.getBytes(StandardCharsets.UTF_8));
    }

    public void sendMail(String from, String password, String to, String content)
            throws MailException, IOException {
        authenticate(from, password);
        Path receiverFolder = requireExistingAccount(to);
        if (content.isBlank()) {
            throw new MailException("Nội dung email không được để trống");
        }
        String fileName = LocalDateTime.now().format(FILE_TIME) + "_from_" + from + ".txt";
        Files.write(receiverFolder.resolve(fileName), content.getBytes(StandardCharsets.UTF_8));
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
        Path passwordFile = passwordFile(username);
        boolean valid = Files.isDirectory(folder)
                && Files.isRegularFile(passwordFile)
                && new String(Files.readAllBytes(passwordFile), StandardCharsets.UTF_8).equals(password);
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
        if (password == null) {

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
}