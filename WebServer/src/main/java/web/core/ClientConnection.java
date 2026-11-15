package web.core;

import java.io.*;
import java.net.Socket;
import java.util.LinkedHashMap;
import java.util.Map;

/** Xử lý một kết nối: đọc request, chọn dịch vụ theo method, gửi response, ghi log. */
class ClientConnection implements Runnable {
    private final Socket socket;
    private final StaticFileService files;
    private final WebListener listener;

    ClientConnection(Socket socket, StaticFileService files, WebListener listener) {
        this.socket = socket;
        this.files = files;
        this.listener = listener;
    }

    @Override public void run() {
        String ip = socket.getInetAddress().getHostAddress();
        try (Socket s = socket) {
            s.setSoTimeout(5000);
            InputStream in = new BufferedInputStream(s.getInputStream());
            OutputStream out = new BufferedOutputStream(s.getOutputStream());

            HttpRequest req = null;
            HttpResponse res;
            try {
                req = HttpRequest.parse(in);
                if (req == null) return;            // kết nối rỗng
                res = handle(req);
            } catch (IllegalArgumentException e) {
                res = HttpResponse.error(400, e.getMessage());
            }

            try {
                res.send(out, req != null && "HEAD".equals(req.method));
                out.flush();
            } catch (IOException ignored) {
                // trình duyệt tự ngắt (hay gặp khi tua video)
            }
            listener.onRequest(ip, req == null ? "-" : req.method,
                    req == null ? "-" : req.rawTarget, res.status, res.note);
        } catch (IOException ignored) {
            // timeout hoặc client đóng kết nối
        }
    }

    private HttpResponse handle(HttpRequest req) {
        try {
            switch (req.method) {
                case "GET":
                case "HEAD":
                    return req.path.equals("/files") ? files.listing() : files.serve(req);
                case "POST":
                    return req.path.equals("/submit")
                            ? handleSubmit(req)
                            : HttpResponse.error(404, "Không có dịch vụ POST tại " + req.path);
                default:
                    return HttpResponse.error(405, "Chỉ hỗ trợ GET, HEAD, POST")
                            .header("Allow", "GET, HEAD, POST");
            }
        } catch (Exception e) {
            return HttpResponse.error(500, e.getMessage() == null ? e.toString() : e.getMessage());
        }
    }

    /** POST /submit: nhận dữ liệu form và hiển thị lại. */
    private HttpResponse handleSubmit(HttpRequest req) {
        Map<String, String> form = req.form();
        StringBuilder sb = new StringBuilder("<!DOCTYPE html><html><head><meta charset='utf-8'><title>POST</title></head>"
                + "<body style='font-family:Segoe UI,sans-serif;margin:40px'>"
                + "<h2>Server đã nhận dữ liệu POST</h2><ul>");
        for (Map.Entry<String, String> e : form.entrySet())
            sb.append("<li><b>").append(HttpResponse.esc(e.getKey())).append("</b>: ")
                    .append(HttpResponse.esc(e.getValue())).append("</li>");
        sb.append("</ul><a href='/'>← Về trang chủ</a></body></html>");
        HttpResponse r = HttpResponse.html(200, sb.toString());
        r.note = "Nhận POST: " + new LinkedHashMap<>(form);
        return r;
    }
}