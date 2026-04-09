package com.cyber.service;

import com.cyber.connection.DatabaseConnection;
import com.cyber.dao.IFbMenuItemDAO;
import com.cyber.dao.IFbOptionDAO;
import com.cyber.dao.impl.FbMenuItemDAOImpl;
import com.cyber.dao.impl.FbOptionDAOImpl;
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
    private final IFbOptionDAO   optionDAO;

    private FbMenuService() {
        this.menuItemDAO = FbMenuItemDAOImpl.getInstance();
        this.optionDAO   = FbOptionDAOImpl.getInstance();
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

    /**
     * Lấy danh sách Topping đang active.
     */
    public List<Map<String, Object>> getAllToppings() throws BusinessException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return optionDAO.findAllToppings(conn);
        } catch (SQLException e) {
            throw new BusinessException("DB_ERROR", "Lỗi lấy danh sách topping: " + e.getMessage());
        }
    }

    /**
     * Lấy các option (Size, Sugar, Ice) của một MenuItem.
     */
    public List<Map<String, Object>> getOptionsByMenuItemId(int menuItemId) throws BusinessException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return optionDAO.findOptionsByMenuItemId(conn, menuItemId);
        } catch (SQLException e) {
            throw new BusinessException("DB_ERROR", "Lỗi lấy options của món: " + e.getMessage());
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
                throw new BusinessException("ERR_ITEM_NOT_FOUND",
                        "Không tìm thấy món cần cập nhật (ID=" + item.getMenuItemId() + ")");
            }
            menuItemDAO.update(conn, item);
        } catch (SQLException e) {
            throw new BusinessException("DB_ERROR", "Lỗi cập nhật món: " + e.getMessage());
        }
    }

    /**
     * Xoá mềm (soft-delete) một MenuItem: đổi status = HIDDEN.
     *
     * @throws BusinessException ERR_ITEM_NOT_FOUND
     */
    public void deleteMenuItem(int menuItemId) throws BusinessException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            FbMenuItem existing = menuItemDAO.findById(conn, menuItemId);
            if (existing == null) {
                throw new BusinessException("ERR_ITEM_NOT_FOUND",
                        "Không tìm thấy món cần xoá (ID=" + menuItemId + ")");
            }
            menuItemDAO.deleteItem(conn, menuItemId);
        } catch (SQLException e) {
            throw new BusinessException("DB_ERROR", "Lỗi xoá món: " + e.getMessage());
        }
    }

    /**
     * Tạo topping mới (có ghi log).
     */
    public int createTopping(String name, BigDecimal extraPrice, com.cyber.model.User actor) throws BusinessException {
        if (name == null || name.isBlank()) {
            throw new BusinessException("ERR_VALIDATION", "Tên topping không được để trống.");
        }
        if (extraPrice == null || extraPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("ERR_VALIDATION", "Giá topping không được âm.");
        }
        
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);
            
            int newId = optionDAO.createTopping(conn, name.trim(), extraPrice);
            
            // Ghi log
            String action = String.format("Thêm Topping mới: %s (Giá: %s)", name, com.cyber.util.FormatUtils.formatVND(extraPrice));
            LogService.getInstance().log(conn, com.cyber.model.enums.LogType.FB, actor, action, null);
            
            conn.commit();
            return newId;
        } catch (SQLException e) {
            if (conn != null) try { conn.rollback(); } catch (SQLException ex) {}
            throw new BusinessException("DB_ERROR", "Lỗi tạo topping: " + e.getMessage());
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); conn.close(); } catch (SQLException ex) {}
            }
        }
    }

    /**
     * Cập nhật thông tin Topping (có ghi log).
     */
    public void updateTopping(int toppingId, String name, BigDecimal extraPrice, com.cyber.model.User actor) throws BusinessException {
        if (name == null || name.isBlank()) {
            throw new BusinessException("ERR_VALIDATION", "Tên topping không được để trống.");
        }
        if (extraPrice == null || extraPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("ERR_VALIDATION", "Giá topping không được âm.");
        }

        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);
            
            Map<String, Object> existing = optionDAO.findToppingById(conn, toppingId);
            if (existing == null) {
                throw new BusinessException("ERR_NOT_FOUND", "Không tìm thấy Topping ID=" + toppingId);
            }

            optionDAO.updateTopping(conn, toppingId, name.trim(), extraPrice);
            
            // Ghi log
            String action = String.format("Cập nhật Topping ID %d: %s -> %s (Giá: %s -> %s)", 
                    toppingId, existing.get("name"), name, 
                    com.cyber.util.FormatUtils.formatVND((BigDecimal)existing.get("extra_price")),
                    com.cyber.util.FormatUtils.formatVND(extraPrice));
            LogService.getInstance().log(conn, com.cyber.model.enums.LogType.FB, actor, action, null);
            
            conn.commit();
        } catch (SQLException e) {
            if (conn != null) try { conn.rollback(); } catch (SQLException ex) {}
            throw new BusinessException("DB_ERROR", "Lỗi cập nhật topping: " + e.getMessage());
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); conn.close(); } catch (SQLException ex) {}
            }
        }
    }

    /**
     * Xoá (vô hiệu hoá) Topping (có ghi log).
     */
    public void deleteTopping(int toppingId, com.cyber.model.User actor) throws BusinessException {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);
            
            Map<String, Object> existing = optionDAO.findToppingById(conn, toppingId);
            if (existing == null) {
                throw new BusinessException("ERR_NOT_FOUND", "Không tìm thấy Topping ID=" + toppingId);
            }

            optionDAO.deactivateTopping(conn, toppingId);
            
            // Ghi log
            String action = String.format("Xoá (Vô hiệu hoá) Topping: %s (ID: %d)", existing.get("name"), toppingId);
            LogService.getInstance().log(conn, com.cyber.model.enums.LogType.FB, actor, action, null);
            
            conn.commit();
        } catch (SQLException e) {
            if (conn != null) try { conn.rollback(); } catch (SQLException ex) {}
            throw new BusinessException("DB_ERROR", "Lỗi xoá topping: " + e.getMessage());
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); conn.close(); } catch (SQLException ex) {}
            }
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
    }
}
