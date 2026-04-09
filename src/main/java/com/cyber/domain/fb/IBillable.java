package com.cyber.domain.fb;

import java.math.BigDecimal;

/**
 * Interface gốc của Decorator Pattern cho phân hệ F&B.
 * Mọi thứ có thể tính tiền (món lẻ, topping, size) đều implement interface này.
 */
public interface IBillable {

    /**
     * Tính giá thực tế của item (đã bao gồm mọi decorator).
     *
     * @return Giá thành (BigDecimal) — không bao giờ null
     */
    BigDecimal calculatePrice();

    /**
     * Trả về chuỗi mô tả đầy đủ (VD: "Trà sữa + Size L + Trân châu đen").
     *
     * @return Chuỗi mô tả
     */
    String getDescription();
}
