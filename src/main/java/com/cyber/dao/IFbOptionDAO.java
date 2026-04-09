package com.cyber.dao;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * DAO Interface cho bảng fb_toppings và fb_item_options.
 */
public interface IFbOptionDAO {

    /**
     * Lấy toàn bộ topping đang ACTIVE (cho Customer).
     * Key: topping_id, name, extra_price, stock_quantity, status.
     */
    List<Map<String, Object>> findAllToppings(Connection conn) throws SQLException;

    /**
     * Lấy toàn bộ topping bao gồm cả HIDDEN (cho Admin).
     */
    List<Map<String, Object>> findAllToppingsForAdmin(Connection conn) throws SQLException;

    /**
     * Tìm topping theo ID (bất kể trạng thái, dùng cho Admin).
     *
     * @return Map có keys: topping_id, name, extra_price, stock_quantity, status; null nếu không tìm thấy
     */
    Map<String, Object> findToppingById(Connection conn, int toppingId) throws SQLException;

    /**
     * Lấy toàn bộ option (Size, Weight, v.v.) của một MenuItem.
     */
    List<Map<String, Object>> findOptionsByMenuItemId(Connection conn, int menuItemId) throws SQLException;

    /**
     * Tạo topping mới.
     *
     * @return ID toppingId vừa tạo
     */
    int createTopping(Connection conn, String name, BigDecimal extraPrice, int stockQuantity) throws SQLException;

    /**
     * Cập nhật thông tin topping (tên, phụ phí, tồn kho).
     */
    void updateTopping(Connection conn, int toppingId, String name, BigDecimal extraPrice, int stockQuantity) throws SQLException;

    /**
     * Cập nhật status cho topping (ACTIVE, OUT_OF_STOCK, HIDDEN)
     */
    void updateToppingStatus(Connection conn, int toppingId, String status) throws SQLException;

    /**
     * Trừ tồn kho topping khi đặt hàng.
     * Nếu stock về 0, tự động đổi status = OUT_OF_STOCK.
     */
    void deductToppingStock(Connection conn, int toppingId, int quantity) throws SQLException;
}
