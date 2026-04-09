package com.cyber.domain.fb.discount;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * STRATEGY PATTERN — Concrete Strategy: Giảm giá Happy Hour.
 * Áp dụng giảm giá theo phần trăm chỉ khi time hiện tại nằm trong khung giờ Happy Hour.
 * Nếu ngoài giờ, không giảm (tương đương NoDiscountStrategy).
 *
 * <p>Cách dùng:
 * <pre>
 *   IDiscountStrategy happyHour = new HappyHourDiscountStrategy(
 *       LocalTime.of(14, 0),   // 14:00
 *       LocalTime.of(17, 0),   // 17:00
 *       15.0                   // Giảm 15%
 *   );
 *   // Nếu hiện tại là 15:30 => áp dụng giảm 15%
 *   // Nếu hiện tại là 19:00 => không giảm (trả về giá gốc)
 * </pre>
 * </p>
 */
public class HappyHourDiscountStrategy implements IDiscountStrategy {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private final LocalTime  startTime;          // Giờ bắt đầu Happy Hour
    private final LocalTime  endTime;            // Giờ kết thúc Happy Hour
    private final BigDecimal discountPercent;    // Phần trăm giảm khi trong Happy Hour

    /**
     * @param startTime       Giờ bắt đầu Happy Hour (VD: LocalTime.of(14,0))
     * @param endTime         Giờ kết thúc Happy Hour (VD: LocalTime.of(17,0))
     * @param discountPercent Phần trăm giảm (0 < x <= 100)
     */
    public HappyHourDiscountStrategy(LocalTime startTime, LocalTime endTime, double discountPercent) {
        if (startTime == null || endTime == null) {
            throw new IllegalArgumentException("startTime và endTime không được null.");
        }
        if (startTime.isAfter(endTime)) {
            throw new IllegalArgumentException("startTime phải trước endTime.");
        }
        if (discountPercent <= 0 || discountPercent > 100) {
            throw new IllegalArgumentException("discountPercent phải nằm trong khoảng (0, 100].");
        }
        this.startTime       = startTime;
        this.endTime         = endTime;
        this.discountPercent = BigDecimal.valueOf(discountPercent);
    }

    /**
     * Kiểm tra thời điểm hiện tại có trong khung giờ Happy Hour không.
     * Nếu có => áp dụng giảm phần trăm. Nếu không => trả về giá gốc.
     */
    @Override
    public BigDecimal applyDiscount(BigDecimal originalPrice) {
        if (originalPrice == null || originalPrice.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        if (isHappyHour()) {
            BigDecimal multiplier = BigDecimal.valueOf(100)
                    .subtract(discountPercent)
                    .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
            return originalPrice.multiply(multiplier).setScale(0, RoundingMode.HALF_UP);
        }
        return originalPrice;  // Ngoài giờ Happy Hour => không giảm
    }

    @Override
    public String getStrategyName() {
        return "HAPPY_HOUR_" + startTime.format(TIME_FMT)
                + "_" + endTime.format(TIME_FMT)
                + "_" + discountPercent.toPlainString() + "PCT";
    }

    /**
     * Trả về true nếu thời điểm hiện tại nằm trong [startTime, endTime).
     */
    public boolean isHappyHour() {
        LocalTime now = LocalTime.now();
        return !now.isBefore(startTime) && now.isBefore(endTime);
    }

    public LocalTime getStartTime() { return startTime; }
    public LocalTime getEndTime()   { return endTime; }
    public BigDecimal getDiscountPercent() { return discountPercent; }
}
