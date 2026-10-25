package com.mailclient.protocol.common;

import com.mailclient.model.EmailAttachment;
import com.mailclient.model.EmailMessage;

import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Trình phân tích cú pháp email thô (RFC 822 / RFC 2822 / MIME)
 * trích xuất tiêu đề, người gửi, người nhận, ngày gửi, thân thư (text/html)
 * và tệp đính kèm.
 */
public class EmailParser {

    private static final Pattern RFC2047_PATTERN = Pattern.compile("=\\?([^?]+)\\?([BQbq])\\?([^?]+)\\?=");

    public static EmailMessage parse(String rawEmail, int messageId, long rawSize) {
        EmailMessage email = new EmailMessage();
        email.setId(messageId);
        email.setSize(rawSize);
        email.setRawContent(rawEmail);

        if (rawEmail == null || rawEmail.isEmpty()) {
            return email;
        }

        // Tách phần Header và phần Body (ngăn cách bởi 2 ký tự xuống dòng liên tiếp)
        String headerSection;
        String bodySection;

        int headerEnd = rawEmail.indexOf("\r\n\r\n");
        if (headerEnd != -1) {
            headerSection = rawEmail.substring(0, headerEnd);
            bodySection = rawEmail.substring(headerEnd + 4);
        } else {
            headerEnd = rawEmail.indexOf("\n\n");
            if (headerEnd != -1) {
                headerSection = rawEmail.substring(0, headerEnd);
                bodySection = rawEmail.substring(headerEnd + 2);
            } else {
                headerSection = rawEmail;
                bodySection = "";
            }
        }

        // 1. Phân tích các headers (xử lý header folding)
        Map<String, String> headers = parseHeaders(headerSection);
        email.setHeaders(headers);

        // Đọc các trường header cơ bản
        email.setSubject(decodeMimeWords(headers.getOrDefault("subject", "(Không có tiêu đề)")));
        email.setFrom(decodeMimeWords(headers.getOrDefault("from", "Không rõ người gửi")));
        email.setDate(headers.getOrDefault("date", ""));

        String toStr = headers.get("to");
        if (toStr != null) {
            for (String t : toStr.split(",")) {
                email.addTo(decodeMimeWords(t.trim()));
            }
        }

        String ccStr = headers.get("cc");
        if (ccStr != null) {
            for (String c : ccStr.split(",")) {
                email.addCc(decodeMimeWords(c.trim()));
            }
        }

        // 2. Phân tích Body và Tệp đính kèm
        String contentType = headers.getOrDefault("content-type", "text/plain");
        String contentTransferEncoding = headers.getOrDefault("content-transfer-encoding", "7bit");

        if (contentType.toLowerCase().contains("multipart/")) {
            String boundary = extractParameter(contentType, "boundary");
            if (boundary != null) {
                parseMultipart(bodySection, boundary, email);
            } else {
                email.setBody(decodeTransferEncoding(bodySection, contentTransferEncoding));
            }
        } else {
            // Đơn phần (Single-part message)
            String decodedBody = decodeTransferEncoding(bodySection, contentTransferEncoding);
            if (contentType.toLowerCase().contains("text/html")) {
                email.setHtmlBody(decodedBody);
                email.setBody(stripHtml(decodedBody));
            } else {
                email.setBody(decodedBody);
            }
        }

        return email;
    }

    private static Map<String, String> parseHeaders(String headerSection) {
        Map<String, String> headers = new LinkedHashMap<>();
        String[] lines = headerSection.split("\r?\n");
        String currentKey = null;
        StringBuilder currentValue = new StringBuilder();

        for (String line : lines) {
            if (line.startsWith(" ") || line.startsWith("\t")) {
                // Header folding: Dòng tiếp nối của header trước
                if (currentKey != null) {
                    currentValue.append(" ").append(line.trim());
                }
            } else {
                if (currentKey != null) {
                    headers.put(currentKey.toLowerCase(), currentValue.toString());
                }
                int colonIdx = line.indexOf(':');
                if (colonIdx != -1) {
                    currentKey = line.substring(0, colonIdx).trim();
                    currentValue = new StringBuilder(line.substring(colonIdx + 1).trim());
                } else {
                    currentKey = null;
                }
            }
        }
        if (currentKey != null) {
            headers.put(currentKey.toLowerCase(), currentValue.toString());
        }

        return headers;
    }

