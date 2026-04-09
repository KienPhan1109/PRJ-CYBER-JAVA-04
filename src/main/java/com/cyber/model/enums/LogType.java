package com.cyber.model.enums;

/**
 * Phân loại nhóm Audit Log.
 * USER     - Các hành động liên quan đến tài khoản người dùng (nạp tiền, khóa, tạo mới...).
 * COMPUTER - Các hành động liên quan đến máy trạm (thêm, sửa, xóa, đặt máy).
 * FB       - Các hành động liên quan đến F&B (đặt đơn, xác nhận, hủy đơn...).
 */
public enum LogType {
    USER,
    COMPUTER,
    FB
}
