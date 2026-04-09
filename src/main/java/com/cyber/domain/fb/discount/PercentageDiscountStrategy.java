package com.cyber.domain.fb.discount;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * STRATEGY PATTERN — Concrete Strategy: Giảm giá theo phần trăm (%).
 * Phù hợp cho: Thẻ VIP (10%), khuyến mãi cuối tuần (15%), v.v.
 *
 * <p>Cách dùng:
 * <pre>
 *   IDiscountStrategy vipDiscount = new PercentageDiscountStrategy(10, "VIP_10_PERCENT");
 *   BigDecimal finalPrice = vipDiscount.applyDiscount(new BigDecimal("80000"));
 *   // finalPrice = 72.000đ (giảm 10%)
 * </pre>
 * </p>
 */
public class PercentageDiscountStrategy implements IDiscountStrategy {

    private final BigDecimal discountPercent;   // VD: 10 => giảm 10%
    private final String     strategyName;

    /**
     * @param discountPercent Phần trăm giảm (0 < discountPercent <= 100)
     * @param strategyName    Tên chiến lược để lưu DB (VD: "VIP_10_PERCENT")
     */
    public PercentageDiscountStrategy(double discountPercent, String strategyName) {
        if (discountPercent <= 0 || discountPercent > 100) {
            throw new IllegalArgumentException("discountPercent phải nằm trong khoảng (0, 100].");
        }
        this.discountPercent = BigDecimal.valueOf(discountPercent);
        this.strategyName    = (strategyName != null && !strategyName.isBlank())
                               ? strategyName
                               : "PERCENTAGE_" + discountPercent + "_PERCENT";
    }

    /**
     * Giá sau giảm = originalPrice × (100 - discountPercent) / 100.
     * Làm tròn đến 0 chữ số thập phân (đơn vị đồng).
     */
    @Override
    public BigDecimal applyDiscount(BigDecimal originalPrice) {
        if (originalPrice == null || originalPrice.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal multiplier = BigDecimal.valueOf(100)
                .subtract(discountPercent)
                .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        return originalPrice.multiply(multiplier).setScale(0, RoundingMode.HALF_UP);
    }

    @Override
    public String getStrategyName() {
        return strategyName;
    }

    public BigDecimal getDiscountPercent() {
        return discountPercent;
    }
}
