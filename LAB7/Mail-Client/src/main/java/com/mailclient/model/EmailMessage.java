package com.mailclient.model;

import java.util.*;

/**
 * Đại diện cho một bức thư điện tử (Email).
 */
public class EmailMessage {
    private int id; // Số thứ tự trong hộp thư (đối với POP3)
    private String from = "";
    private List<String> to = new ArrayList<>();
    private List<String> cc = new ArrayList<>();
    private List<String> bcc = new ArrayList<>();
    private String subject = "";
    private String date = "";
    private String body = "";
    private String htmlBody = "";
    private List<EmailAttachment> attachments = new ArrayList<>();
    private long size = 0;
    private String rawContent = "";
    private Map<String, String> headers = new LinkedHashMap<>();

    public EmailMessage() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from != null ? from.trim() : "";
    }

    public List<String> getTo() {
        return to;
    }

    public void setTo(List<String> to) {
        this.to = to != null ? to : new ArrayList<>();
    }

    public void addTo(String recipient) {
        if (recipient != null && !recipient.trim().isEmpty()) {
            this.to.add(recipient.trim());
        }
    }

    public List<String> getCc() {
        return cc;
    }

    public void setCc(List<String> cc) {
        this.cc = cc != null ? cc : new ArrayList<>();
    }

    public void addCc(String recipient) {
        if (recipient != null && !recipient.trim().isEmpty()) {
            this.cc.add(recipient.trim());
        }
    }

    public List<String> getBcc() {
        return bcc;
    }

    public void setBcc(List<String> bcc) {
        this.bcc = bcc != null ? bcc : new ArrayList<>();
    }

    public void addBcc(String recipient) {
        if (recipient != null && !recipient.trim().isEmpty()) {
            this.bcc.add(recipient.trim());
        }
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject != null ? subject.trim() : "";
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date != null ? date.trim() : "";
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body != null ? body : "";
    }

    public String getHtmlBody() {
        return htmlBody;
    }

    public void setHtmlBody(String htmlBody) {
        this.htmlBody = htmlBody != null ? htmlBody : "";
    }

    public List<EmailAttachment> getAttachments() {
        return attachments;
    }

    public void setAttachments(List<EmailAttachment> attachments) {
        this.attachments = attachments != null ? attachments : new ArrayList<>();
    }

    public void addAttachment(EmailAttachment attachment) {
        if (attachment != null) {
            this.attachments.add(attachment);
        }
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public String getRawContent() {
        return rawContent;
    }

    public void setRawContent(String rawContent) {
        this.rawContent = rawContent != null ? rawContent : "";
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers != null ? headers : new LinkedHashMap<>();
    }

    public String getFormattedSize() {
        long bytes = size > 0 ? size : (rawContent != null ? rawContent.length() : 0);
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        char pre = "KMGTPE".charAt(exp - 1);
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }
}
