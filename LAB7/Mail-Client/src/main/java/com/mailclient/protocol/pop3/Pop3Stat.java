package com.mailclient.protocol.pop3;

/**
 * Thông tin thống kê từ lệnh STAT của POP3.
 */
public class Pop3Stat {
    private final int messageCount;
    private final long totalOctets;

    public Pop3Stat(int messageCount, long totalOctets) {
        this.messageCount = messageCount;
        this.totalOctets = totalOctets;
    }

    public int getMessageCount() {
        return messageCount;
    }

    public long getTotalOctets() {
        return totalOctets;
    }

    public String getFormattedSize() {
        if (totalOctets < 1024) return totalOctets + " B";
        int exp = (int) (Math.log(totalOctets) / Math.log(1024));
        char pre = "KMGTPE".charAt(exp - 1);
        return String.format("%.1f %sB", totalOctets / Math.pow(1024, exp), pre);
    }

    @Override
    public String toString() {
        return messageCount + " thư (" + getFormattedSize() + ")";
    }
}
