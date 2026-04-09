package com.cyber.domain.fb;

import java.math.BigDecimal;

/**
 * COMPOSITE PATTERN — Leaf Node.
 * Đại diện cho một món ăn/nước uống đơn lẻ trong menu.
 * Được bọc bên ngoài bởi {@link ItemDecorator} để thêm Size, Topping, v.v.
 */
public class SingleItem implements IBillable {

    private final FbMenuItem menuItem;

    /**
     * @param menuItem Dữ liệu món ăn được load từ DAO
     */
    public SingleItem(FbMenuItem menuItem) {
        if (menuItem == null) {
            throw new IllegalArgumentException("FbMenuItem không được null khi tạo SingleItem.");
        }
        this.menuItem = menuItem;
    }

    /**
     * Giá gốc của món ăn, chưa cộng thêm bất kỳ Decorator nào.
     */
    @Override
    public BigDecimal calculatePrice() {
        return menuItem.getBasePrice();
    }

    /**
     * Mô tả là tên món ăn. Decorator sẽ mở rộng chuỗi này.
     */
    @Override
    public String getDescription() {
        return menuItem.getName();
    }

    /** Truy cập entity gốc khi cần (ví dụ: lấy prepTime, tags, ...). */
    public FbMenuItem getMenuItem() {
        return menuItem;
    }
}
