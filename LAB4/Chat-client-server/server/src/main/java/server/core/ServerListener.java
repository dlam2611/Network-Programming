package server.core;

public interface ServerListener {
    void onLog(String ip, String user, String action);
    void onOnlineChanged(int count);
}