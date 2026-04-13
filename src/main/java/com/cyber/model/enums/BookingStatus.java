package com.cyber.model.enums;

/**
 * Các trạng thái của một phiên đặt máy (Booking).
 */
public enum BookingStatus {
    /** Đang chờ Staff phê duyệt (Khách hàng gửi yêu cầu) */
    PENDING,
    
    /** Đang trong phiên sử dụng tích cực */
    ACTIVE,
    
    /** Đã kết thúc và thanh toán xong */
    COMPLETED,
    
    /** Đã bị hủy (bởi Staff hoặc do quá hạn đặt trước) */
    CANCELLED,
    
    /** Đang được giữ chỗ (Đặt trước từ xa) */
    RESERVED
}
