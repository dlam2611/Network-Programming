package com.multiserver.common.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class PasswordUtil {

    /**
     * Băm mật khẩu bằng SHA-256
     */
    public static String hashPassword(String rawPassword) {
        if (rawPassword == null) {
            return "";
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedHash = digest.digest(rawPassword.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : encodedHash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Lỗi thuật toán mã hóa SHA-256", e);
        }
    }

    /**
     * Kiểm tra mật khẩu (hỗ trợ cả trường hợp giáo viên/người dùng nhập trực tiếp text thường vào DB)
     */
    public static boolean checkPassword(String rawPassword, String storedPassword) {
        if (rawPassword == null || storedPassword == null) {
            return false;
        }
        // Trường hợp mật khẩu trong DB đã được hash SHA-256
        if (storedPassword.equalsIgnoreCase(hashPassword(rawPassword))) {
            return true;
        }
        // Trường hợp dữ liệu test thủ công bằng chuỗi thường trong DB
        return rawPassword.equals(storedPassword);
    }
}

