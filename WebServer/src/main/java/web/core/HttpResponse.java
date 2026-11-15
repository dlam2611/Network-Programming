package web.core;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/** Một response HTTP. Body là mảng byte, hoặc một đoạn của file (để gửi file lớn và Range). */
public class HttpResponse {
    public final int status;
    public final Map<String, String> headers = new LinkedHashMap<>();
    public byte[] body = new byte[0];
    public File file;            // nếu khác null thì gửi đoạn [offset, offset+length) của file
    public long offset, length;
    public String note = "";     // ghi chú hiển thị ở bảng log

    public HttpResponse(int status) { this.status = status; }

    public HttpResponse header(String k, String v) {
        headers.put(k, v);
        return this;
    }

    public static HttpResponse html(int status, String html) {
        HttpResponse r = new HttpResponse(status);
        r.body = html.getBytes(StandardCharsets.UTF_8);
        r.header("Content-Type", "text/html; charset=utf-8");
        return r;
    }

    public static HttpResponse error(int status, String detail) {
        String t = status + " " + reason(status);
        HttpResponse r = html(status, "<!DOCTYPE html><html><head><meta charset='utf-8'><title>" + t + "</title></head>"
                + "<body style='font-family:Segoe UI,sans-serif;text-align:center;margin-top:80px'>"
                + "<h1>" + t + "</h1><p>" + esc(detail) + "</p><a href='/'>Về trang chủ</a></body></html>");
        r.note = t;
        return r;
    }

    public static String esc(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    public static String reason(int s) {
        switch (s) {
            case 200: return "OK";
            case 206: return "Partial Content";
            case 400: return "Bad Request";
            case 403: return "Forbidden";
            case 404: return "Not Found";
            case 405: return "Method Not Allowed";
            case 416: return "Range Not Satisfiable";
            default: return "Internal Server Error";
        }
    }

    /** Gửi response. Với HEAD chỉ gửi header, không gửi body. */
    public void send(OutputStream out, boolean headOnly) throws IOException {
        long len = file != null ? length : body.length;
        StringBuilder sb = new StringBuilder();
        sb.append("HTTP/1.1 ").append(status).append(' ').append(reason(status)).append("\r\n");
        sb.append("Server: JavaWebServer\r\n");
        sb.append("Date: ").append(DateTimeFormatter.RFC_1123_DATE_TIME.format(ZonedDateTime.now(ZoneOffset.UTC))).append("\r\n");
        sb.append("Connection: close\r\n");
        sb.append("Content-Length: ").append(len).append("\r\n");
        for (Map.Entry<String, String> h : headers.entrySet())
            sb.append(h.getKey()).append(": ").append(h.getValue()).append("\r\n");
        sb.append("\r\n");
        out.write(sb.toString().getBytes(StandardCharsets.ISO_8859_1));
        if (headOnly) return;

        if (file != null) {
            try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
                raf.seek(offset);
                byte[] buf = new byte[16384];
                long left = length;
                while (left > 0) {
                    int n = raf.read(buf, 0, (int) Math.min(buf.length, left));
                    if (n < 0) break;
                    out.write(buf, 0, n);
                    left -= n;
                }
            }
        } else {
            out.write(body);
        }
    }
}