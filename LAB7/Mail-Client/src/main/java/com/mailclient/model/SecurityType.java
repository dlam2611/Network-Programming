package com.mailclient.model;

/**
 * Các loại bảo mật kết nối mạng cho SMTP và POP3.
 */
public enum SecurityType {
    NONE("Không mã hóa (Plain TCP)"),
    SSL_TLS("SSL / TLS"),
    STARTTLS("STARTTLS (Nâng cấp bảo mật)");

    private final String displayName;

    SecurityType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
