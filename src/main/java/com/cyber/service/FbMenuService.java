package com.cyber.service;

import com.cyber.connection.DatabaseConnection;
import com.cyber.dao.IFbMenuItemDAO;
import com.cyber.dao.impl.FbMenuItemDAOImpl;
import com.cyber.domain.fb.FbMenuItem;
import com.cyber.exception.BusinessException;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * Service Layer cho quản lý Menu F&B.
 * Tuân thủ 3-Tier: View -> FbMenuService -> DAO.
 * View KHÔNG được gọi DAO trực tiếp.
 *
 * <p>Chỉ tầng này mới quản lý Connection và Transaction.
 * DAO chỉ nhận Connection được pass vào, không tự mở.</p>
 */
public class FbMenuService {

    private static FbMenuService instance;
    private final IFbMenuItemDAO menuItemDAO;

    private FbMenuService() {
        this.menuItemDAO = FbMenuItemDAOImpl.getInstance();
    }

    public static synchronized FbMenuService getInstance() {
        if (instance == null) {
            instance = new FbMenuService();
        }
        return instance;
    }

    // -------------------------------------------------------
    // READ Operations (không cần transaction)
    // -------------------------------------------------------

    /**
     * Lấy toàn bộ menu đang ACTIVE hoặc OUT_OF_STOCK (cho User).
     */
    public List<FbMenuItem> getAllActiveMenuItems() throws BusinessException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return menuItemDAO.getAllActiveItems(conn);
        } catch (SQLException e) {
            throw new BusinessException("DB_ERROR", "Lỗi lấy danh sách menu: " + e.getMessage());
        }
    }

    /**
     * Lấy toàn bộ menu bao gồm cả HIDDEN (cho Admin).
     */
    public List<FbMenuItem> getAllMenuItemsForAdmin() throws BusinessException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return menuItemDAO.getAllItemsForAdmin(conn);
        } catch (SQLException e) {
            throw new BusinessException("DB_ERROR", "Lỗi lấy danh sách menu toàn bộ: " + e.getMessage());
        }
    }

    /**
     * Tìm MenuItem theo ID.
     *
     * @throws BusinessException ERR_ITEM_NOT_FOUND nếu không tìm thấy
     */
    public FbMenuItem getMenuItemById(int menuItemId) throws BusinessException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            FbMenuItem item = menuItemDAO.findById(conn, menuItemId);
            if (item == null) {
                throw new BusinessException("ERR_ITEM_NOT_FOUND", "Không tìm thấy món với ID=" + menuItemId);
            }
            return item;
        } catch (SQLException e) {
            throw new BusinessException("DB_ERROR", "Lỗi tìm món theo ID: " + e.getMessage());
        }
    }



    // -------------------------------------------------------
    // WRITE Operations (cần transaction khi multi-step)
    // -------------------------------------------------------

    /**
     * Tạo mới một MenuItem. Validate trước khi ghi DB.
     *
     * @return ID của MenuItem vừa tạo
     * @throws BusinessException ERR_VALIDATION nếu dữ liệu không hợp lệ
     */
    public int createMenuItem(FbMenuItem item) throws BusinessException {
        validateMenuItem(item);
        try (Connection conn = DatabaseConnection.getConnection()) {
            return menuItemDAO.create(conn, item);
        } catch (SQLException e) {
            throw new BusinessException("DB_ERROR", "Lỗi tạo món mới: " + e.getMessage());
        }
    }

    /**
     * Cập nhật thông tin một MenuItem.
     *
     * @throws BusinessException ERR_ITEM_NOT_FOUND | ERR_VALIDATION
     */
    public void updateMenuItem(FbMenuItem item) throws BusinessException {
        validateMenuItem(item);
        try (Connection conn = DatabaseConnection.getConnection()) {
            FbMenuItem existing = menuItemDAO.findById(conn, item.getMenuItemId());
            if (existing == null) {
                throw new BusinessException("ERR_ITEM_NOT_FOUND", "Không tìm thấy món cần sửa!");
            }
            if (existing.getStatus() == com.cyber.model.enums.FBStatus.HIDDEN) {
                throw new BusinessException("ERR_ITEM_HIDDEN", "Món ăn đang bị ẩn, không thể sửa thông tin mòn ăn.");
            }
            
            // Auto OUT_OF_STOCK if stock == 0 and currently ACTIVE
            if (item.getStockQuantity() == 0 && item.getStatus() == com.cyber.model.enums.FBStatus.ACTIVE) {
                item.setStatus(com.cyber.model.enums.FBStatus.OUT_OF_STOCK);
            }
            
            menuItemDAO.update(conn, item);
        } catch (SQLException e) {
            throw new BusinessException("DB_ERROR", "Lỗi cập nhật món: " + e.getMessage());
        }
    }

    /**
     * Thay đổi trạng thái Món ăn (Toggle): Ẩn <-> Hiện.
     * @throws BusinessException ERR_ITEM_NOT_FOUND
     */
    public void toggleMenuItemStatus(int menuItemId) throws BusinessException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            FbMenuItem existing = menuItemDAO.findById(conn, menuItemId);
            if (existing == null) {
                throw new BusinessException("ERR_ITEM_NOT_FOUND",
                        "Không tìm thấy món (ID=" + menuItemId + ")");
            }
            if (existing.getStatus() == com.cyber.model.enums.FBStatus.HIDDEN) {
                existing.setStatus(existing.getStockQuantity() > 0 ? com.cyber.model.enums.FBStatus.ACTIVE : com.cyber.model.enums.FBStatus.OUT_OF_STOCK);
            } else {
                existing.setStatus(com.cyber.model.enums.FBStatus.HIDDEN);
            }
            menuItemDAO.update(conn, existing);
        } catch (SQLException e) {
            throw new BusinessException("DB_ERROR", "Lỗi thay đổi trạng thái món: " + e.getMessage());
        }
    }



    // -------------------------------------------------------
    // Private Validation
    // -------------------------------------------------------

    private void validateMenuItem(FbMenuItem item) throws BusinessException {
        if (item == null) {
            throw new BusinessException("ERR_VALIDATION", "Dữ liệu món không được null.");
        }
        if (item.getName() == null || item.getName().isBlank()) {
            throw new BusinessException("ERR_VALIDATION", "Tên món không được để trống.");
        }
        if (item.getBasePrice() == null || item.getBasePrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("ERR_VALIDATION", "Giá gốc không được âm.");
        }
        if (item.getPrepTimeInMinutes() < 0) {
            throw new BusinessException("ERR_VALIDATION", "Thời gian chuẩn bị không được âm.");
        }
        if (item.getCategoryId() <= 0) {
            throw new BusinessException("ERR_VALIDATION", "Chưa chọn danh mục hợp lệ.");
        }
        // description và tags cho phép null — không validate
    }
}
