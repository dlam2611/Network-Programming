package web.core;

import java.util.HashMap;
import java.util.Map;

/** Đuôi file → Content-Type. */
public final class MimeTypes {
    private MimeTypes() { }

    private static final Map<String, String> MAP = new HashMap<>();
    static {
        String[][] t = {
                {"html", "text/html; charset=utf-8"}, {"htm", "text/html; charset=utf-8"},
                {"css", "text/css; charset=utf-8"}, {"js", "text/javascript; charset=utf-8"},
                {"json", "application/json; charset=utf-8"}, {"txt", "text/plain; charset=utf-8"},
                {"png", "image/png"}, {"jpg", "image/jpeg"}, {"jpeg", "image/jpeg"},
                {"gif", "image/gif"}, {"svg", "image/svg+xml"}, {"ico", "image/x-icon"},
                {"pdf", "application/pdf"}, {"mp3", "audio/mpeg"}, {"wav", "audio/wav"},
                {"mp4", "video/mp4"}, {"webm", "video/webm"}, {"zip", "application/zip"},
                {"doc", "application/msword"},
                {"docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"}
        };
        for (String[] e : t) MAP.put(e[0], e[1]);
    }

    public static String of(String fileName) {
        int i = fileName.lastIndexOf('.');
        String ext = i < 0 ? "" : fileName.substring(i + 1).toLowerCase();
        return MAP.getOrDefault(ext, "application/octet-stream");
    }
}