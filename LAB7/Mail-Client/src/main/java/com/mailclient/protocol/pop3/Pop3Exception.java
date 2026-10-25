package com.mailclient.protocol.pop3;

/**
 * Ngoại lệ xảy ra trong quá trình thực thi giao thức POP3.
 */
public class Pop3Exception extends Exception {
    public Pop3Exception(String message) {
        super(message);
    }

    public Pop3Exception(String message, Throwable cause) {
        super(message, cause);
    }
}
