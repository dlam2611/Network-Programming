package web.core;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/** Một request HTTP đã được phân tích. */
public class HttpRequest {
    private static final int MAX_BODY = 10 * 1024 * 1024;

    public String method, rawTarget, path, query = "", version;
    public final Map<String, String> headers = new HashMap<>();   // khóa viết thường
    public byte[] body = new byte[0];

    /** @return null nếu kết nối rỗng. Ném IllegalArgumentException nếu request sai định dạng. */
    public static HttpRequest parse(InputStream in) throws IOException {
        String line = readLine(in);
        if (line == null || line.isEmpty()) return null;
        String[] p = line.split(" ");
        if (p.length != 3 || !p[2].startsWith("HTTP/"))
            throw new IllegalArgumentException("Dòng yêu cầu không hợp lệ");

        HttpRequest r = new HttpRequest();
        r.method = p[0].toUpperCase();
        r.rawTarget = p[1];
        r.version = p[2];
        int q = p[1].indexOf('?');
        String rawPath = q < 0 ? p[1] : p[1].substring(0, q);
        r.query = q < 0 ? "" : p[1].substring(q + 1);
        r.path = URLDecoder.decode(rawPath.replace("+", "%2B"), StandardCharsets.UTF_8);

        while ((line = readLine(in)) != null && !line.isEmpty()) {
            int i = line.indexOf(':');
            if (i > 0) r.headers.put(line.substring(0, i).trim().toLowerCase(), line.substring(i + 1).trim());
        }

        String cl = r.headers.get("content-length");
        if (cl != null) {
            int n;
            try { n = Integer.parseInt(cl); } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Content-Length không hợp lệ");
            }
            if (n < 0 || n > MAX_BODY) throw new IllegalArgumentException("Nội dung gửi lên quá lớn");
            r.body = in.readNBytes(n);
        }
        return r;
    }

    private static String readLine(InputStream in) throws IOException {
        StringBuilder sb = new StringBuilder();
        int c;
        while ((c = in.read()) != -1) {
            if (c == '\n') break;
            if (c != '\r') sb.append((char) c);
            if (sb.length() > 8192) throw new IllegalArgumentException("Dòng quá dài");
        }
        if (c == -1 && sb.length() == 0) return null;
        return sb.toString();
    }

    public String param(String name) { return parseUrlEncoded(query).get(name); }

    /** Dữ liệu form (application/x-www-form-urlencoded) trong body của POST. */
    public Map<String, String> form() {
        return parseUrlEncoded(new String(body, StandardCharsets.UTF_8));
    }

    private static Map<String, String> parseUrlEncoded(String s) {
        Map<String, String> m = new LinkedHashMap<>();
        if (s.isEmpty()) return m;
        for (String pair : s.split("&")) {
            int i = pair.indexOf('=');
            String k = i < 0 ? pair : pair.substring(0, i);
            String v = i < 0 ? "" : pair.substring(i + 1);
            m.put(URLDecoder.decode(k, StandardCharsets.UTF_8), URLDecoder.decode(v, StandardCharsets.UTF_8));
        }
        return m;
    }
}