package com.cyber.view;

import com.cyber.domain.fb.FbMenuItem;
import com.cyber.exception.BusinessException;
import com.cyber.model.enums.FBStatus;
import com.cyber.model.enums.FbTemperature;
import com.cyber.service.FbMenuService;
import com.cyber.util.FormatUtils;
import com.cyber.util.InputUtils;
import com.cyber.util.PrintUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * View quản lý Menu F&B nâng cao dành cho Admin.
 * Cho phép CRUD fb_menu_items (với đầy đủ Core Attributes) và quản lý Topping.
 * Tuân thủ: View -> Service -> DAO. Không gọi DAO trực tiếp.
 */
public class FbMenuManagementView {

    private final FbMenuService menuService;
    private final com.cyber.model.User adminUser;

    public FbMenuManagementView(com.cyber.model.User adminUser) {
        this.menuService = FbMenuService.getInstance();
        this.adminUser = adminUser;
    }

    public void displayMenu() {
        while (true) {
            System.out.println("\n==================================");
            System.out.println("         QUẢN LÝ MENU F&B         ");
            System.out.println("==================================");
            System.out.println("1. Xem danh sách Menu");
            System.out.println("2. Thêm món mới vào Menu");
            System.out.println("3. Sửa thông tin món");
            System.out.println("4. Ẩn / Hiện món");
            System.out.println("0. Quay lại");

            int choice = InputUtils.inputInt("Chọn chức năng (0-4): ", 0, 4);
            switch (choice) {
                case 1 -> displayMenuList();
                case 2 -> handleAddItem();
                case 3 -> handleEditItem();
                case 4 -> handleDeleteItem();
                case 0 -> {
                    return;
                }
            }
        }
    }

    // -------------------------------------------------------
    // 1. Hiển thị danh sách Menu (có cột Mô tả)
    // -------------------------------------------------------
    private void displayMenuList() {
        try {
            List<FbMenuItem> items = menuService.getAllMenuItemsForAdmin();
            System.out.println("\n" + "=".repeat(170));
            System.out.println("                                                    DANH SÁCH MÓN TOÀN HỆ THỐNG (ADMIN)");
            System.out.println("=".repeat(170));
            System.out.printf("%-6s | %-10s | %-20s | %-30s | %-12s | %-8s | %-6s | %-15s | %-10s | %-10s | %-15s%n",
                    "ID", "Danh mục", "Tên món", "Mô tả", "Giá gốc", "Tồn kho", "T.gian", "Tags", "Nhiệt độ", "Giờ P.vụ", "Trạng thái");
            System.out.println("-".repeat(170));
            if (items.isEmpty()) {
                System.out.println("  Chưa có món nào trong menu.");
            } else {
                for (FbMenuItem item : items) {
                    String desc = item.getDescription() != null ? item.getDescription() : "(Không có)";
                    if (desc.length() > 28) desc = desc.substring(0, 25) + "...";
                    String tags = item.getItemTags() != null ? item.getItemTags() : "(Không)";
                    if (tags.length() > 13) tags = tags.substring(0, 10) + "...";

                    System.out.printf("%-6s | %-10s | %-20s | %-30s | %-12s | %-8d | %-6d | %-15s | %-10s | %-10s | %-15s%n",
                            FormatUtils.formatId("IT", item.getMenuItemId()),
                            item.getCategoryName() != null ? item.getCategoryName() : "N/A",
                            item.getName().length() > 18 ? item.getName().substring(0, 15) + "..." : item.getName(),
                            desc,
                            FormatUtils.formatVND(item.getBasePrice()),
                            item.getStockQuantity(),
                            item.getPrepTimeInMinutes(),
                            tags,
                            FormatUtils.formatFbTemperature(item.getTemperatureLevel()),
                            item.getAvailability() != null ? item.getAvailability() : "ALL",
                            FormatUtils.formatFbStatus(item.getStatus()));
                }
            }
            System.out.println("=".repeat(170));
        } catch (BusinessException e) {
            PrintUtils.printError(e.getMessage());
        }
    }

    // -------------------------------------------------------
    // 2. Thêm món mới (desc & tags cho phép null)
    // -------------------------------------------------------
    private void handleAddItem() {
        System.out.println("\n--- THÊM MÓN MỚI VÀO MENU ---");
        System.out.println("Chọn danh mục:");
        System.out.println("1. FOOD | 2. DRINK | 3. SNACK | 4. TOPPING");
        int categoryId = InputUtils.inputInt("Lựa chọn (1-4): ", 1, 4);

        String name = InputUtils.inputString("Tên món: ");

        // Mô tả: cho phép null (Enter để bỏ qua)
        String desc = InputUtils.inputStringOptional("Mô tả (Enter để bỏ qua): ");
        if (desc.isEmpty()) desc = null;

        BigDecimal price = InputUtils.inputBigDecimal("Giá gốc (VND): ", BigDecimal.ZERO);
        int stock = InputUtils.inputInt("Tồn kho ban đầu: ", 0, 99999);
        int prepTime = InputUtils.inputInt("Thời gian chuẩn bị (phút): ", 0, 120);

        // Tags: cho phép null (Enter để bỏ qua)
        String tags = InputUtils.inputStringOptional("Tags (VD: Spicy,Vegan,BestSeller) [Enter để bỏ qua]: ");
        if (tags.isEmpty()) tags = null;

        String availability = InputUtils.inputString("Khung giờ phục vụ (ALL hoặc VD: 06:00-22:00): ");

        System.out.println("Chọn nhiệt độ:");
        System.out.println("1. HOT | 2. COLD | 3. ICED | 4. NONE");
        int tempChoice = InputUtils.inputInt("Lựa chọn (1-4): ", 1, 4);
        FbTemperature temperatureLevel = switch (tempChoice) {
            case 1 -> FbTemperature.HOT;
            case 2 -> FbTemperature.COLD;
            case 3 -> FbTemperature.ICED;
            default -> FbTemperature.NONE;
        };

        FbMenuItem newItem = new FbMenuItem();
        newItem.setCategoryId(categoryId);
        newItem.setName(name);
        newItem.setDescription(desc);
        newItem.setBasePrice(price);
        newItem.setStockQuantity(stock);
        newItem.setPrepTimeInMinutes(prepTime);
        newItem.setItemTags(tags);
        newItem.setAvailability(availability);
        newItem.setTemperatureLevel(temperatureLevel);
        newItem.setStatus(stock == 0 ? FBStatus.OUT_OF_STOCK : FBStatus.ACTIVE);

        try {
            int newId = menuService.createMenuItem(newItem);
            PrintUtils.printSuccess("Đã thêm món mới vào Menu! ID = %d", newId);
        } catch (BusinessException e) {
            PrintUtils.printError(e.getMessage());
        }
    }

