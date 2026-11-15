package web.core;

public interface WebListener {
    void onRequest(String ip, String method, String url, int status, String note);
    void onStatus(String text);
}