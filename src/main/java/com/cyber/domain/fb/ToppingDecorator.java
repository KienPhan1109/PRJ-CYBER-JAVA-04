package com.cyber.domain.fb;

import java.math.BigDecimal;

/**
 * DECORATOR PATTERN — Concrete Decorator: Topping.
 * Cộng thêm giá một loại topping vào giá của wrappee.
 * Có thể xếp chồng nhiều ToppingDecorator để thêm nhiều topping khác nhau.
 *
 * <p>Cách dùng:
 * <pre>
 *   IBillable drink = new SingleItem(traSuaItem);                     // 35.000
 *   drink = new SizeDecorator(drink, SizeDecorator.SizeType.L);       // +10.000
 *   drink = new ToppingDecorator(drink, "Trân châu đen", 7000);       // +7.000
 *   drink = new ToppingDecorator(drink, "Thạch cà phê", 6000);        // +6.000
 *   // Tổng = 35.000 + 10.000 + 7.000 + 6.000 = 58.000đ
 * </pre>
 * </p>
 */
public class ToppingDecorator extends ItemDecorator {

    private final int       toppingId;      // ID trong bảng fb_toppings (0 nếu không có)
    private final String    toppingName;    // Tên topping (VD: "Trân châu đen")
    private final BigDecimal toppingPrice;  // Phụ phí topping

    /**
     * @param wrappee      Đối tượng IBillable gốc
     * @param toppingId    ID trong DB (dùng để persist)
     * @param toppingName  Tên topping để hiển thị
     * @param toppingPrice Giá topping (>= 0)
     */
    public ToppingDecorator(IBillable wrappee, int toppingId, String toppingName, BigDecimal toppingPrice) {
        super(wrappee);
        this.toppingId    = toppingId;
        this.toppingName  = (toppingName != null && !toppingName.isBlank()) ? toppingName : "Topping";
        this.toppingPrice = (toppingPrice != null && toppingPrice.compareTo(BigDecimal.ZERO) >= 0)
                            ? toppingPrice
                            : BigDecimal.ZERO;
    }

    /** Convenience constructor không cần toppingId (dùng khi demo/test). */
    public ToppingDecorator(IBillable wrappee, String toppingName, BigDecimal toppingPrice) {
        this(wrappee, 0, toppingName, toppingPrice);
    }

    /** Giá = Giá wrappee + phụ phí topping. */
    @Override
    public BigDecimal calculatePrice() {
        return wrappee.calculatePrice().add(toppingPrice);
    }

    /** Mô tả = "... + Trân châu đen (+7.000đ)". */
    @Override
    public String getDescription() {
        String extra = toppingPrice.compareTo(BigDecimal.ZERO) > 0
                ? " (+" + String.format("%,.0f", toppingPrice) + "đ)"
                : "";
        return wrappee.getDescription() + " + " + toppingName + extra;
    }

    public int getToppingId() { return toppingId; }
    public String getToppingName() { return toppingName; }
    public BigDecimal getToppingPrice() { return toppingPrice; }
}
