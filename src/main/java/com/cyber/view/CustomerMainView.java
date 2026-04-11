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
            System.out.println("3. Xem lịch sử / trạng thái món đã đặt (F&B)");
            System.out.println("4. Tra cứu & Ngắt máy trạm (Session Status)");
            System.out.println("5. Đặt máy trước (Cọc 1h)");
            System.out.println("6. Lịch sử đặt máy");
            System.out.println("0. Đăng xuất");
            System.out.println("==========================================");

            int choice = InputUtils.inputInt("Vui lòng chọn chức năng (0-6): ", 0, 6);

            try {
                switch (choice) {
                    case 1: bookComputerFlow();     break;
                    case 2: orderFoodFlow();        break;
                    case 3: viewCurrentStatus();    break;
                    case 4: displayCurrentSession(); break;
                    case 5: reserveComputerFlow();  break;
                    case 6: viewBookingHistoryFlow(); break;
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

            // === PHẦN 1: Hiển thị các yêu cầu đang chờ duyệt (PENDING) ===
            List<Booking> allBookings = bookingService.getActiveBookingsByUserId(customerUser.getUserId());
            // Lấy danh sách PENDING riêng qua service
            List<Booking> pendingBookings = bookingService.getPendingBookings().stream()
                    .filter(b -> b.getUserId() == customerUser.getUserId())
                    .collect(java.util.stream.Collectors.toList());
            
            if (!pendingBookings.isEmpty()) {
                System.out.println("\n--- YÊU CẦU ĐANG CHỜ STAFF DUYỆT ---");
                for (Booking pending : pendingBookings) {
                    PrintUtils.printTableSeparator(70);
                    System.out.printf("| %-30s | %-33s |\n", "Booking ID", pending.getBookingId());
                    System.out.printf("| %-30s | %-33s |\n", "Tên máy", pending.getComputerName() != null ? pending.getComputerName() : pending.getComputerId());
                    System.out.printf("| %-30s | %-33s |\n", "Đơn giá/h", FormatUtils.formatVND(pending.getHourlyRateSnapshot()));
                    System.out.printf("| %-30s | %-33s |\n", "Trạng thái", PrintUtils.colorText("PENDING - Chờ duyệt", "YELLOW"));
                }
                PrintUtils.printTableSeparator(70);
            }

            // === PHẦN 2: Hiển thị các phiên chơi ACTIVE ===
            // allBookings chỉ chứa ACTIVE (đã fix query)
            if (allBookings.isEmpty() && pendingBookings.isEmpty()) {
                PrintUtils.printWarning("Bạn không có phiên chơi hoặc yêu cầu nào đang hoạt động.");
                return;
            }

            if (!allBookings.isEmpty()) {
                System.out.println("\n--- TRẠNG THÁI CÁC PHIÊN CHƠI HIỆN TẠI ---");

                // Tính tổng rate/giây của tất cả máy đang dùng (cho ước tính thời gian còn lại)
                BigDecimal totalRatePerSecond = BigDecimal.ZERO;
                for (Booking bg : allBookings) {
                    BigDecimal bgHourly = bg.getHourlyRateSnapshot();
                    if (bgHourly == null) bgHourly = BigDecimal.ZERO;
                    totalRatePerSecond = totalRatePerSecond.add(bgHourly.divide(new BigDecimal(3600), 4, java.math.RoundingMode.HALF_UP));
                }

                // Thời gian còn lại = số dư hiện tại / tổng rate (balance đã được heartbeat trừ rồi)
                long expectedRemainingSecs = 0;
                if (totalRatePerSecond.compareTo(BigDecimal.ZERO) > 0) {
                    expectedRemainingSecs = customerUser.getBalance().divide(totalRatePerSecond, 0, java.math.RoundingMode.DOWN).longValue();
                }

                for (Booking active : allBookings) {
                    long diffInMillis = System.currentTimeMillis() - active.getStartTime().getTime();
                    long secondsUsed = diffInMillis / 1000;
                    if (secondsUsed < 0) secondsUsed = 0;

                    String usedTimeStr = FormatUtils.formatDuration(secondsUsed);
                    String remainTimeStr = FormatUtils.formatDuration(expectedRemainingSecs);
                    if (expectedRemainingSecs < 300) {
                        remainTimeStr = "\033[31m" + remainTimeStr + " (SẮP HẾT GIỜ)\033[0m";
                    }

                    PrintUtils.printTableSeparator(70);
                    System.out.printf("| %-30s | %-33s |\n", "Tên máy", active.getComputerName() != null ? active.getComputerName() : active.getComputerId());
                    System.out.printf("| %-30s | %-33s |\n", "Thời gian bắt đầu", FormatUtils.formatTimestamp(active.getStartTime()));
                    System.out.printf("| %-30s | %-33s |\n", "Thời gian đã sử dụng", usedTimeStr);
                    System.out.printf("| %-30s | %-33s |\n", "Đơn giá/h", FormatUtils.formatVND(active.getHourlyRateSnapshot()));
                    System.out.printf("| %-30s | %-42s |\n", "Thời gian còn lại dự kiến", remainTimeStr);

                    // Always-on Transparency cho lịch RESERVED tiếp theo
                    Booking nextRes = bookingService.getNextReservation(active.getComputerId());
                    if (nextRes != null) {
                        long diffToRes = nextRes.getStartTime().getTime() - System.currentTimeMillis();
                        long minsToRes = diffToRes / 60000;
                        if (minsToRes < 0) minsToRes = 0;
                        
                        PrintUtils.printTableSeparator(70);
                        if (minsToRes > 5) {
                            System.out.printf("| %-66s |\n", "⚠️ TIẾP THEO: Có người đặt lúc " + FormatUtils.formatTimestamp(nextRes.getStartTime()) + " (Khoảng " + minsToRes + " phút nữa)");
                        } else {
                            System.out.println("| \033[31m[!!!] CẢNH BÁO ĐỎ: Hệ thống sẽ NGẮT MÁY BẢN TRONG " + minsToRes + " PHÚT TỚI!\033[0m |");
                            System.out.println("| \033[31m[!!!] Vui lòng lưu lại công việc của bạn ngay bây giờ.\033[0m               |");
                        }
                    }
                }
                PrintUtils.printTableSeparator(70);
                System.out.println("  Số dư khả dụng hiện tại: " + FormatUtils.formatVND(customerUser.getBalance()));
                System.out.println("  (Số dư được cập nhật mỗi 10 giây bởi hệ thống)");

                System.out.println("\n-------------------------------------------");
                System.out.println("Bạn có muốn ngắt (trả) máy trạm nào không?");
                int checkoutId = InputUtils.inputInt("Nhập ID của máy (booking_id hoặc computer_id đều được, nhập 0 để bỏ qua): ", 0, Integer.MAX_VALUE);
                
                if (checkoutId != 0) {
                    Booking targetToCheckout = null;
                    for (Booking b : allBookings) {
                        if (b.getBookingId() == checkoutId || b.getComputerId() == checkoutId) {
                            targetToCheckout = b;
                            break;
                        }
                    }
                    
                    if (targetToCheckout == null) {
                        PrintUtils.printWarning("Không tìm thấy ID hợp lệ trong danh sách máy đang thuê.");
                    } else {
                        String confirm = InputUtils.inputString("Xác nhận ngắt máy " + targetToCheckout.getComputerName() + "? (Y/N): ", "^[YyNn]$", "Chỉ nhập Y hoặc N");
                        if (confirm.equalsIgnoreCase("y")) {
                            bookingService.endSession(targetToCheckout.getBookingId());
                            PrintUtils.printSuccess("Đã ngắt máy " + targetToCheckout.getComputerName() + " thành công!");
                        }
                    }
                }
            }

        } catch (BusinessException e) {
            PrintUtils.printError(e.getMessage());
        }
    }

    // =========================================================
    // BOOKING FLOW (giữ nguyên từ Phase 1)
    // =========================================================

    private void bookComputerFlow() throws BusinessException {
        System.out.println("\n--- ĐẶT MÁY TRẠM ---");
        
        List<Booking> activeBookings = bookingService.getActiveBookingsByUserId(customerUser.getUserId());
        if (!activeBookings.isEmpty()) {
            PrintUtils.printWarning("Bạn đang sử dụng 1 máy trạm rồi. Vui lòng ngắt máy hiện tại trước khi thuê máy mới!");
            return;
        }
        
        // Kiểm tra xem có yêu cầu PENDING chưa được duyệt không
        List<Booking> pendingBookings = bookingService.getPendingBookings().stream()
                .filter(b -> b.getUserId() == customerUser.getUserId())
                .collect(java.util.stream.Collectors.toList());
        if (!pendingBookings.isEmpty()) {
            PrintUtils.printWarning("Bạn đã có yêu cầu mở máy đang chờ Staff duyệt. Vui lòng chờ hoặc liên hệ nhân viên!");
            return;
        }
        
        System.out.println("Lưu ý: Sau khi Staff duyệt, tiền máy sẽ được hệ thống trừ dần tự động mỗi 10 giây.");

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
        Timestamp end   = null;
        // Kiểm tra xem hiện tại máy còn trống không (start, null)
        List<Computer> availableComputers = computerService.getAvailableComputersByZone(zone, start, start);
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

        // Cảnh báo minh bạch (Transparency): Kiểm tra máy có lịch đặt trước không
        Booking nextRes = bookingService.getNextReservation(targetComputer.getComputerId());
        if (nextRes != null) {
            System.out.println();
            PrintUtils.printWarning("=================================================");
            PrintUtils.printWarning("⚠️ CHÚ Ý: MÁY ĐÃ CÓ NGƯỜI ĐẶT TRƯỚC VÀO LÚC " + FormatUtils.formatTimestamp(nextRes.getStartTime()));
            PrintUtils.printWarning("Hệ thống sẽ TỰ ĐỘNG NGẮT MÁY của bạn đúng vào giờ đó.");
            PrintUtils.printWarning("=================================================");
            String ans = InputUtils.inputString("Bạn vẫn muốn ngồi máy này chứ? (Y/N): ");
            if (!ans.equalsIgnoreCase("y")) {
                System.out.println("Đã hủy đặt máy.");
                return;
            }
        }

        System.out.println("\nĐẶT MÁY (PAY AS YOU GO):");
        System.out.printf("  Máy: %s | Đơn giá: %s/h%n",
                targetComputer.getName(), FormatUtils.formatVND(targetComputer.getPricePerHour()));
        System.out.println("  Số dư khả dụng: " + FormatUtils.formatVND(customerUser.getBalance()));

        String confirm = InputUtils.inputString("Xác nhận gửi yêu cầu mở máy? (Y/N): ");
        if (confirm.equalsIgnoreCase("y")) {
            Booking newBooking = new Booking(0, customerUser.getUserId(),
                    targetComputer.getComputerId(), start, end, "PENDING", BigDecimal.ZERO, targetComputer.getPricePerHour());
            this.currentBookingId = bookingService.bookComputer(customerUser.getUserId(), newBooking);
            PrintUtils.printSuccess("Yêu cầu mở máy " + targetComputer.getName() + " đã được gửi!");
            PrintUtils.printWarning("Vui lòng chờ Nhân viên (Staff) phê duyệt. Máy sẽ được bật sau khi được duyệt.");
        } else {
            System.out.println("Đã hủy đặt máy.");
        }
    }

    // -------------------------------------------------------
    // Chế độ 2: Đặt máy trước (Reservation + Cọc 1h)
    // -------------------------------------------------------

    private void reserveComputerFlow() throws BusinessException {
        System.out.println("\n--- ĐẶT MÁY TRƯỚC (CỌc 1 GIờ) ---");
        System.out.println("Lưu ý: Bạn sẽ bị trừ tiền cọc bằng 1 giờ chơi ngay lập tức.");
        System.out.println("Nếu quá 1 phút kể từ giờ đặt mà không mở máy, tiền cọc sẽ MẤT.\n");

        // Nhập thời gian muốn đến (tương lai) trước khi kiểm tra máy trống
        System.out.println("\nNhập thời gian bạn muốn đến (phải là tương lai):");
        String dateTimeStr = InputUtils.inputString("Nhập thời gian (yyyy-MM-dd HH:mm): ");

        java.sql.Timestamp startTime;
        try {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm");
            sdf.setLenient(false);
            java.util.Date parsed = sdf.parse(dateTimeStr);
            startTime = new java.sql.Timestamp(parsed.getTime());
        } catch (java.text.ParseException e) {
            PrintUtils.printError("Định dạng thời gian không hợp lệ. Hãy nhập theo mẫu yyyy-MM-dd HH:mm");
            return;
        }

        if (startTime.getTime() <= System.currentTimeMillis()) {
            PrintUtils.printError("Thời gian phải trong tương lai!");
            return;
        }

        // Chọn khu vực
        System.out.println("\nChọn khu vực:");
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

        // Tính thời gian kết thúc dự kiến = startTime + 1 giờ (3600000ms) để giữ chỗ
        java.sql.Timestamp endTime = new java.sql.Timestamp(startTime.getTime() + 3600000L);

        List<Computer> available = computerService.getAvailableComputersByZone(zone, startTime, endTime);
        if (available.isEmpty()) {
            PrintUtils.printWarning("Không có máy nào khả dụng trong khu vực này vào thời gian bạn chọn.");
            return;
        }

        System.out.println("\nDanh sách máy trống lúc " + dateTimeStr + ":");
        for (int i = 0; i < available.size(); i++) {
            Computer c = available.get(i);
            System.out.printf("%d. %s (%s) | %s/h%n",
                    i + 1, c.getName(), c.getZone(), FormatUtils.formatVND(c.getPricePerHour()));
        }
        int pcIdx = InputUtils.inputInt("Chọn số thứ tự máy (1-" + available.size() + "): ", 1, available.size());
        Computer targetComputer = available.get(pcIdx - 1);

        // Hiển thị thông tin xác nhận
        BigDecimal deposit = targetComputer.getPricePerHour();
        System.out.println("\n--- XÁC NHẬN ĐẶT TRƯỚC ---");
        System.out.printf("  Máy: %s (%s)%n", targetComputer.getName(), targetComputer.getZone());
        System.out.printf("  Thời gian đến: %s%n", dateTimeStr);
        System.out.printf("  Tiền cọc (1h): %s%n", FormatUtils.formatVND(deposit));
        System.out.printf("  Số dư hiện tại: %s%n", FormatUtils.formatVND(customerUser.getBalance()));
        PrintUtils.printWarning("Quá 1 phút kể từ giờ đặt, tiền cọc sẽ bị mất nếu không mở máy!");

        String confirm = InputUtils.inputString("Xác nhận đặt trước? (Y/N): ");
        if (confirm.equalsIgnoreCase("y")) {
            int bookingId = bookingService.reserveComputer(
                    customerUser.getUserId(), targetComputer.getComputerId(), startTime);

            // Cập nhật lại balance trong UI
            try {
                User updated = com.cyber.service.UserService.getInstance().getUserById(customerUser.getUserId());
                customerUser.setBalance(updated.getBalance());
            } catch (BusinessException ignored) {}

            PrintUtils.printSuccess("Đặt máy trước thành công! Booking #" + bookingId);
            System.out.println("  Tiền cọc đã trừ: " + FormatUtils.formatVND(deposit));
            System.out.println("  Số dư còn lại: " + FormatUtils.formatVND(customerUser.getBalance()));
            PrintUtils.printWarning("Hãy đến quán đúng giờ và yêu cầu Staff mở máy để được hoàn cọc!");
        } else {
            System.out.println("Đã hủy.");
        }
    }

    private void viewBookingHistoryFlow() throws BusinessException {
        System.out.println("\n--- LỊCH SỬ ĐẶT MÁY ---");
        List<Booking> list = bookingService.getBookingHistoryByUserId(customerUser.getUserId());
        if (list.isEmpty()) {
            System.out.println("Bạn chưa có lịch sử đặt máy nào.");
            return;
        }
        
        PrintUtils.printTableSeparator(115);
        System.out.printf("| %-5s | %-12s | %-20s | %-20s | %-15s | %-15s | %-10s |\n",
                "ID", "Máy", "Bắt đầu", "Kết thúc", "Tổng tiền", "Đơn giá/h", "Trạng thái");
        PrintUtils.printTableSeparator(115);
        
        for (Booking b : list) {
            String statusStr = b.getStatus();
            switch (statusStr) {
                case "ACTIVE": statusStr = "\033[32mACTIVE\033[0m"; break;
                case "COMPLETED": statusStr = "\033[36mCOMPLETED\033[0m"; break;
                case "CANCELLED": statusStr = "\033[31mCANCELLED\033[0m"; break;
                case "RESERVED": statusStr = "\033[33mRESERVED\033[0m"; break;
                case "PENDING": statusStr = "\033[35mPENDING\033[0m"; break;
            }
            
            System.out.printf("| %-5d | %-12s | %-20s | %-20s | %-15s | %-15s | %-19s |\n",
                    b.getBookingId(),
                    b.getComputerName() != null ? b.getComputerName() : String.valueOf(b.getComputerId()),
                    FormatUtils.formatTimestamp(b.getStartTime()),
                    FormatUtils.formatTimestamp(b.getEndTime()),
                    FormatUtils.formatVND(b.getTotalFee()),
                    FormatUtils.formatVND(b.getHourlyRateSnapshot()),
                    statusStr);
        }
        PrintUtils.printTableSeparator(115);
    }

    private void viewCurrentStatus() throws BusinessException {
        System.out.println("\n--- TRẠNG THÁI DỊCH VỤ F&B HIỆN TẠI ---");

        // Xem TẤT CẢ đơn hàng F&B (bao gồm DELIVERED, CANCELLED)
        List<FbOrder> allOrders = orderService.getAllOrdersByUserId(customerUser.getUserId());
        if (allOrders.isEmpty()) {
            System.out.println("\nĐồ ăn & Thức uống: Chưa có đơn hàng nào.");
        } else {
            System.out.println("\nTẤT CẢ đơn hàng F&B:");
            for (FbOrder o : allOrders) {
                String statusColor = switch (o.getStatus().name()) {
                    case "PENDING"   -> "\033[33mPENDING\033[0m";
                    case "PREPARING" -> "\033[34mPREPARING\033[0m";
                    case "DELIVERED" -> "\033[32mDELIVERED\033[0m";
                    case "CANCELLED" -> "\033[31mCANCELLED\033[0m";
                    default          -> o.getStatus().name();
                };
                System.out.printf(" - Đơn #%d | Máy: %s | Tiền: %s | Trạng thái: %s\n",
                        o.getOrderId(),
                        o.getComputerName(),
                        FormatUtils.formatVND(o.getTotalAmount()),
                        statusColor);
                        
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

            // Món lẻ flow (KHÔNG áp dụng strategy lúc này)
            FbAdvancedCartItem singleCartItem = buildSingleItem(selectedItem, allToppings, com.cyber.domain.fb.discount.NoDiscountStrategy.getInstance(), qty);
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
            BigDecimal finalPay = strategy.applyDiscount(cartTotal);
            orderService.orderFoodAdvanced(customerUser.getUserId(), currentBookingId, cart);
            customerUser.setBalance(customerUser.getBalance().subtract(finalPay));
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
        System.out.println("\n" + "=".repeat(130));
        System.out.println("  MENU F&B");
        System.out.println("=".repeat(130));
        System.out.printf("%-5s | %-10s | %-22s | %-25s | %-12s | %-8s | %-6s | %-15s%n",
                "ID", "Danh mục", "Tên món", "Mô tả", "Giá gốc", "Tồn kho", "T.gian", "Tags");
        System.out.println("-".repeat(130));
        for (FbMenuItem m : menuItems) {
            String stockStr = m.getStockQuantity() > 0 ? String.valueOf(m.getStockQuantity()) : "[HẾT HÀNG]";
            String nameCol = m.getStatus() == FBStatus.OUT_OF_STOCK 
                             ? PrintUtils.colorText(m.getName(), "YELLOW") 
                             : m.getName();
            String desc = m.getDescription() != null ? m.getDescription() : "(Không có)";
            if (desc.length() > 23) desc = desc.substring(0, 20) + "...";

            System.out.printf("%-5d | %-10s | %-22s | %-25s | %-12s | %-8s | %-6d' | %-15s%n",
                    m.getMenuItemId(),
                    m.getCategoryName() != null ? m.getCategoryName() : "-",
                    nameCol,
                    desc,
                    FormatUtils.formatVND(m.getBasePrice()),
                    stockStr,
                    m.getPrepTimeInMinutes(),
                    m.getItemTags() != null ? m.getItemTags() : "-");
        }
        System.out.println("=".repeat(130));
    }

    private void printCartSummary(List<FbAdvancedCartItem> cart, BigDecimal preDiscountTotal,
                                  IDiscountStrategy strategy) {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("  HOÁ ĐƠN DỰ KIẾN");
        System.out.println("=".repeat(80));
        
        for (FbAdvancedCartItem item : cart) {
            System.out.printf("  %-45s x%-3d = %s%n",
                    truncate(item.getItemDescription(), 45),
                    item.getQuantity(),
                    FormatUtils.formatVND(item.getFinalPrice()));
        }
        System.out.println("-".repeat(80));

        BigDecimal totalDiscountAmt = strategy.calculateDiscountAmount(preDiscountTotal);
        BigDecimal newTotal = strategy.applyDiscount(preDiscountTotal);

        System.out.printf("  %-48s %s%n", "Tiền trước giảm:", FormatUtils.formatVND(preDiscountTotal));
        if (totalDiscountAmt.compareTo(BigDecimal.ZERO) > 0) {
            System.out.printf("  %-48s %s%n", "Tổng giảm giá (" + strategy.getStrategyName() + "):", "-" + FormatUtils.formatVND(totalDiscountAmt));
        }
        System.out.printf("  %-48s %s%n", "TỔNG TIỀN HIỆN TẠI (SAU GIẢM):", FormatUtils.formatVND(newTotal));
        System.out.println("  Số dư hiện tại: " + FormatUtils.formatVND(customerUser.getBalance()));
        System.out.println("=".repeat(80));
        
        // Cập nhật lại list cart với chiết khấu để lưu DB khớp doanh thu.
        // Gán TOÀN BỘ giảm giá vào item đầu tiên (để chênh lệch về 0)
        if (!cart.isEmpty() && totalDiscountAmt.compareTo(BigDecimal.ZERO) > 0) {
            FbAdvancedCartItem first = cart.get(0);
            first.setDiscountApplied(totalDiscountAmt);
            first.setDiscountStrategyName(strategy.getStrategyName());
        }
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
