package com.cyber.domain.fb.discount;

import java.math.BigDecimal;

/**
 * STRATEGY PATTERN — Concrete Strategy: Giảm giá số tiền cố định.
 * Phù hợp cho: Voucher giảm 20.000đ, ưu đãi sinh nhật giảm 50.000đ, v.v.
 *
 * <p>Cách dùng:
 * <pre>
 *   IDiscountStrategy voucher = new FixedAmountDiscountStrategy(new BigDecimal("20000"), "VOUCHER_20K");
 *   BigDecimal finalPrice = voucher.applyDiscount(new BigDecimal("80000"));
 *   // finalPrice = 60.000đ (giảm 20.000đ)
 * </pre>
 * </p>
 */
public class FixedAmountDiscountStrategy implements IDiscountStrategy {

    private final BigDecimal discountAmount;  // Số tiền giảm cố định (VD: 20.000đ)
    private final String     strategyName;

    /**
     * @param discountAmount Số tiền giảm (> 0)
     * @param strategyName   Tên chiến lược để lưu DB (VD: "VOUCHER_20K")
     */
    public FixedAmountDiscountStrategy(BigDecimal discountAmount, String strategyName) {
        if (discountAmount == null || discountAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("discountAmount phải > 0.");
        }
        this.discountAmount = discountAmount;
        this.strategyName   = (strategyName != null && !strategyName.isBlank())
                              ? strategyName
                              : "FIXED_" + discountAmount.toPlainString() + "VND";
    }

    /**
     * Giá sau giảm = max(0, originalPrice - discountAmount).
     * Đảm bảo không âm (VD: món 10.000đ dùng voucher 20.000đ => thanh toán 0đ).
     */
    @Override
    public BigDecimal applyDiscount(BigDecimal originalPrice) {
        if (originalPrice == null || originalPrice.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal result = originalPrice.subtract(discountAmount);
        return result.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : result;
    }

    @Override
    public String getStrategyName() {
        return strategyName;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }
}
