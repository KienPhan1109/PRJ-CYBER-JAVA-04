package com.cyber.domain.fb.discount;

import java.math.BigDecimal;

public interface IDiscountStrategy {
    BigDecimal applyDiscount(BigDecimal originalPrice);

    String getStrategyName();

    default BigDecimal calculateDiscountAmount(BigDecimal originalPrice) {
        if (originalPrice == null || originalPrice.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return originalPrice.subtract(applyDiscount(originalPrice));
    }
}
