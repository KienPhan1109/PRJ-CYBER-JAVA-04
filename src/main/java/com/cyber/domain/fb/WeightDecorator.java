package com.cyber.domain.fb;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * DECORATOR PATTERN — Concrete Decorator: Weight (theo gram).
 * Tính giá dựa trên số gram thay vì giá cố định.
 * Phù hợp với các món bán theo trọng lượng (VD: thịt nướng, hải sản, snack đóng gói).
 *
 * <p>Cách tính: Giá tổng = pricePerGram × weightInGrams</p>
 *
 * <p>Cách dùng:
 * <pre>
 *   IBillable thit = new SingleItem(thitNuongItem);            // basePrice = 1 (giá mỗi gram)
 *   thit = new WeightDecorator(thit, new BigDecimal("1.5"),    // pricePerGram = 1.500đ/g
 *                              new BigDecimal("200"));         // 200 gram
 *   // Tổng = 1.500 × 200 = 300.000đ
 * </pre>
 * </p>
 */
public class WeightDecorator extends ItemDecorator {

    private final BigDecimal pricePerGram;   // Giá mỗi gram (VD: 1.500đ/gram)
    private final BigDecimal weightInGrams;  // Số gram khách đặt

    /**
     * @param wrappee        Đối tượng IBillable gốc (basePrice thường là 0 hoặc giá nền)
     * @param pricePerGram   Giá mỗi gram (> 0)
     * @param weightInGrams  Số gram khách đặt (> 0)
     */
    public WeightDecorator(IBillable wrappee, BigDecimal pricePerGram, BigDecimal weightInGrams) {
        super(wrappee);
        if (pricePerGram == null || pricePerGram.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("pricePerGram phải > 0.");
        }
        if (weightInGrams == null || weightInGrams.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("weightInGrams phải > 0.");
        }
        this.pricePerGram  = pricePerGram;
        this.weightInGrams = weightInGrams;
    }

    /**
     * Giá = pricePerGram × weightInGrams, làm tròn đến 0 chữ số thập phân.
     * Không cộng thêm wrappee.calculatePrice() vì WeightDecorator thay thế hoàn toàn cách tính giá.
     */
    @Override
    public BigDecimal calculatePrice() {
        return pricePerGram.multiply(weightInGrams).setScale(0, RoundingMode.HALF_UP);
    }

    /**
     * Mô tả = "... × 200g (1.500đ/g)".
     */
    @Override
    public String getDescription() {
        return wrappee.getDescription()
                + " × " + String.format("%,.0f", weightInGrams) + "g"
                + " (" + String.format("%,.0f", pricePerGram) + "đ/g)";
    }

    public BigDecimal getPricePerGram() { return pricePerGram; }
    public BigDecimal getWeightInGrams() { return weightInGrams; }
}
