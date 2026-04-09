package com.cyber.view;

import com.cyber.domain.fb.FbMenuItem;
import com.cyber.exception.BusinessException;
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

    public FbMenuManagementView() {
        this.menuService = FbMenuService.getInstance();
    }

    public void displayMenu() {
        while (true) {
            System.out.println("\n--- QUẢN LÝ MENU F&B NÂNG CAO ---");
            System.out.println("1. Xem danh sách Menu");
            System.out.println("2. Thêm món mới vào Menu");
            System.out.println("3. Sửa thông tin món");
            System.out.println("4. Ẩn/Xoá mềm món");
            System.out.println("5. Quản lý Topping");
            System.out.println("0. Quay lại");

            int choice = InputUtils.inputInt("Chọn chức năng (0-5): ", 0, 5);
            switch (choice) {
                case 1: displayMenuList();  break;
                case 2: handleAddItem();    break;
                case 3: handleEditItem();   break;
                case 4: handleDeleteItem(); break;
                case 5: manageToppings();   break;
                case 0: return;
            }
        }
    }

    // -------------------------------------------------------
    // 1. Hiển thị danh sách Menu
    // -------------------------------------------------------
    private void displayMenuList() {
        try {
            List<FbMenuItem> items = menuService.getAllActiveMenuItems();
            System.out.println("\n" + "=".repeat(130));
            System.out.printf("%-5s | %-10s | %-25s | %-12s | %-7s | %-6s | %-20s | %-8s | %-6s%n",
                    "ID", "Danh mục", "Tên món", "Giá gốc", "Tồn kho", "T.gian", "Tags", "Nhiệt độ", "Giờ phục vụ");
            System.out.println("-".repeat(130));
            if (items.isEmpty()) {
                System.out.println("  Chưa có món nào trong menu.");
            } else {
                for (FbMenuItem item : items) {
                    System.out.printf("%-5d | %-10s | %-25s | %-12s | %-7d | %-6d | %-20s | %-8s | %-6s%n",
                            item.getMenuItemId(),
                            item.getCategoryName() != null ? item.getCategoryName() : "-",
                            item.getName(),
                            FormatUtils.formatVND(item.getBasePrice()),
                            item.getStockQuantity(),
                            item.getPrepTimeInMinutes(),
                            item.getItemTags() != null ? item.getItemTags() : "-",
                            item.getTemperatureLevel() != null ? item.getTemperatureLevel() : "-",
                            item.getAvailability() != null ? item.getAvailability() : "ALL");
                }
            }
            System.out.println("=".repeat(130));
        } catch (BusinessException e) {
            PrintUtils.printError(e.getMessage());
        }
    }

    // -------------------------------------------------------
    // 2. Thêm món mới
    // -------------------------------------------------------
    private void handleAddItem() {
        System.out.println("\n--- THÊM MÓN MỚI VÀO MENU ---");
        System.out.println("Danh mục: 1=FOOD  2=DRINK  3=SNACK");

        int catChoice = InputUtils.inputInt("Chọn danh mục (1-3): ", 1, 3);
        // Seed data: FOOD=1, DRINK=2, SNACK=3
        int categoryId = catChoice;

        String name = InputUtils.inputString("Tên món: ");
        String desc = InputUtils.inputString("Mô tả: ");
        BigDecimal price = InputUtils.inputBigDecimal("Giá gốc (VND): ", BigDecimal.ZERO);
        int stock = InputUtils.inputInt("Tồn kho ban đầu: ", 0, 99999);
        int prepTime = InputUtils.inputInt("Thời gian chuẩn bị (phút): ", 0, 120);
        String tags = InputUtils.inputString("Tags (VD: Spicy,Vegan,BestSeller): ");
        String availability = InputUtils.inputString("Khung giờ phục vụ (ALL hoặc VD: 06:00-22:00): ");

        System.out.println("Nhiệt độ: 1=HOT  2=COLD  3=ICED  4=NONE");
        int tempChoice = InputUtils.inputInt("Chọn (1-4): ", 1, 4);
        String[] tempValues = {"HOT", "COLD", "ICED", "NONE"};
        String temperatureLevel = tempValues[tempChoice - 1];

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
        newItem.setStatus(stock == 0 ? "OUT_OF_STOCK" : "ACTIVE");

        try {
            int newId = menuService.createMenuItem(newItem);
            PrintUtils.printSuccess("Đã thêm món mới vào Menu! ID = %d", newId);
        } catch (BusinessException e) {
            PrintUtils.printError(e.getMessage());
        }
    }

    // -------------------------------------------------------
    // 3. Sửa món
    // -------------------------------------------------------
    private void handleEditItem() {
        System.out.println("\n--- SỬA THÔNG TIN MÓN ---");
        int id = InputUtils.inputInt("Nhập Menu Item ID cần sửa: ", 1, Integer.MAX_VALUE);

        try {
            FbMenuItem existing = menuService.getMenuItemById(id);
            System.out.println("Đang sửa: " + existing.getName());

            String name = InputUtils.inputStringUpdate(
                    "Tên mới (Cũ: " + existing.getName() + ") [Enter giữ nguyên]: ", existing.getName());
            String desc = InputUtils.inputStringUpdate(
                    "Mô tả mới [Enter giữ nguyên]: ", existing.getDescription());
            BigDecimal price = InputUtils.inputBigDecimalUpdate(
                    "Giá gốc mới [Enter giữ nguyên]: ", existing.getBasePrice(), BigDecimal.ZERO);
            int stock = InputUtils.inputIntUpdate(
                    "Tồn kho mới [Enter giữ nguyên]: ", existing.getStockQuantity(), 0, 99999);
            int prepTime = InputUtils.inputIntUpdate(
                    "Thời gian chuẩn bị (phút) [Enter giữ nguyên]: ", existing.getPrepTimeInMinutes(), 0, 120);
            String tags = InputUtils.inputStringUpdate(
                    "Tags [Enter giữ nguyên]: ", existing.getItemTags() != null ? existing.getItemTags() : "");
            String avail = InputUtils.inputStringUpdate(
                    "Khung giờ [Enter giữ nguyên]: ", existing.getAvailability() != null ? existing.getAvailability() : "ALL");

            existing.setName(name);
            existing.setDescription(desc);
            existing.setBasePrice(price);
            existing.setStockQuantity(stock);
            existing.setPrepTimeInMinutes(prepTime);
            existing.setItemTags(tags);
            existing.setAvailability(avail);
            existing.setStatus(stock == 0 ? "OUT_OF_STOCK" : "ACTIVE");

            menuService.updateMenuItem(existing);
            PrintUtils.printSuccess("Cập nhật món thành công!");
        } catch (BusinessException e) {
            PrintUtils.printError(e.getMessage());
        }
    }

    // -------------------------------------------------------
    // 4. Ẩn/Xoá mềm món
    // -------------------------------------------------------
    private void handleDeleteItem() {
        System.out.println("\n--- ẨN/XOÁ MỀM MÓN ---");
        int id = InputUtils.inputInt("Nhập Menu Item ID cần ẩn: ", 1, Integer.MAX_VALUE);
        String confirm = InputUtils.inputString("Xác nhận ẩn món này? (Y/N): ", "^[YyNn]$", "Chỉ nhập Y hoặc N.");

        if (confirm.equalsIgnoreCase("Y")) {
            try {
                menuService.deleteMenuItem(id);
                PrintUtils.printSuccess("Đã ẩn món ID=%d khỏi menu (status = HIDDEN).", id);
            } catch (BusinessException e) {
                PrintUtils.printError(e.getMessage());
            }
        } else {
            System.out.println("Đã hủy thao tác.");
        }
    }

    // -------------------------------------------------------
    // 5. Quản lý Topping
    // -------------------------------------------------------
    private void manageToppings() {
        while (true) {
            System.out.println("\n--- QUẢN LÝ TOPPING ---");
            System.out.println("1. Xem danh sách Topping");
            System.out.println("2. Thêm Topping mới");
            System.out.println("0. Quay lại");

            int choice = InputUtils.inputInt("Chọn (0-2): ", 0, 2);
            switch (choice) {
                case 1: displayToppingList(); break;
                case 2: handleAddTopping();   break;
                case 0: return;
            }
        }
    }

    private void displayToppingList() {
        try {
            List<Map<String, Object>> toppings = menuService.getAllToppings();
            System.out.println("\n" + "-".repeat(50));
            System.out.printf("%-5s | %-25s | %-12s%n", "ID", "Tên Topping", "Phụ phí");
            System.out.println("-".repeat(50));
            if (toppings.isEmpty()) {
                System.out.println("  Chưa có topping nào.");
            } else {
                for (Map<String, Object> t : toppings) {
                    System.out.printf("%-5d | %-25s | %-12s%n",
                            t.get("topping_id"),
                            t.get("name"),
                            FormatUtils.formatVND((BigDecimal) t.get("extra_price")));
                }
            }
            System.out.println("-".repeat(50));
        } catch (BusinessException e) {
            PrintUtils.printError(e.getMessage());
        }
    }

    private void handleAddTopping() {
        System.out.println("\n--- THÊM TOPPING MỚI ---");
        String name = InputUtils.inputString("Tên topping: ");
        BigDecimal price = InputUtils.inputBigDecimal("Phụ phí (VND, nhập 0 nếu miễn phí): ", BigDecimal.ZERO);

        try {
            int newId = menuService.createTopping(name, price);
            PrintUtils.printSuccess("Đã thêm topping '%s' (ID=%d) với phụ phí %s.",
                    name, newId, FormatUtils.formatVND(price));
        } catch (BusinessException e) {
            PrintUtils.printError(e.getMessage());
        }
    }
}
