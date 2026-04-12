package com.cyber.domain.fb.discount;

import java.math.BigDecimal;

public class FixedAmountDiscountStrategy implements IDiscountStrategy {
    private final BigDecimal discountAmount;
    private final String     strategyName;

    public FixedAmountDiscountStrategy(BigDecimal discountAmount, String strategyName) {
        if (discountAmount == null || discountAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("discountAmount phải > 0.");
        }
        this.discountAmount = discountAmount;
        this.strategyName   = (strategyName != null && !strategyName.isBlank())
                              ? strategyName
                              : "FIXED_" + discountAmount.toPlainString() + "VND";
    }

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
}
