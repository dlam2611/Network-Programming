package com.mailclient.model;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Base64;

/**
 * Đại diện cho một tệp đính kèm trong Email.
 */
public class EmailAttachment {
    private String fileName;
    private String contentType;
    private byte[] data;

    public EmailAttachment(String fileName, String contentType, byte[] data) {
        this.fileName = fileName;
        this.contentType = contentType != null ? contentType : "application/octet-stream";
        this.data = data;
    }

    public static EmailAttachment fromFile(File file) throws IOException {
        String mimeType = Files.probeContentType(file.toPath());
        if (mimeType == null) {
            mimeType = "application/octet-stream";
        }
        byte[] bytes = new byte[(int) file.length()];
        try (FileInputStream fis = new FileInputStream(file)) {
            int read = 0;
            int offset = 0;
            while (offset < bytes.length && (read = fis.read(bytes, offset, bytes.length - offset)) >= 0) {
                offset += read;
            }
        }
        return new EmailAttachment(file.getName(), mimeType, bytes);
    }

    public String getFileName() {
        return fileName;
    }

    public String getContentType() {
        return contentType;
    }

    public byte[] getData() {
        return data;
    }

    public long getSize() {
        return data != null ? data.length : 0;
    }

    public String getBase64Data() {
        return data != null ? Base64.getEncoder().encodeToString(data) : "";
    }

    public String getFormattedSize() {
        long bytes = getSize();
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        char pre = "KMGTPE".charAt(exp - 1);
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }

    @Override
    public String toString() {
        return fileName + " (" + getFormattedSize() + ")";
    }
}
