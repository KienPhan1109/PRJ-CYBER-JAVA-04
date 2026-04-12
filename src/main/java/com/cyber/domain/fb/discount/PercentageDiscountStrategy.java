package com.cyber.domain.fb.discount;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class PercentageDiscountStrategy implements IDiscountStrategy {

    private final BigDecimal discountPercent;
    private final String     strategyName;

    public PercentageDiscountStrategy(double discountPercent, String strategyName) {
        if (discountPercent <= 0 || discountPercent > 100) {
            throw new IllegalArgumentException("discountPercent phải nằm trong khoảng (0, 100].");
        }
        this.discountPercent = BigDecimal.valueOf(discountPercent);
        this.strategyName    = (strategyName != null && !strategyName.isBlank())
                               ? strategyName
                               : "PERCENTAGE_" + discountPercent + "_PERCENT";
    }

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
}
