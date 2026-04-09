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
     * Lấy toàn bộ topping đang active.
     * Key: toppingId, Value: [name, extra_price].
     */
    List<Map<String, Object>> findAllToppings(Connection conn) throws SQLException;

    /**
     * Tìm topping theo ID.
     *
     * @return Map có keys: topping_id, name, extra_price; null nếu không tìm thấy
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
    int createTopping(Connection conn, String name, BigDecimal extraPrice) throws SQLException;

    /**
     * Cập nhật thông tin topping.
     */
    void updateTopping(Connection conn, int toppingId, String name, BigDecimal extraPrice) throws SQLException;

    /**
     * Soft-delete topping (is_active = false).
     */
    void deactivateTopping(Connection conn, int toppingId) throws SQLException;
}
