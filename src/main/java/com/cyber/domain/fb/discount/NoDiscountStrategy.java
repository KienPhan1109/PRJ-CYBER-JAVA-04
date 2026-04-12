package com.cyber.domain.fb.discount;

import java.math.BigDecimal;

public class NoDiscountStrategy implements IDiscountStrategy {
    private static final NoDiscountStrategy INSTANCE = new NoDiscountStrategy();

    private NoDiscountStrategy() {}

    public static NoDiscountStrategy getInstance() {
        return INSTANCE;
    }

    @Override
    public BigDecimal applyDiscount(BigDecimal originalPrice) {
        return (originalPrice != null) ? originalPrice : BigDecimal.ZERO;
    }

    @Override
    public String getStrategyName() {
        return "NO_DISCOUNT";
    }
}
