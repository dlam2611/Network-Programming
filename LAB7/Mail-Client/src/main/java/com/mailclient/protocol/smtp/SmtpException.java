package com.mailclient.protocol.smtp;

/**
 * Ngoại lệ xảy ra trong quá trình thực thi giao thức SMTP.
 */
public class SmtpException extends Exception {
    private int responseCode;

    public SmtpException(String message) {
        super(message);
    }

    public SmtpException(int responseCode, String message) {
        super("[" + responseCode + "] " + message);
        this.responseCode = responseCode;
    }

    public SmtpException(String message, Throwable cause) {
        super(message, cause);
    }

    public int getResponseCode() {
        return responseCode;
    }
}
