package com.cyber.domain.fb;

import java.math.BigDecimal;

/**
 * DECORATOR PATTERN — Abstract Decorator.
 * Bọc một IBillable bên trong và delegate các lời gọi xuống đối tượng đó.
 * Các subclass sẽ override {@link #calculatePrice()} và {@link #getDescription()}
 * để thêm hành vi (cộng giá, mô tả).
 *
 * <p>Hệ thống phân cấp Decorator:
 * <pre>
 *   IBillable (interface)
 *   └── ItemDecorator (abstract)
 *       ├── SizeDecorator      — cộng thêm giá theo Size M/L
 *       ├── ToppingDecorator   — cộng thêm giá Topping
 *       └── WeightDecorator    — cộng giá theo gram
 * </pre>
 * </p>
 */
public abstract class ItemDecorator implements IBillable {

    /** Đối tượng được bọc bên trong (wrapped component). */
    protected final IBillable wrappee;

    protected ItemDecorator(IBillable wrappee) {
        if (wrappee == null) {
            throw new IllegalArgumentException("Wrappee (IBillable) không được null trong Decorator.");
        }
        this.wrappee = wrappee;
    }

    /** Mặc định delegate giá xuống wrappee — subclass override để cộng thêm. */
    @Override
    public BigDecimal calculatePrice() {
        return wrappee.calculatePrice();
    }

    /** Mặc định delegate mô tả xuống wrappee — subclass override để mở rộng. */
    @Override
    public String getDescription() {
        return wrappee.getDescription();
    }

    /** Cho phép truy cập wrappee gốc khi cần (ví dụ: serialize JSON). */
    public IBillable getWrappee() {
        return wrappee;
    }
}
