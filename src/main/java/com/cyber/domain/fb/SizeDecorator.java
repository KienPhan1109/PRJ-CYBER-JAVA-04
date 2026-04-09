package com.cyber.domain.fb;

import java.math.BigDecimal;

/**
 * DECORATOR PATTERN — Concrete Decorator: Size.
 * Cộng thêm phụ phí vào giá gốc tuỳ theo kích cỡ (M hoặc L).
 *
 * <p>Cách dùng:
 * <pre>
 *   IBillable traSua = new SingleItem(traSuaItem);
 *   IBillable traSuaSizeL = new SizeDecorator(traSua, SizeDecorator.SizeType.L);
 *   // Giá = 35.000 + 10.000 = 45.000đ
 * </pre>
 * </p>
 */
public class SizeDecorator extends ItemDecorator {

    /** Enum chuẩn hoá các size được hỗ trợ. */
    public enum SizeType {
        S("Size S", BigDecimal.ZERO),
        M("Size M", BigDecimal.ZERO),
        L("Size L", new BigDecimal("10000"));

        private final String label;
        private final BigDecimal extraPrice;

        SizeType(String label, BigDecimal extraPrice) {
            this.label      = label;
            this.extraPrice = extraPrice;
        }

        public String getLabel() { return label; }
        public BigDecimal getExtraPrice() { return extraPrice; }
    }

    private final SizeType sizeType;

    /**
     * @param wrappee  Đối tượng IBillable gốc được bọc
     * @param sizeType Kích cỡ khách chọn
     */
    public SizeDecorator(IBillable wrappee, SizeType sizeType) {
        super(wrappee);
        this.sizeType = (sizeType != null) ? sizeType : SizeType.M;
    }

    /**
     * Giá = Giá của wrappee + phụ phí size.
     */
    @Override
    public BigDecimal calculatePrice() {
        return wrappee.calculatePrice().add(sizeType.getExtraPrice());
    }

    /**
     * Mô tả = "... + Size L (+10.000đ)".
     */
    @Override
    public String getDescription() {
        String extra = sizeType.getExtraPrice().compareTo(BigDecimal.ZERO) > 0
                ? " (+" + String.format("%,.0f", sizeType.getExtraPrice()) + "đ)"
                : "";
        return wrappee.getDescription() + " + " + sizeType.getLabel() + extra;
    }

    public SizeType getSizeType() {
        return sizeType;
    }
}
