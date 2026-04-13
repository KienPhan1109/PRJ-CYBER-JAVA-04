package com.cyber.view;

import com.cyber.domain.fb.FbMenuItem;
import com.cyber.exception.BusinessException;
import com.cyber.model.enums.FBStatus;
import com.cyber.service.FbMenuService;
import com.cyber.util.FormatUtils;
import com.cyber.util.InputUtils;
import com.cyber.util.PrintUtils;

import java.util.List;

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

            int choice = InputUtils.inputInt("Vui lòng chọn chức năng (0-4): ", 0, 4);
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
            if (items.isEmpty()) {
                System.out.println("  Chưa có món nào trong menu.");
                return;
            }
            int pageSize = 10;
            int totalPages = (int) Math.ceil((double) items.size() / pageSize);
            int currentPage = 1;

            while (true) {
                int start = (currentPage - 1) * pageSize;
                int end = Math.min(start + pageSize, items.size());

                System.out.println("\n" + "=".repeat(150));
                System.out.println("DANH SÁCH MÓN TOÀN HỆ THỐNG");
                System.out.println("=".repeat(150));
                System.out.printf("%-6s | %-10s | %-20s | %-30s | %-12s | %-6s | %-5s | %-10s | %-10s | %-12s%n",
                        "ID", "Danh mục", "Tên món", "Mô tả", "Giá gốc", "Kho", "Phút", "Nhiệt độ", "Giờ P.Vụ", "Trạng thái");
                System.out.println("-".repeat(150));

                for (int i = start; i < end; i++) {
                    FbMenuItem item = items.get(i);
                    System.out.printf("%-6s | %-10s | %-20s | %-30s | %-12s | %-6d | %-5d | %-19s | %-10s | %-21s%n",
                            FormatUtils.formatId("IT", item.getMenuItemId()),
                            FormatUtils.formatValue(item.getCategoryName()),
                            FormatUtils.truncate(item.getName(), 20),
                            FormatUtils.truncate(item.getDescription()),
                            FormatUtils.formatVND(item.getBasePrice()),
                            item.getStockQuantity(),
                            item.getPrepTimeInMinutes(),
                            FormatUtils.formatFbTemperature(item.getTemperatureLevel()),
                            FormatUtils.formatFbAvailability(item.getAvailability()),
                            FormatUtils.formatFbStatus(item.getStatus()));
                }
                System.out.println("=".repeat(150));
                System.out.println("Tổng: " + items.size() + " món | Trang " + currentPage + "/" + totalPages);

                if (totalPages <= 1) break;
                System.out.println("[N] Trang sau | [P] Trang trước | [Q] Thoát");
                String nav = InputUtils.inputString("Lựa chọn: ").toUpperCase();
                if (nav.equals("N") && currentPage < totalPages) currentPage++;
                else if (nav.equals("P") && currentPage > 1) currentPage--;
                else if (nav.equals("Q")) break;
            }
        } catch (BusinessException e) {
            PrintUtils.printError(e.getMessage());
        }
    }

    // -------------------------------------------------------
    // 2. Thêm món mới (validate tên tức thì + inputData)
    // -------------------------------------------------------
    private void handleAddItem() {
        System.out.println("\n--- THÊM MÓN MỚI VÀO MENU ---");

        // Validate tên món ngay lập tức
        String name;
        while (true) {
            name = InputUtils.inputString("Tên món: ");
            try {
                if (menuService.isNameExists(name)) {
                    PrintUtils.printError("Tên món '" + name + "' đã tồn tại! Vui lòng nhập tên khác.");
                    continue;
                }
                break; // Tên hợp lệ, cho nhập tiếp
            } catch (BusinessException e) {
                PrintUtils.printError(e.getMessage());
                return;
            }
        }

        FbMenuItem newItem = new FbMenuItem();
        newItem.inputData(false, name);

        try {
            int newId = menuService.createMenuItem(newItem, adminUser);
            PrintUtils.printSuccess("Đã thêm món mới vào Menu! ID = %d", newId);
        } catch (BusinessException e) {
            PrintUtils.printError(e.getMessage());
        }
    }

    // -------------------------------------------------------
    // 3. Sửa món (validate tên tức thì + inputData)
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

            // Validate tên món ngay lập tức (cho phép giữ nguyên tên cũ)
            String name;
            while (true) {
                name = InputUtils.inputStringUpdate(
                        "Tên mới (Cũ: " + existing.getName() + ") [Enter giữ nguyên]: ", existing.getName());
                if (!name.equals(existing.getName())) {
                    try {
                        if (menuService.isNameExists(name)) {
                            PrintUtils.printError("Tên món '" + name + "' đã tồn tại! Vui lòng nhập tên khác.");
                            continue;
                        }
                    } catch (BusinessException e) {
                        PrintUtils.printError(e.getMessage());
                        return;
                    }
                }
                break;
            }

            existing.inputData(true, name);

            menuService.updateMenuItem(existing, adminUser);
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
        try {
            FbMenuItem item = menuService.getMenuItemById(id);
            boolean isHidden = item.getStatus() == FBStatus.HIDDEN;
            String action = isHidden ? "HIỆN" : "ẨN";
            String confirmMsg = String.format("Bạn có chắc chắn muốn %s món [%s] không? (Y/N): ", action, item.getName());
            String confirm = InputUtils.inputString(confirmMsg, "^[YyNn]$", "Chỉ nhập Y hoặc N.");

            if (confirm.equalsIgnoreCase("Y")) {
                menuService.toggleMenuItemStatus(id, adminUser);
                PrintUtils.printSuccess("Đã %s món [%s] thành công.", action, item.getName());
            } else {
                System.out.println("Đã hủy thao tác.");
            }
        } catch (BusinessException e) {
            PrintUtils.printError(e.getMessage());
        }
    }


}
