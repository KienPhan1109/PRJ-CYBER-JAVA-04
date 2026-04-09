package com.cyber.domain.fb.discount;

import java.math.BigDecimal;

/**
 * STRATEGY PATTERN — Strategy Interface: Discount.
 * Định nghĩa hợp đồng cho tất cả các chiến lược giảm giá.
 * Mỗi class implement sẽ mang một thuật toán giảm giá cụ thể.
 *
 * <p>Hệ thống Strategy:
 * <pre>
 *   IDiscountStrategy (interface)
 *   ├── NoDiscountStrategy            — Không giảm
 *   ├── PercentageDiscountStrategy    — Giảm theo %
 *   ├── FixedAmountDiscountStrategy   — Giảm số tiền cố định
 *   └── HappyHourDiscountStrategy     — Giảm nếu trong khung giờ Happy Hour
 * </pre>
 * </p>
 */
public interface IDiscountStrategy {

    /**
     * Áp dụng chiến lược giảm giá lên giá gốc.
     *
     * @param originalPrice Giá gốc trước khi giảm (không được null, phải >= 0)
     * @return Giá sau khi giảm (không bao giờ âm, không bao giờ > originalPrice)
     */
    BigDecimal applyDiscount(BigDecimal originalPrice);

    /**
     * Tên/mô tả của strategy để lưu xuống DB (cột discount_strategy_name).
     *
     * @return Chuỗi định danh strategy (VD: "VIP_10_PERCENT", "HAPPY_HOUR_15_PERCENT")
     */
    String getStrategyName();

    /**
     * Tính toán số tiền thực sự đã giảm (để lưu vào cột discount_applied).
     *
     * @param originalPrice Giá gốc
     * @return Số tiền được giảm (>= 0)
     */
    default BigDecimal calculateDiscountAmount(BigDecimal originalPrice) {
        if (originalPrice == null || originalPrice.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return originalPrice.subtract(applyDiscount(originalPrice));
    }
}
