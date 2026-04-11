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
            System.out.println("5. Quản lý Topping dùng chung");
            System.out.println("6. Quản lý Tùy chọn của món (Size/Đường/Đá)");
            System.out.println("0. Quay lại");

            int choice = InputUtils.inputInt("Chọn chức năng (0-6): ", 0, 6);
            switch (choice) {
                case 1 -> displayMenuList();
                case 2 -> handleAddItem();
                case 3 -> handleEditItem();
                case 4 -> handleDeleteItem();
                case 5 -> manageToppings();
                case 6 -> manageItemOptions();
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
        System.out.println("1. FOOD | 2. DRINK | 3.SNACK");
        int categoryId = InputUtils.inputInt("Lựa chọn (1-3): ", 1, 3);

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
            System.out.println("Đang sửa: " + existing.getName());

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

    // -------------------------------------------------------
    // 5. Quản lý Topping (có stock_quantity + status)
    // -------------------------------------------------------
    private void manageToppings() {
        while (true) {
            System.out.println("\n--- QUẢN LÝ TOPPING ---");
            System.out.println("1. Xem danh sách Topping");
            System.out.println("2. Thêm Topping mới");
            System.out.println("3. Sửa Topping");
            System.out.println("4. Ẩn / Hiện Topping (Toggle)");
            System.out.println("0. Quay lại");

            int choice = InputUtils.inputInt("Chọn (0-4): ", 0, 4);
            switch (choice) {
                case 1: displayToppingList(); break;
                case 2: handleAddTopping();   break;
                case 3: handleEditTopping();  break;
                case 4: handleDeleteTopping(); break;
                case 0: return;
            }
        }
    }

    private void displayToppingList() {
        try {
            // Admin xem toàn bộ kể cả HIDDEN/OUT_OF_STOCK
            List<Map<String, Object>> toppings = menuService.getAllToppingsForAdmin();
            System.out.println("\n" + "=".repeat(80));
            System.out.println("                          DANH SÁCH TOPPING (ADMIN)");
            System.out.println("=".repeat(80));
            System.out.printf("%-5s | %-25s | %-12s | %-10s | %-15s%n",
                    "ID", "Tên Topping", "Phụ phí", "Tồn kho", "Trạng thái");
            System.out.println("-".repeat(80));
            if (toppings.isEmpty()) {
                System.out.println("  Chưa có topping nào.");
            } else {
                for (Map<String, Object> t : toppings) {
                    String status = (String) t.get("status");
                    String statusDisplay = switch (status) {
                        case "ACTIVE"       -> "\033[32mACTIVE\033[0m";
                        case "OUT_OF_STOCK" -> "\033[33mHẾT HÀNG\033[0m";
                        case "HIDDEN"       -> "\033[31mĐÃ KHOÁ\033[0m";
                        default             -> status;
                    };
                    System.out.printf("%-5d | %-25s | %-12s | %-10d | %-15s%n",
                            t.get("topping_id"),
                            t.get("name"),
                            FormatUtils.formatVND((BigDecimal) t.get("extra_price")),
                            (int) t.get("stock_quantity"),
                            statusDisplay);
                }
            }
            System.out.println("=".repeat(80));
        } catch (BusinessException e) {
            PrintUtils.printError(e.getMessage());
        }
    }

    private void handleAddTopping() {
        System.out.println("\n--- THÊM TOPPING MỚI ---");
        String name = InputUtils.inputString("Tên topping: ");
        BigDecimal price = InputUtils.inputBigDecimal("Phụ phí (VND, nhập 0 nếu miễn phí): ", BigDecimal.ZERO);
        int stockQty = InputUtils.inputInt("Tồn kho ban đầu: ", 0, 99999);

        try {
            int newId = menuService.createTopping(name, price, stockQty, adminUser);
            PrintUtils.printSuccess("Đã thêm topping '%s' (ID=%d) với phụ phí %s, tồn kho: %d.",
                    name, newId, FormatUtils.formatVND(price), stockQty);
        } catch (BusinessException e) {
            PrintUtils.printError(e.getMessage());
        }
    }

    private void handleEditTopping() {
        System.out.println("\n--- SỬA TOPPING ---");
        int id = InputUtils.inputInt("Nhập Topping ID cần sửa: ");
        try {
            List<Map<String, Object>> list = menuService.getAllToppingsForAdmin();
            Map<String, Object> existing = list.stream()
                    .filter(t -> (int)t.get("topping_id") == id)
                    .findFirst().orElse(null);
            
            if (existing == null) {
                PrintUtils.printWarning("Không tìm thấy Topping ID=" + id);
                return;
            }

            String currentName = (String) existing.get("name");
            BigDecimal currentPrice = (BigDecimal) existing.get("extra_price");
            int currentStock = (int) existing.get("stock_quantity");

            String name = InputUtils.inputStringUpdate("Tên mới (Cũ: " + currentName + ") [Enter giữ nguyên]: ", currentName);
            BigDecimal price = InputUtils.inputBigDecimalUpdate("Phụ phí mới [Enter giữ nguyên]: ", currentPrice, BigDecimal.ZERO);
            int stockQty = InputUtils.inputIntUpdate("Tồn kho mới (Cũ: " + currentStock + ") [Enter giữ nguyên]: ", currentStock, 0, 99999);

            menuService.updateTopping(id, name, price, stockQty, adminUser);
            PrintUtils.printSuccess("Cập nhật Topping thành công!");
        } catch (BusinessException e) {
            PrintUtils.printError(e.getMessage());
        }
    }

    private void handleDeleteTopping() {
        System.out.println("\n--- ẨN / HIỆN TOPPING (TOGGLE) ---");
        int id = InputUtils.inputInt("Nhập Topping ID cần thay đổi trạng thái: ");
        String confirm = InputUtils.inputString("Xác nhận thay đổi trạng thái topping này? (Y/N): ", "^[YyNn]$", "Chỉ nhập Y hoặc N.");
        if (confirm.equalsIgnoreCase("y")) {
            try {
                menuService.toggleToppingStatus(id, adminUser);
                PrintUtils.printSuccess("Đã thay đổi trạng thái Topping ID=%d thành công.", id);
            } catch (
                    BusinessException e) {
                PrintUtils.printError(e.getMessage());
            }
        } else {
            System.out.println("Đã hủy thao tác.");
        }
    }

    // -------------------------------------------------------
    // 6. Quản lý Tùy chọn của Món (Item Options)
    // -------------------------------------------------------
    private void manageItemOptions() {
        System.out.println("\n--- QUẢN LÝ TÙY CHỌN MÓN (SIZE, ĐƯỜNG, ĐÁ,...) ---");
        int itemId = InputUtils.inputInt("Nhập ID của món cần quản lý (Menu Item ID): ", 1, Integer.MAX_VALUE);
        
        try {
            FbMenuItem item = menuService.getMenuItemById(itemId);
            System.out.println("=> Đang cấu hình Menu Item: " + item.getName());
            
            while (true) {
                System.out.println("\n1. Xem danh sách Option hiện tại của: " + item.getName());
                System.out.println("2. Thêm Option mới");
                System.out.println("3. Xóa Option");
                System.out.println("0. Quay lại mục trước");
                
                int optChoice = InputUtils.inputInt("Lựa chọn (0-3): ", 0, 3);
                switch (optChoice) {
                    case 1: displayItemOptions(itemId); break;
                    case 2: handleAddItemOption(itemId); break;
                    case 3: handleRemoveItemOption(itemId); break;
                    case 0: return;
                }
            }
        } catch (BusinessException e) {
            PrintUtils.printError(e.getMessage());
        }
    }

    private void displayItemOptions(int menuItemId) {
        try {
            List<Map<String, Object>> options = menuService.getOptionsByMenuItemId(menuItemId);
            System.out.println("\n--- DANH SÁCH OPTION ---\n");
            if (options.isEmpty()) {
                System.out.println("Chưa có bất kỳ tùy chọn nào được cấu hình cho món này.");
                return;
            }
            
            System.out.printf("%-5s | %-15s | %-15s | %-15s%n", "ID", "Phân Loại", "Nhãn", "Phụ phí");
            System.out.println("-".repeat(55));
            for (Map<String, Object> opt : options) {
                System.out.printf("%-5d | %-15s | %-15s | %-15s%n",
                        opt.get("option_id"),
                        opt.get("option_type"),
                        opt.get("option_label"),
                        FormatUtils.formatVND((BigDecimal) opt.get("extra_price")));
            }
        } catch (BusinessException e) {
            PrintUtils.printError(e.getMessage());
        }
    }

    private void handleAddItemOption(int menuItemId) {
        System.out.println("\n-- THÊM TÙY CHỌN MỚI --");
        System.out.println("Chọn phân loại Tùy chọn (Enum database mapping):");
        System.out.println("1. SIZE");
        System.out.println("2. SUGAR_LEVEL");
        System.out.println("3. ICE_LEVEL");
        System.out.println("4. WEIGHT");
        System.out.println("5. Khác (OTHER)");
        int typeChoice = InputUtils.inputInt("Chọn (1-5): ", 1, 5);
        String optionType = switch (typeChoice) {
            case 1 -> "SIZE";
            case 2 -> "SUGAR_LEVEL";
            case 3 -> "ICE_LEVEL";
            case 4 -> "WEIGHT";
            default -> "OTHER";
        };
        
        String optionLabel = InputUtils.inputString("Nhãn hiển thị (VD: Size M, 50% Đường...): ");
        BigDecimal extraPrice = InputUtils.inputBigDecimal("Phụ phí (VND, nhập 0 nếu miễn phí): ", BigDecimal.ZERO);
        
        try {
            menuService.addOptionToItem(menuItemId, optionType, optionLabel, extraPrice, adminUser);
            PrintUtils.printSuccess("Thêm tùy chọn thành công!");
        } catch (BusinessException e) {
            PrintUtils.printError(e.getMessage());
        }
    }

    private void handleRemoveItemOption(int menuItemId) {
        displayItemOptions(menuItemId);
        int optId = InputUtils.inputInt("Nhập ID của Option cần xóa: ", 1, Integer.MAX_VALUE);
        String confirm = InputUtils.inputString("Xác nhận xóa Option này rĩnh viễn? (Y/N): ", "^[YyNn]$", "Chỉ nhập Y hoặc N.");
        if (confirm.equalsIgnoreCase("y")) {
            try {
                menuService.removeOptionFromItem(menuItemId, optId, adminUser);
                PrintUtils.printSuccess("Đã xóa tùy chọn ID=%d thành công.", optId);
            } catch (BusinessException e) {
                PrintUtils.printError(e.getMessage());
            }
        } else {
            System.out.println("Hủy thao tác.");
        }
    }
}
