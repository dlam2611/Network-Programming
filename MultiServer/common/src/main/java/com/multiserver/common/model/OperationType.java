package com.multiserver.common.model;

/**
 * Các loại thao tác của hệ thống ngân hàng phân tán.
 */
public enum OperationType {
    REGISTER("ĐĂNG KÝ"),
    LOGIN("ĐĂNG NHẬP"),
    DEPOSIT("NẠP TIỀN"),
    WITHDRAW("RÚT TIỀN"),
    TRANSFER("CHUYỂN KHOẢN"),
    SYNC_REGISTER("ĐỒNG BỘ: ĐĂNG KÝ"),
    SYNC_BALANCE("ĐỒNG BỘ: SỐ DƯ"),
    SYNC_TRANSFER("ĐỒNG BỘ: CHUYỂN KHOẢN"),
    SYNC_DELETE("ĐỒNG BỘ: XÓA"),
    SYNC_FULL("ĐỒNG BỘ: TOÀN BỘ"),
    DB_MANAGEMENT("QUẢN TRỊ CSDL");

    private final String displayName;

    OperationType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}

