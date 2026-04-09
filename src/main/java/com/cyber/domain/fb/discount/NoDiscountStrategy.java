package com.cyber.domain.fb.discount;

import java.math.BigDecimal;

/**
 * STRATEGY PATTERN — Concrete Strategy: Không giảm giá.
 * Dùng cho khách hàng thường (không có thẻ VIP, không trong Happy Hour).
 * Áp dụng Null Object Pattern — tránh kiểm tra null.
 */
public class NoDiscountStrategy implements IDiscountStrategy {

    /** Singleton vì không có trạng thái. */
    private static final NoDiscountStrategy INSTANCE = new NoDiscountStrategy();

    private NoDiscountStrategy() {}

    public static NoDiscountStrategy getInstance() {
        return INSTANCE;
    }

    /** Trả về giá gốc, không thay đổi. */
    @Override
    public BigDecimal applyDiscount(BigDecimal originalPrice) {
        return (originalPrice != null) ? originalPrice : BigDecimal.ZERO;
    }

    @Override
    public String getStrategyName() {
        return "NO_DISCOUNT";
    }
}