    private static void parseMultipart(String body, String boundary, EmailMessage email) {
        String delimiter = "--" + boundary;
        String[] parts = body.split(Pattern.quote(delimiter));

        for (String part : parts) {
            part = part.trim();
            if (part.isEmpty() || part.equals("--")) continue; // Bỏ qua phần rỗng hoặc phần kết thúc

            int sep = part.indexOf("\r\n\r\n");
            int sepLen = 4;
            if (sep == -1) {
                sep = part.indexOf("\n\n");
                sepLen = 2;
            }

            if (sep == -1) continue;

            String partHeaderSection = part.substring(0, sep);
            String partBodySection = part.substring(sep + sepLen);

            Map<String, String> partHeaders = parseHeaders(partHeaderSection);
            String pContentType = partHeaders.getOrDefault("content-type", "text/plain");
            String pDisposition = partHeaders.getOrDefault("content-disposition", "");
            String pEncoding = partHeaders.getOrDefault("content-transfer-encoding", "7bit");

            // Kiểm tra xem part này có phải là tệp đính kèm không
            String fileName = extractParameter(pDisposition, "filename");
            if (fileName == null) {
                fileName = extractParameter(pContentType, "name");
            }

            if (fileName != null || pDisposition.toLowerCase().contains("attachment")) {
                // Tệp đính kèm
                String finalName = fileName != null ? decodeMimeWords(fileName) : "attachment_" + System.currentTimeMillis();
                byte[] data = decodeTransferEncodingToBytes(partBodySection, pEncoding);
                email.addAttachment(new EmailAttachment(finalName, pContentType.split(";")[0].trim(), data));
            } else if (pContentType.toLowerCase().contains("multipart/")) {
                // Xử lý multipart lồng nhau (nested multipart)
                String subBoundary = extractParameter(pContentType, "boundary");
                if (subBoundary != null) {
                    parseMultipart(partBodySection, subBoundary, email);
                }
            } else if (pContentType.toLowerCase().contains("text/html")) {
                String decoded = decodeTransferEncoding(partBodySection, pEncoding);
                email.setHtmlBody(decoded);
                if (email.getBody().isEmpty()) {
                    email.setBody(stripHtml(decoded));
                }
            } else if (pContentType.toLowerCase().contains("text/plain")) {
                String decoded = decodeTransferEncoding(partBodySection, pEncoding);
                email.setBody(decoded);
            }
        }
    }

    public static String extractParameter(String headerValue, String paramName) {
        if (headerValue == null) return null;
        Pattern p = Pattern.compile(paramName + "\\s*=\\s*\"?([^\";]+)\"?", Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(headerValue);
        if (m.find()) {
            return m.group(1).trim();
        }
        return null;
    }

    public static String decodeMimeWords(String text) {
        if (text == null) return "";
        Matcher m = RFC2047_PATTERN.matcher(text);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String charset = m.group(1);
            String encoding = m.group(2).toUpperCase();
            String encodedText = m.group(3);

            try {
                byte[] bytes;
                if ("B".equals(encoding)) {
                    bytes = Base64.getDecoder().decode(encodedText);
                } else {
                    bytes = decodeQuotedPrintable(encodedText.replace("_", " "));
                }
                Charset cs = Charset.forName(charset);
                m.appendReplacement(sb, Matcher.quoteReplacement(new String(bytes, cs)));
            } catch (Exception e) {
                m.appendReplacement(sb, Matcher.quoteReplacement(m.group(0)));
            }
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private static String decodeTransferEncoding(String content, String encoding) {
        byte[] bytes = decodeTransferEncodingToBytes(content, encoding);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private static byte[] decodeTransferEncodingToBytes(String content, String encoding) {
        if (content == null) return new byte[0];
        String enc = encoding != null ? encoding.trim().toLowerCase() : "";

        if (enc.contains("base64")) {
            // Loại bỏ khoảng trắng và xuống dòng trước khi giải mã Base64
            String clean = content.replaceAll("\\s+", "");
            try {
                return Base64.getDecoder().decode(clean);
            } catch (Exception e) {
                return content.getBytes(StandardCharsets.UTF_8);
            }
        } else if (enc.contains("quoted-printable")) {
            return decodeQuotedPrintable(content);
        } else {
            return content.getBytes(StandardCharsets.UTF_8);
        }
    }

    private static byte[] decodeQuotedPrintable(String s) {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '=') {
                if (i + 1 < s.length() && (s.charAt(i + 1) == '\r' || s.charAt(i + 1) == '\n')) {
                    // Soft line break
                    if (i + 2 < s.length() && s.charAt(i + 1) == '\r' && s.charAt(i + 2) == '\n') {
                        i += 2;
                    } else {
                        i++;
                    }
                } else if (i + 2 < s.length()) {
                    try {
                        int b = Integer.parseInt(s.substring(i + 1, i + 3), 16);
                        buffer.write(b);
                        i += 2;
                    } catch (NumberFormatException e) {
                        buffer.write('=');
                    }
                } else {
                    buffer.write('=');
                }
            } else {
                buffer.write((byte) c);
            }
        }
        return buffer.toByteArray();
    }

    private static String stripHtml(String html) {
        if (html == null) return "";
        return html.replaceAll("(?i)<br\\s*/?>", "\n")
                   .replaceAll("(?i)</p>", "\n\n")
                   .replaceAll("<[^>]+>", "")
                   .replace("&nbsp;", " ")
                   .replace("&lt;", "<")
                   .replace("&gt;", ">")
                   .replace("&amp;", "&")
                   .trim();
    }
}
