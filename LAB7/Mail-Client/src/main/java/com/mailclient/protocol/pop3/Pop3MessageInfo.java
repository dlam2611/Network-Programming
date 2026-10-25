package com.mailclient.protocol.pop3;

/**
 * Thông tin một thư trong danh sách từ lệnh LIST của POP3.
 */
public class Pop3MessageInfo {
    private final int messageNumber;
    private final long octets;

    public Pop3MessageInfo(int messageNumber, long octets) {
        this.messageNumber = messageNumber;
        this.octets = octets;
    }

    public int getMessageNumber() {
        return messageNumber;
    }

    public long getOctets() {
        return octets;
    }

    public String getFormattedSize() {
        if (octets < 1024) return octets + " B";
        int exp = (int) (Math.log(octets) / Math.log(1024));
        char pre = "KMGTPE".charAt(exp - 1);
        return String.format("%.1f %sB", octets / Math.pow(1024, exp), pre);
    }

    @Override
    public String toString() {
        return "#" + messageNumber + " (" + getFormattedSize() + ")";
    }
}
