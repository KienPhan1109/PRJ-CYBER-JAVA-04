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
import com.cyber.model.User;
import com.cyber.model.enums.LogType;

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
    private final LogService logService;

    private FbMenuService() {
        this.menuItemDAO = FbMenuItemDAOImpl.getInstance();
        this.logService = LogService.getInstance();
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
            if (item == null || item.isDeleted()) {
                throw new BusinessException("ERR_ITEM_NOT_FOUND", "Không tìm thấy món với ID=" + menuItemId);
            }
            return item;
        } catch (SQLException e) {
            throw new BusinessException("DB_ERROR", "Lỗi tìm món theo ID: " + e.getMessage());
        }
    }

    /**
     * Kiểm tra xem tên món đã tồn tại trong DB chưa (dùng cho validate tức thì).
     */
    public boolean isNameExists(String name) throws BusinessException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return menuItemDAO.findByName(conn, name) != null;
        } catch (SQLException e) {
            throw new BusinessException("DB_ERROR", "Lỗi kiểm tra tên món: " + e.getMessage());
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
    public int createMenuItem(FbMenuItem item, User actor) throws BusinessException {
        validateMenuItem(item);
        try (Connection conn = DatabaseConnection.getConnection()) {
            // Kiểm tra trùng tên
            com.cyber.domain.fb.FbMenuItem existing = menuItemDAO.findByName(conn, item.getName());
            if (existing != null) {
                throw new BusinessException("DUPLICATE_NAME", "Tên món '" + item.getName() + "' đã tồn tại trong hệ thống.");
            }
            int newId = menuItemDAO.create(conn, item);
            if (actor != null) {
                logService.logStandalone(LogType.FB, actor,
                        "Thêm món mới: " + item.getName() + " (ID=" + newId + ")", newId);
            }
            return newId;
        } catch (java.sql.SQLException e) {
            throw new BusinessException("DB_ERROR", "Lỗi tạo món mới: " + e.getMessage());
        }
    }

    /** Backward compat - không có actor thì không log */
    public int createMenuItem(FbMenuItem item) throws BusinessException {
        return createMenuItem(item, null);
    }

    /**
     * Cập nhật thông tin một MenuItem.
     *
     * @throws BusinessException ERR_ITEM_NOT_FOUND | ERR_VALIDATION
     */
    public void updateMenuItem(FbMenuItem item, User actor) throws BusinessException {
        validateMenuItem(item);
        try (Connection conn = DatabaseConnection.getConnection()) {
            FbMenuItem existing = menuItemDAO.findById(conn, item.getMenuItemId());
            if (existing == null || existing.isDeleted()) {
                throw new BusinessException("ERR_ITEM_NOT_FOUND", "Không tìm thấy món cần sửa!");
            }
            if (existing.getStatus() == com.cyber.model.enums.FBStatus.HIDDEN) {
                throw new BusinessException("ERR_ITEM_HIDDEN", "Món ăn đang bị ẩn, không thể sửa thông tin món ăn.");
            }
            
            if (item.getStockQuantity() == 0 && item.getStatus() == com.cyber.model.enums.FBStatus.ACTIVE) {
                item.setStatus(com.cyber.model.enums.FBStatus.OUT_OF_STOCK);
            }
            
            menuItemDAO.update(conn, item);
            if (actor != null) {
                logService.logStandalone(LogType.FB, actor,
                        "Cập nhật món: " + item.getName() + " (ID=" + item.getMenuItemId() + ")", item.getMenuItemId());
            }
        } catch (SQLException e) {
            throw new BusinessException("DB_ERROR", "Lỗi cập nhật món: " + e.getMessage());
        }
    }

    public void updateMenuItem(FbMenuItem item) throws BusinessException {
        updateMenuItem(item, null);
    }

    /**
     * Thay đổi trạng thái Món ăn (Toggle): Ẩn <-> Hiện.
     * @throws BusinessException ERR_ITEM_NOT_FOUND
     */
    public void toggleMenuItemStatus(int menuItemId, User actor) throws BusinessException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            FbMenuItem existing = menuItemDAO.findById(conn, menuItemId);
            if (existing == null || existing.isDeleted()) {
                throw new BusinessException("ERR_ITEM_NOT_FOUND",
                        "Không tìm thấy món (ID=" + menuItemId + ")");
            }
            String oldStatus = existing.getStatus().name();
            if (existing.getStatus() == com.cyber.model.enums.FBStatus.HIDDEN) {
                existing.setStatus(existing.getStockQuantity() > 0 ? com.cyber.model.enums.FBStatus.ACTIVE : com.cyber.model.enums.FBStatus.OUT_OF_STOCK);
            } else {
                existing.setStatus(com.cyber.model.enums.FBStatus.HIDDEN);
            }
            menuItemDAO.update(conn, existing);
            if (actor != null) {
                logService.logStandalone(LogType.FB, actor,
                        "Toggle món [" + existing.getName() + "]: " + oldStatus + " -> " + existing.getStatus().name(),
                        menuItemId);
            }
        } catch (SQLException e) {
            throw new BusinessException("DB_ERROR", "Lỗi thay đổi trạng thái món: " + e.getMessage());
        }
    }

    public void toggleMenuItemStatus(int menuItemId) throws BusinessException {
        toggleMenuItemStatus(menuItemId, null);
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

    /**
     * Xóa vĩnh viễn (Soft Delete) một món ăn.
     */
    public void deleteMenuItem(int menuItemId, User actor) throws BusinessException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            FbMenuItem existing = menuItemDAO.findById(conn, menuItemId);
            if (existing == null || existing.isDeleted()) {
                throw new BusinessException("ERR_ITEM_NOT_FOUND", "Không tìm thấy món (ID=" + menuItemId + ")");
            }
            if (existing.getStatus() != com.cyber.model.enums.FBStatus.HIDDEN) {
                throw new BusinessException("INVALID_STATUS", "Chỉ có thể xóa món khi đang ở trạng thái ẨN (HIDDEN). Vui lòng Ẩn món trước khi xóa.");
            }
            menuItemDAO.deleteItem(conn, menuItemId);
            if (actor != null) {
                logService.logStandalone(LogType.FB, actor,
                        "Xóa (soft) món: " + existing.getName() + " (ID=" + menuItemId + ")", menuItemId);
            }
        } catch (SQLException e) {
            throw new BusinessException("DB_ERROR", "Lỗi xóa món: " + e.getMessage());
        }
    }
}
