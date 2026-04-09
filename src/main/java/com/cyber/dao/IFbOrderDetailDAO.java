package com.cyber.dao;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * DAO Interface cho bảng fb_order_details (nâng cao, hỗ trợ Decorator config JSON).
 * Phân biệt với IFbOrderDAO (Phase 1) để không phá vỡ code cũ.
 */
public interface IFbOrderDetailDAO {

    /**
     * Lưu một order detail vào bảng fb_order_details.
     *
     * @param orderId              FK trỏ vào fb_orders
     * @param menuItemId           FK trỏ vào fb_menu_items
     * @param quantity             Số lượng
     * @param unitPrice            Giá đã tính (sau Decorator + Strategy)
     * @param itemDescription      Mô tả đầy đủ (getDescription())
     * @param itemConfigJson       Chuỗi JSON cấu hình Decorator
     * @param discountApplied      Số tiền đã giảm
     * @param discountStrategyName Tên Strategy đã dùng
     */
    void saveOrderDetail(Connection conn,
                         int orderId,
                         int menuItemId,
                         int quantity,
                         BigDecimal unitPrice,
                         String itemDescription,
                         String itemConfigJson,
                         BigDecimal discountApplied,
                         String discountStrategyName) throws SQLException;

    /**
     * Lấy toàn bộ chi tiết của một đơn hàng (kèm Join với fb_menu_items).
     *
     * @return Danh sách Map, mỗi phần tử là một dòng trong fb_order_details
     */
    List<Map<String, Object>> findDetailsByOrderId(Connection conn, int orderId) throws SQLException;
}
