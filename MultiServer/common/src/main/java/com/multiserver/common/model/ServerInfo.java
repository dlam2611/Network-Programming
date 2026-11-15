package com.multiserver.common.model;

import java.io.Serializable;
import java.util.Objects;

/**
 * Thông tin kết nối của một Server trong hệ thống phân tán.
 */
public class ServerInfo implements Serializable {
    private static final long serialVersionUID = 1L;

    private int id;
    private String name;
    private String host;
    private int port;

    public ServerInfo() {
    }

    public ServerInfo(int id, String name, String host, int port) {
        this.id = id;
        this.name = name;
        this.host = host;
        this.port = port;
    }

    public ServerInfo(String host, int port) {
        this(0, host + ":" + port, host, port);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ServerInfo that = (ServerInfo) o;
        return port == that.port && Objects.equals(host, that.host);
    }

    @Override
    public int hashCode() {
        return Objects.hash(host, port);
    }

    @Override
    public String toString() {
        return name + " (" + host + ":" + port + ")";
    }
}