    // -------------------------------------------------------
    // 3. Sửa món (desc & tags cho phép để trống → null)
    // -------------------------------------------------------
    private void handleEditItem() {
        System.out.println("\n--- SỬA THÔNG TIN MÓN ---");
        int id = InputUtils.inputInt("Nhập Menu Item ID cần sửa: ", 1, Integer.MAX_VALUE);

        try {
            FbMenuItem existing = menuService.getMenuItemById(id);
            if (existing.getStatus() == FBStatus.HIDDEN) {
                PrintUtils.printError("Món này đang bị ẨN. Vui lòng HIỆN món ăn trước khi sửa đổi.");
                return;
            }

            System.out.println("Danh mục: 1. FOOD | 2. DRINK | 3. SNACK | 4. TOPPING");
            int catId = InputUtils.inputIntUpdate(
                    "Danh mục mới (Cũ: " + existing.getCategoryId() + ") [Enter giữ nguyên]: ", existing.getCategoryId(), 1, 4);

            String name = InputUtils.inputStringUpdate(
                    "Tên mới (Cũ: " + existing.getName() + ") [Enter giữ nguyên]: ", existing.getName());

            // Mô tả: cho phép để trống
            String oldDesc = existing.getDescription() != null ? existing.getDescription() : "";
            String desc = InputUtils.inputStringUpdate(
                    "Mô tả mới (Cũ: " + (oldDesc.isEmpty() ? "Không có" : oldDesc) + ") [Enter giữ nguyên]: ", oldDesc);
            if (desc != null && desc.isBlank()) desc = null;

            BigDecimal price = InputUtils.inputBigDecimalUpdate(
                    "Giá gốc mới [Enter giữ nguyên]: ", existing.getBasePrice(), BigDecimal.ZERO);
            int stock = InputUtils.inputIntUpdate(
                    "Tồn kho mới [Enter giữ nguyên]: ", existing.getStockQuantity(), 0, 99999);
            int prepTime = InputUtils.inputIntUpdate(
                    "Thời gian chuẩn bị (phút) [Enter giữ nguyên]: ", existing.getPrepTimeInMinutes(), 0, 120);

            // Tags: cho phép để trống
            String oldTags = existing.getItemTags() != null ? existing.getItemTags() : "";
            String tags = InputUtils.inputStringUpdate(
                    "Tags (Cũ: " + (oldTags.isEmpty() ? "Không có" : oldTags) + ") [Enter giữ nguyên]: ", oldTags);
            if (tags != null && tags.isBlank()) tags = null;

            String avail = InputUtils.inputStringUpdate(
                    "Khung giờ [Enter giữ nguyên]: ", existing.getAvailability() != null ? existing.getAvailability() : "ALL");

            existing.setCategoryId(catId);
            existing.setName(name);
            existing.setDescription(desc);
            existing.setBasePrice(price);
            existing.setStockQuantity(stock);
            existing.setPrepTimeInMinutes(prepTime);
            existing.setItemTags(tags);
            existing.setAvailability(avail);
            existing.setStatus(stock == 0 ? FBStatus.OUT_OF_STOCK : FBStatus.ACTIVE);

            menuService.updateMenuItem(existing);
            PrintUtils.printSuccess("Cập nhật món thành công!");
        } catch (BusinessException e) {
            PrintUtils.printError(e.getMessage());
        }
    }

    // -------------------------------------------------------
    // 4. Ẩn/Hiện món (Toggle HIDDEN <-> ACTIVE)
    // -------------------------------------------------------
    private void handleDeleteItem() {
        System.out.println("\n--- ẨN / HIỆN MÓN (TOGGLE) ---");
        int id = InputUtils.inputInt("Nhập Menu Item ID cần thay đổi trạng thái: ", 1, Integer.MAX_VALUE);
        String confirm = InputUtils.inputString("Xác nhận thay đổi trạng thái món này? (Y/N): ", "^[YyNn]$", "Chỉ nhập Y hoặc N.");

        if (confirm.equalsIgnoreCase("Y")) {
            try {
                menuService.toggleMenuItemStatus(id);
                PrintUtils.printSuccess("Đã thay đổi trạng thái món ID=%d thành công.", id);
            } catch (BusinessException e) {
                PrintUtils.printError(e.getMessage());
            }
        } else {
            System.out.println("Đã hủy thao tác.");
        }
    }


}
