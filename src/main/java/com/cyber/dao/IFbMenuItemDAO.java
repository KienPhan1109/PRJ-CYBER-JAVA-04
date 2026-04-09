package com.cyber.dao;

import com.cyber.domain.fb.FbMenuItem;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * DAO Interface cho bảng fb_menu_items.
 * Tuân thủ 3-Tier: Service -> DAO -> DB, không bao giờ gọi trực tiếp từ View.
 */
public interface IFbMenuItemDAO {

    /**
     * Lấy tất cả món đang ACTIVE hoặc OUT_OF_STOCK (kèm categoryName bằng JOIN).
     */
    List<FbMenuItem> findAllActive(Connection conn) throws SQLException;

    /**
     * Lấy tất cả món (bao gồm cả HIDDEN).
     */
    List<FbMenuItem> findAll(Connection conn) throws SQLException;

    /**
     * Tìm món theo ID (bất kể status).
     */
    FbMenuItem findById(Connection conn, int menuItemId) throws SQLException;

    /**
     * Tìm tất cả món thuộc một danh mục.
     */
    List<FbMenuItem> findByCategoryId(Connection conn, int categoryId) throws SQLException;

    /**
     * Tạo mới một MenuItem trong DB.
     *
     * @return ID vừa được tạo bởi DB (AUTO_INCREMENT)
     */
    int create(Connection conn, FbMenuItem item) throws SQLException;

    /**
     * Cập nhật thông tin MenuItem.
     */
    void update(Connection conn, FbMenuItem item) throws SQLException;

    /**
     * Soft-delete: Đổi status = 'HIDDEN' thay vì xoá vật lý.
     */
    void softDelete(Connection conn, int menuItemId) throws SQLException;

    /**
     * Trừ tồn kho khi đặt hàng thành công.
     *
     * @param quantity Số lượng cần trừ
     */
    void deductStock(Connection conn, int menuItemId, int quantity) throws SQLException;
}
