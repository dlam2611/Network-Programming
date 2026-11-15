package web.core;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/** Phục vụ file tĩnh trong thư mục www: Content-Type, Range (mp3/mp4), download, chống ../ */
public class StaticFileService {
    private final File root;

    public StaticFileService(File root) { this.root = root; }

    public HttpResponse serve(HttpRequest req) throws IOException {
        String rootPath = root.getCanonicalPath();
        File f = new File(root, req.path).getCanonicalFile();

        // chặn truy cập ra ngoài thư mục www (ví dụ /../../abc)
        if (!f.getPath().equals(rootPath) && !f.getPath().startsWith(rootPath + File.separator))
            return HttpResponse.error(403, "Không được phép truy cập đường dẫn này");
        if (f.isDirectory()) f = new File(f, "index.html");
        if (!f.isFile()) return HttpResponse.error(404, "Không tìm thấy " + req.path);

        long len = f.length();
        long start = 0, end = len - 1;
        boolean partial = false;

        String range = req.headers.get("range");
        if (len > 0 && range != null && range.startsWith("bytes=")) {
            try {
                String[] r = range.substring(6).split(",")[0].trim().split("-", 2);
                if (r[0].isEmpty()) {                        // bytes=-500 : 500 byte cuối
                    start = Math.max(0, len - Long.parseLong(r[1]));
                } else {
                    start = Long.parseLong(r[0]);
                    if (r.length > 1 && !r[1].isEmpty()) end = Math.min(end, Long.parseLong(r[1]));
                }
                if (start > end || start >= len) {
                    HttpResponse bad = HttpResponse.error(416, "Range không hợp lệ");
                    return bad.header("Content-Range", "bytes */" + len);
                }
                partial = true;
            } catch (NumberFormatException ignored) {
                start = 0; end = len - 1;                    // Range lỗi: gửi cả file
            }
        }

        boolean download = "1".equals(req.param("download"));
        HttpResponse res = new HttpResponse(partial ? 206 : 200);
        res.header("Content-Type", MimeTypes.of(f.getName()));
        res.header("Accept-Ranges", "bytes");
        if (partial) res.header("Content-Range", "bytes " + start + "-" + end + "/" + len);
        if (download) res.header("Content-Disposition", disposition(f.getName()));
        res.file = f;
        res.offset = start;
        res.length = len == 0 ? 0 : end - start + 1;
        res.note = (download ? "Tải về " : partial ? "Phát đoạn " + start + "-" + end + " của " : "Gửi file ")
                + f.getName() + " (" + len / 1024 + " KB)";
        return res;
    }

    /** Trang /files: liệt kê file trong www/files kèm link mở, tải về và trình phát mp3, mp4. */
    public HttpResponse listing() {
        File[] fs = new File(root, "files").listFiles(File::isFile);
        StringBuilder sb = new StringBuilder("<!DOCTYPE html><html><head><meta charset='utf-8'><title>Danh sách file</title>"
                + "<style>body{font-family:Segoe UI,sans-serif;margin:30px;background:#fff;color:#222}"
                + ".item{border:1px solid #ddd;border-radius:8px;padding:12px;margin:10px 0}a{color:#0084ff}</style></head>"
                + "<body><h2>📁 Danh sách file</h2><a href='/'>← Trang chủ</a>");
        if (fs == null || fs.length == 0) {
            sb.append("<p>Chưa có file nào. Hãy bỏ file .pdf, .mp3, .mp4 vào thư mục <b>www/files</b>.</p>");
        } else {
            Arrays.sort(fs);
            for (File f : fs) {
                String name = f.getName();
                String url = "/files/" + URLEncoder.encode(name, StandardCharsets.UTF_8).replace("+", "%20");
                String lower = name.toLowerCase();
                sb.append("<div class='item'><b>").append(HttpResponse.esc(name)).append("</b> (")
                        .append(f.length() / 1024).append(" KB) &nbsp; <a href='").append(url).append("'>Mở</a> | <a href='")
                        .append(url).append("?download=1'>Tải về</a>");
                if (lower.endsWith(".mp3"))
                    sb.append("<br><audio controls preload='none' src='").append(url).append("'></audio>");
                if (lower.endsWith(".mp4") || lower.endsWith(".webm"))
                    sb.append("<br><video controls preload='none' width='480' src='").append(url).append("'></video>");
                sb.append("</div>");
            }
        }
        sb.append("</body></html>");
        HttpResponse r = HttpResponse.html(200, sb.toString());
        r.note = "Danh sách file";
        return r;
    }

    private static String disposition(String name) {
        String ascii = name.replaceAll("[^\\x20-\\x7E]", "_").replace("\"", "_");
        String enc = URLEncoder.encode(name, StandardCharsets.UTF_8).replace("+", "%20");
        return "attachment; filename=\"" + ascii + "\"; filename*=UTF-8''" + enc;
    }
}