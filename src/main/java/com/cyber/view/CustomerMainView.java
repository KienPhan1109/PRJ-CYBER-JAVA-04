package com.cyber.view;


import com.cyber.domain.fb.FbMenuItem;
import com.cyber.domain.fb.IBillable;
import com.cyber.domain.fb.SingleItem;
import com.cyber.domain.fb.SizeDecorator;
import com.cyber.domain.fb.ToppingDecorator;
import com.cyber.domain.fb.discount.FixedAmountDiscountStrategy;
import com.cyber.domain.fb.discount.IDiscountStrategy;
import com.cyber.domain.fb.discount.NoDiscountStrategy;
import com.cyber.domain.fb.discount.PercentageDiscountStrategy;
import com.cyber.exception.BusinessException;
import com.cyber.model.*;
import com.cyber.model.enums.ComputerZone;
import com.cyber.model.enums.FBStatus;
import com.cyber.model.enums.FbTemperature;
import com.cyber.service.BookingService;
import com.cyber.service.ComputerService;
import com.cyber.service.FbMenuService;
import com.cyber.service.FbOrderService;
import com.cyber.service.FbOrderService.FbAdvancedCartItem;
import com.cyber.util.FormatUtils;
import com.cyber.util.InputUtils;
import com.cyber.util.PrintUtils;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CustomerMainView {

    private final User            customerUser;
    private final BookingService  bookingService;
    private final ComputerService computerService;
    private final FbMenuService   menuService;
    private final FbOrderService  orderService;

    private Integer currentBookingId = null;

    public CustomerMainView(User customerUser) {
        this.customerUser    = customerUser;
        this.bookingService  = BookingService.getInstance();
        this.computerService = ComputerService.getInstance();
        this.menuService     = FbMenuService.getInstance();
        this.orderService    = FbOrderService.getInstance();
    }

    public void displayMenu() {
        while (true) {
            System.out.println("\n==========================================");
            System.out.println("         KHÁCH HÀNG (CUSTOMER) PANEL      ");
            System.out.println("         Xin chào: " + customerUser.getFullName());
            System.out.println("         Số dư khả dụng: " + FormatUtils.formatVND(customerUser.getBalance()));
            System.out.println("==========================================");
            System.out.println("1. Đặt máy trạm (Booking)");
            System.out.println("2. Đặt đồ ăn / Thức uống (F&B)");
            System.out.println("3. Xem trạng thái dịch vụ (Máy & Món đã đặt)");
            System.out.println("4. Tra cứu phiên chơi hiện tại (Session Status)");
            System.out.println("0. Đăng xuất");
            System.out.println("==========================================");

            int choice = InputUtils.inputInt("Vui lòng chọn chức năng (0-4): ", 0, 4);

            try {
                switch (choice) {
                    case 1: bookComputerFlow(); break;
                    case 2: orderFoodFlow();    break;
                    case 3: viewCurrentStatus(); break;
                    case 4: displayCurrentSession(); break;
                    case 0:
                        PrintUtils.printWarning("Đang đăng xuất khỏi hệ thống Khách hàng...");
                        return;
                }
            } catch (BusinessException e) {
                PrintUtils.printError(e.getMessage());
            } catch (Exception e) {
                PrintUtils.printError("Lỗi hệ thống: " + e.getMessage());
            }
        }
    }

    private void displayCurrentSession() {
        try {
            // Lấy balance realtime qua Service (tuân thủ 3-Tier)
            try {
                User currentUserRealtime = com.cyber.service.UserService.getInstance()
                        .getUserById(customerUser.getUserId());
                customerUser.setBalance(currentUserRealtime.getBalance());
            } catch (BusinessException ex) {
                // Nếu lỗi thì giữ balance cũ, không crash
            }

            List<Booking> activeBookings = bookingService.getActiveBookingsByUserId(customerUser.getUserId());
            if (activeBookings.isEmpty()) {
                PrintUtils.printWarning("Bạn không có phiên chơi nào đang hoạt động.");
                return;
            }

            Booking active = activeBookings.get(0);
            
            BigDecimal hourly = active.getHourlyRateSnapshot();
            if (hourly == null) hourly = BigDecimal.ZERO;
            BigDecimal ratePerMinute = hourly.divide(new BigDecimal(60), 2, java.math.RoundingMode.HALF_UP);
            
            long diffInMillis = System.currentTimeMillis() - active.getStartTime().getTime();
            long minutesUsed = diffInMillis / (1000 * 60);
            
            BigDecimal spent = ratePerMinute.multiply(new BigDecimal(minutesUsed)).setScale(2, java.math.RoundingMode.HALF_UP);
            
            long expectedRemainingMins = 0;
            if (ratePerMinute.compareTo(BigDecimal.ZERO) > 0) {
                expectedRemainingMins = customerUser.getBalance().divide(ratePerMinute, 0, java.math.RoundingMode.DOWN).longValue();
            }

            System.out.println("\n--- TRẠNG THÁI PHIÊN CHƠI HIỆN TẠI ---");
            PrintUtils.printTableSeparator(70);
            System.out.printf("| %-30s | %-33s |\n", "Thông số", "Giá trị");
            PrintUtils.printTableSeparator(70);
            System.out.printf("| %-30s | %-33s |\n", "Tên máy", active.getComputerName() != null ? active.getComputerName() : active.getComputerId());
            System.out.printf("| %-30s | %-33s |\n", "Thời gian bắt đầu", active.getStartTime().toString());
            System.out.printf("| %-30s | %-33s |\n", "Thời gian đã sử dụng", minutesUsed + " phút");
            System.out.printf("| %-30s | %-33s |\n", "Tiền giờ đã chi", FormatUtils.formatVND(spent));
            System.out.printf("| %-30s | %-33s |\n", "Số dư còn lại (Realtime)", FormatUtils.formatVND(customerUser.getBalance()));
            System.out.printf("| %-30s | %-33s |\n", "Thời gian còn lại dự kiến", expectedRemainingMins + " phút");
            PrintUtils.printTableSeparator(70);

        } catch (BusinessException e) {
            PrintUtils.printError(e.getMessage());
        }
    }

    // =========================================================
    // BOOKING FLOW (giữ nguyên từ Phase 1)
    // =========================================================

    private void bookComputerFlow() throws BusinessException {
        System.out.println("\n--- ĐẶT MÁY TRẠM ---");
        System.out.println("Lưu ý: Tiền cọc sẽ được trừ ngay lập tức vào số dư.");
        int hours = InputUtils.inputInt("Bạn muốn đặt máy trong mấy giờ? (Tối thiểu 1h): ", 1, 24);

        System.out.println("Chọn khu vực:");
        System.out.println("1. VIP | 2. STANDARD | 3. ESPORT | 4. STREAMING | 5. COUPLE | 6. BẤT KỲ");
        int zoneChoice = InputUtils.inputInt("Chọn (1-6): ", 1, 6);

        ComputerZone zone = null;
        switch (zoneChoice) {
            case 1: zone = ComputerZone.VIP;       break;
            case 2: zone = ComputerZone.STANDARD;  break;
            case 3: zone = ComputerZone.ESPORT;    break;
            case 4: zone = ComputerZone.STREAMING; break;
            case 5: zone = ComputerZone.COUPLE;    break;
        }

        Timestamp start = new Timestamp(System.currentTimeMillis());
        Timestamp end   = new Timestamp(System.currentTimeMillis() + (hours * 3600000L));
        List<Computer> availableComputers = computerService.getAvailableComputersByZone(zone, start, end);
        if (availableComputers.isEmpty()) {
            PrintUtils.printWarning("Rất tiếc! Hiện tại không có máy trống nào phù hợp ở khu vực bạn chọn.");
            return;
        }

        System.out.println("\nDANH SÁCH MÁY TRỐNG:");
        System.out.printf("%-5s | %-15s | %-15s | %-30s | %-15s%n", "ID", "Tên Máy", "Khu Vực", "Cấu hình", "Đơn giá/h");
        System.out.println("-".repeat(87));
        for (Computer c : availableComputers) {
            System.out.printf("%-5d | %-15s | %-15s | %-30s | %-15s%n",
                c.getComputerId(), c.getName(), c.getZone(), c.getHardwareConfig(),
                FormatUtils.formatVND(c.getPricePerHour()));
        }
        System.out.println("-".repeat(87));

        int computerId = InputUtils.inputInt("Nhập ID máy muốn đặt (0 để hủy): ", 0, Integer.MAX_VALUE);
        if (computerId == 0) { System.out.println("Đã hủy đặt máy."); return; }

        Computer targetComputer = availableComputers.stream()
                .filter(c -> c.getComputerId() == computerId).findFirst().orElse(null);
        if (targetComputer == null) { PrintUtils.printError("ID máy không hợp lệ."); return; }

        BigDecimal fee = targetComputer.getPricePerHour().multiply(new BigDecimal(hours));
        System.out.println("\nDỰ TOÁN THUÊ MÁY:");
        System.out.printf("  Máy: %s | Thời gian: %d giờ | Chi phí: %s%n",
                targetComputer.getName(), hours, FormatUtils.formatVND(fee));
        System.out.println("  Số dư của bạn: " + FormatUtils.formatVND(customerUser.getBalance()));

        String confirm = InputUtils.inputString("Xác nhận Đặt và Trừ Tiền? (Y/N): ");
        if (confirm.equalsIgnoreCase("y")) {
            Booking newBooking = new Booking(0, customerUser.getUserId(),
                    targetComputer.getComputerId(), start, end, "IN_PROGRESS", fee, targetComputer.getPricePerHour());
            this.currentBookingId = bookingService.bookComputer(customerUser.getUserId(), newBooking);
            customerUser.setBalance(customerUser.getBalance().subtract(fee));
            PrintUtils.printSuccess("Đặt máy thành công! Bắt đầu sử dụng máy " + targetComputer.getName());
        } else {
            System.out.println("Đã hủy đặt máy.");
        }
    }

    private void viewCurrentStatus() throws BusinessException {
        System.out.println("\n--- TRẠNG THÁI DỊCH VỤ HIỆN TẠI ---");

        // 1. Xem Máy
        List<Booking> activeBookings = bookingService.getActiveBookingsByUserId(customerUser.getUserId());
        if (activeBookings.isEmpty()) {
            System.out.println("Máy trạm: Không có máy nào đang đặt.");
        } else {
            System.out.println("Máy trạm đang đặt:");
            for (Booking b : activeBookings) {
                System.out.printf(" - %s | Từ: %s Đến: %s | Trạng thái: %s\n",
                        b.getComputerName(), b.getStartTime(), b.getEndTime(), b.getStatus());
            }
        }

        // 2. Xem Đồ ăn (F&B Orders)
        List<FbOrder> myOrders = orderService.getActiveOrdersByUserId(customerUser.getUserId());
        if (myOrders.isEmpty()) {
            System.out.println("\nĐồ ăn & Thức uống: Không có đơn hàng nào chờ.");
        } else {
            System.out.println("\nĐồ ăn & Thức uống đang xử lý:");
            for (FbOrder o : myOrders) {
                System.out.printf(" - Đơn #%d | Bàn: %s | Tiền: %s | Trạng thái: %s\n",
                        o.getOrderId(),
                        o.getComputerName(),
                        FormatUtils.formatVND(o.getTotalAmount()),
                        o.getStatus());
                        
                // Hiển thị món ăn
                List<Map<String, Object>> details = orderService.getOrderDetails(o.getOrderId());
                for (Map<String, Object> d : details) {
                    int qty = ((Number) d.get("quantity")).intValue();
                    String desc = (String) d.get("item_description");
                    System.out.printf("     > %s x%d\n", desc, qty);
                }
            }
        }
    }

    // =========================================================
    // ORDER FOOD FLOW — F&B ADVANCED (Phase 2)
    // Áp dụng: Composite + Decorator + Strategy Patterns
    // =========================================================

    private void orderFoodFlow() throws BusinessException {
        // ---- Chọn Strategy giảm giá ----
        IDiscountStrategy strategy = selectDiscountStrategy();

        // ---- Tải menu ----
        List<FbMenuItem> menuItems = menuService.getAllActiveMenuItems();
        if (menuItems.isEmpty()) {
            PrintUtils.printWarning("Hiện không có món nào trong menu.");
            return;
        }

        // ---- Tải danh sách Topping ----
        List<Map<String, Object>> allToppings = menuService.getAllToppings();

        // ---- Hiển thị menu ----
        printMenuTable(menuItems);

        // ---- Giỏ hàng ----
        List<FbAdvancedCartItem> cart = new ArrayList<>();
        BigDecimal cartTotal = BigDecimal.ZERO;

        while (true) {
            System.out.println("\n>>> Nhập ID món muốn gọi (0 = Chốt đơn / Thoát)");
            int menuItemId = InputUtils.inputInt("Menu Item ID: ", 0, Integer.MAX_VALUE);
            if (menuItemId == 0) break;

            // Tìm món
            FbMenuItem selectedItem = menuItems.stream()
                    .filter(m -> m.getMenuItemId() == menuItemId).findFirst().orElse(null);
            if (selectedItem == null) {
                PrintUtils.printError("Không tìm thấy món có ID=%d trong menu.", menuItemId);
                continue;
            }
            if (selectedItem.getStockQuantity() <= 0) {
                PrintUtils.printError("Món '%s' đã hết hàng.", selectedItem.getName());
                continue;
            }

            int qty = InputUtils.inputInt("Số lượng: ", 1, selectedItem.getStockQuantity());

            // Món lẻ flow
            FbAdvancedCartItem singleCartItem = buildSingleItem(selectedItem, allToppings, strategy, qty);
            cart.add(singleCartItem);
            cartTotal = cartTotal.add(singleCartItem.getFinalPrice());
            PrintUtils.printSuccess("Đã thêm [%s] vào giỏ. Đơn giá: %s",
                    singleCartItem.getItemDescription(),
                    FormatUtils.formatVND(singleCartItem.getFinalPrice()));
        }

        if (cart.isEmpty()) {
            System.out.println("Giỏ hàng trống, không có đơn hàng nào được tạo.");
            return;
        }

        // ---- Hiển thị hoá đơn dự kiến ----
        printCartSummary(cart, cartTotal, strategy);

        String payConfirm = InputUtils.inputString("Xác nhận thanh toán? (Y/N): ");
        if (payConfirm.equalsIgnoreCase("y")) {
            orderService.orderFoodAdvanced(customerUser.getUserId(), currentBookingId, cart);
            customerUser.setBalance(customerUser.getBalance().subtract(cartTotal));
            PrintUtils.printSuccess("Đặt đồ ăn thành công! Đơn hàng đang chờ xử lý.");
        } else {
            System.out.println("Đã hủy đơn F&B.");
        }
    }

    // =========================================================
    // Helpers — Build IBillable và FbAdvancedCartItem
    // =========================================================

    /**
     * Cho khách chọn Strategy giảm giá trước khi order.
     */
    private IDiscountStrategy selectDiscountStrategy() {
        System.out.println("\n--- ÁP DỤNG GIẢM GIÁ ---");
        System.out.println("1. Không giảm (Khách thường)");
        System.out.println("2. Thẻ VIP — Giảm 10%");
        System.out.println("3. Voucher — Giảm 20.000đ");

        int choice = InputUtils.inputInt("Chọn loại giảm giá (1-3): ", 1, 3);
        switch (choice) {
            case 2: return new PercentageDiscountStrategy(10, "VIP_10_PERCENT");
            case 3: return new FixedAmountDiscountStrategy(new BigDecimal("20000"), "VOUCHER_20K");
            default: return NoDiscountStrategy.getInstance();
        }
    }

    /**
     * Build một món lẻ (SingleItem) với Decorator (Size + Topping) và Strategy.
     */
    private FbAdvancedCartItem buildSingleItem(FbMenuItem item,
                                               List<Map<String, Object>> allToppings,
                                               IDiscountStrategy strategy,
                                               int quantity) throws BusinessException {
        IBillable billable = new SingleItem(item);
        System.out.println("\n  >> Tuỳ chỉnh món: " + item.getName() + " (Giá gốc: " + FormatUtils.formatVND(item.getBasePrice()) + ")");

        // Chọn Size nếu là đồ uống
        if (item.getTemperatureLevel() == FbTemperature.COLD
                || item.getTemperatureLevel() == FbTemperature.HOT
                || item.getTemperatureLevel() == FbTemperature.ICED) {
            System.out.println("  Chọn size: 1=M (giữ nguyên)  2=L (+10.000đ)  3=S (giữ nguyên)");
            int sizeChoice = InputUtils.inputInt("  Chọn size (1-3): ", 1, 3);
            SizeDecorator.SizeType sizeType = sizeChoice == 2
                    ? SizeDecorator.SizeType.L
                    : (sizeChoice == 3 ? SizeDecorator.SizeType.S : SizeDecorator.SizeType.M);
            billable = new SizeDecorator(billable, sizeType);
        }

        // Thêm Topping
        billable = applyToppings(billable, allToppings);

        // Áp dụng Strategy
        BigDecimal priceBeforeDiscount = billable.calculatePrice();
        BigDecimal finalPrice = strategy.applyDiscount(priceBeforeDiscount);
        BigDecimal discountAmt = strategy.calculateDiscountAmount(priceBeforeDiscount);

        String configJson = buildConfigJson(item.getMenuItemId(), billable.getDescription(), strategy.getStrategyName());

        return new FbAdvancedCartItem(
                item.getMenuItemId(), quantity, finalPrice,
                billable.getDescription(), configJson,
                discountAmt, strategy.getStrategyName()
        );
    }



    /**
     * Hỏi khách có muốn thêm Topping không. Có thể thêm nhiều tầng.
     */
    private IBillable applyToppings(IBillable billable, List<Map<String, Object>> allToppings) {
        if (allToppings.isEmpty()) return billable;

        System.out.println("\n  Danh sách Topping:");
        for (Map<String, Object> t : allToppings) {
            System.out.printf("    ID=%-3d | %-20s (+%s)%n",
                    t.get("topping_id"), t.get("name"),
                    FormatUtils.formatVND((BigDecimal) t.get("extra_price")));
        }

        while (true) {
            System.out.println("  Nhập Topping ID để thêm (0 = không thêm / kết thúc):");
            int tId = InputUtils.inputInt("  Topping ID: ", 0, Integer.MAX_VALUE);
            if (tId == 0) break;

            Map<String, Object> topping = allToppings.stream()
                    .filter(t -> ((Number) t.get("topping_id")).intValue() == tId)
                    .findFirst().orElse(null);
            if (topping == null) {
                PrintUtils.printError("  Không tìm thấy Topping ID=%d.", tId);
                continue;
            }
            billable = new ToppingDecorator(billable,
                    ((Number) topping.get("topping_id")).intValue(),
                    (String) topping.get("name"),
                    (BigDecimal) topping.get("extra_price"));
            System.out.println("  Đã thêm Topping: " + topping.get("name"));
        }
        return billable;
    }

    /**
     * In bảng menu ra console.
     */
    private void printMenuTable(List<FbMenuItem> menuItems) {
        System.out.println("\n" + "=".repeat(100));
        System.out.println("  MENU F&B");
        System.out.println("=".repeat(100));
        System.out.printf("%-5s | %-10s | %-28s | %-12s | %-8s | %-6s | %-15s%n",
                "ID", "Danh mục", "Tên món", "Giá gốc", "Tồn kho", "T.gian", "Tags");
        System.out.println("-".repeat(100));
        for (FbMenuItem m : menuItems) {
            String stockStr = m.getStockQuantity() > 0 ? String.valueOf(m.getStockQuantity()) : "[HẾT HÀNG]";
            String nameCol = m.getStatus() == FBStatus.OUT_OF_STOCK 
                             ? PrintUtils.colorText(m.getName(), "YELLOW") 
                             : m.getName();

            System.out.printf("%-5d | %-10s | %-28s | %-12s | %-8s | %-6d' | %-15s%n",
                    m.getMenuItemId(),
                    m.getCategoryName() != null ? m.getCategoryName() : "-",
                    nameCol,
                    FormatUtils.formatVND(m.getBasePrice()),
                    stockStr,
                    m.getPrepTimeInMinutes(),
                    m.getItemTags() != null ? m.getItemTags() : "-");
        }
        System.out.println("=".repeat(100));
    }

    /**
     * In hoá đơn tổng trước khi xác nhận thanh toán.
     */
    private void printCartSummary(List<FbAdvancedCartItem> cart, BigDecimal total,
                                  IDiscountStrategy strategy) {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("  HOÁ ĐƠN DỰ KIẾN");
        System.out.println("=".repeat(80));
        BigDecimal totalDiscount = BigDecimal.ZERO;
        for (FbAdvancedCartItem item : cart) {
            System.out.printf("  %-45s x%-3d = %s%n",
                    truncate(item.getItemDescription(), 45),
                    item.getQuantity(),
                    FormatUtils.formatVND(item.getFinalPrice()));
            if (item.getDiscountApplied().compareTo(BigDecimal.ZERO) > 0) {
                System.out.printf("    → Giảm (%s): -%s%n",
                        item.getDiscountStrategyName(),
                        FormatUtils.formatVND(item.getDiscountApplied()));
                totalDiscount = totalDiscount.add(item.getDiscountApplied());
            }
        }
        System.out.println("-".repeat(80));
        if (totalDiscount.compareTo(BigDecimal.ZERO) > 0) {
            System.out.printf("  %-48s %s%n", "Tổng giảm giá:", "-" + FormatUtils.formatVND(totalDiscount));
        }
        System.out.printf("  %-48s %s%n", "TỔNG THANH TOÁN:", FormatUtils.formatVND(total));
        System.out.println("  Số dư hiện tại: " + FormatUtils.formatVND(customerUser.getBalance()));
        System.out.println("=".repeat(80));
    }

    /**
     * Build chuỗi JSON đơn giản để lưu config Decorator vào DB.
     */
    private String buildConfigJson(int menuItemId, String description, String strategyName) {
        return String.format(
                "{\"menuItemId\":%d,\"description\":\"%s\",\"strategy\":\"%s\"}",
                menuItemId,
                description.replace("\"", "'"),
                strategyName
        );
    }

    private String truncate(String s, int maxLen) {
        if (s == null) return "";
        return s.length() <= maxLen ? s : s.substring(0, maxLen - 3) + "...";
    }
}
