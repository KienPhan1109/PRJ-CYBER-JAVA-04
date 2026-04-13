package com.cyber.view;


import com.cyber.domain.fb.FbMenuItem;

import com.cyber.domain.fb.discount.FixedAmountDiscountStrategy;
import com.cyber.domain.fb.discount.IDiscountStrategy;
import com.cyber.domain.fb.discount.NoDiscountStrategy;
import com.cyber.domain.fb.discount.PercentageDiscountStrategy;
import com.cyber.exception.BusinessException;
import com.cyber.model.*;
import com.cyber.model.enums.FBStatus;
import com.cyber.service.BookingService;
import com.cyber.service.ComputerService;
import com.cyber.service.FbMenuService;
import com.cyber.service.FbOrderService;
import com.cyber.service.FbOrderService.FbCartItem;
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
                    // Lấy thêm thông tin Computer để hiện khu vực & cấu hình
                    Computer pendingComp = null;
                    try {
                        pendingComp = computerService.getComputerById(pending.getComputerId());
                    } catch (BusinessException ignored) {}

                    PrintUtils.printTableSeparator(70);
                    System.out.printf("| %-30s | %-33s |\n", "Booking ID", pending.getBookingId());
                    System.out.printf("| %-30s | %-33s |\n", "ID Máy", pending.getComputerId());
                    System.out.printf("| %-30s | %-33s |\n", "Tên máy", pending.getComputerName() != null ? pending.getComputerName() : "N/A");
                    System.out.printf("| %-30s | %-33s |\n", "Khu vực", pendingComp != null ? pendingComp.getZone() : "N/A");
                    System.out.printf("| %-30s | %-33s |\n", "Cấu hình", pendingComp != null ? truncate(pendingComp.getHardwareConfig(), 33) : "N/A");
                    System.out.printf("| %-30s | %-33s |\n", "Đơn giá/h", FormatUtils.formatVND(pending.getHourlyRateSnapshot()));
                    System.out.printf("| %-30s | %-33s |\n", "Trạng thái", PrintUtils.colorText("PENDING - Chờ duyệt", "YELLOW"));
                }
                PrintUtils.printTableSeparator(70);
            }

            // === PHẦN 2: Hiển thị các phiên chơi ACTIVE ===
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

                    // Tính số tiền đã chi (không bao gồm F&B)
                    BigDecimal hourlyRate = active.getHourlyRateSnapshot() != null ? active.getHourlyRateSnapshot() : BigDecimal.ZERO;
                    BigDecimal hoursUsed = BigDecimal.valueOf(secondsUsed).divide(BigDecimal.valueOf(3600), 6, java.math.RoundingMode.HALF_UP);
                    BigDecimal moneySpent = hourlyRate.multiply(hoursUsed).setScale(0, java.math.RoundingMode.HALF_UP);

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
                    System.out.printf("| %-30s | %-33s |\n", "Số tiền đã chi (máy)", FormatUtils.formatVND(moneySpent));
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

                // Chỉ hỏi Y/N để ngắt máy (vì 1 user chỉ mở được 1 máy)
                System.out.println("\n-------------------------------------------");
                String confirm = InputUtils.inputString("Bạn có muốn ngắt máy không? (Y/N): ", "^[YyNn]$", "Chỉ nhập Y hoặc N.");
                if (confirm.equalsIgnoreCase("y")) {
                    Booking activeBooking = allBookings.get(0); // Chỉ có 1 máy duy nhất
                    bookingService.endSession(activeBooking.getBookingId());
                    PrintUtils.printSuccess("Đã ngắt máy " + (activeBooking.getComputerName() != null ? activeBooking.getComputerName() : activeBooking.getComputerId()) + " thành công!");
                }
            }

        } catch (BusinessException e) {
            PrintUtils.printError(e.getMessage());
        }
    }

    // =========================================================
    // BOOKING FLOW — Hiển thị bảng toàn bộ máy, có phân trang, không cột status
    // =========================================================

    private void bookComputerFlow() throws BusinessException {
        System.out.println("\n--- ĐẶT MÁY TRẠM ---");
        
        // Ràng buộc 1 user - 1 session (bao gồm cả RESERVED)
        List<Booking> activeBookings = bookingService.getActiveBookingsByUserId(customerUser.getUserId());
        if (!activeBookings.isEmpty()) {
            PrintUtils.printWarning("Bạn đang sử dụng 1 máy trạm rồi. Vui lòng ngắt máy hiện tại trước khi thuê máy mới!");
            return;
        }
        
        // Kiểm tra có lịch đặt trước chưa
        List<Booking> allBookings = bookingService.getBookingHistoryByUserId(customerUser.getUserId());
        boolean hasReserved = allBookings.stream().anyMatch(b -> "RESERVED".equals(b.getStatus()));
        if (hasReserved) {
            PrintUtils.printWarning("Bạn đã có lịch đặt máy trước. Mỗi tài khoản chỉ được có 1 phiên hoạt động hoặc đặt trước duy nhất.");
            return;
        }
        
        // Kiểm tra yêu cầu PENDING
        List<Booking> pendingBookings = bookingService.getPendingBookings().stream()
                .filter(b -> b.getUserId() == customerUser.getUserId())
                .collect(java.util.stream.Collectors.toList());
        if (!pendingBookings.isEmpty()) {
            PrintUtils.printWarning("Bạn đã có yêu cầu mở máy đang chờ Staff duyệt. Vui lòng chờ hoặc liên hệ nhân viên!");
            return;
        }
        
        System.out.println("Lưu ý: Sau khi Staff duyệt, tiền máy sẽ được hệ thống trừ dần tự động mỗi 10 giây.");

        // Hiển thị bảng TẤT CẢ máy (không chọn zone, không cột trạng thái)
        Timestamp start = new Timestamp(System.currentTimeMillis());
        List<Computer> availableComputers = computerService.getAvailableComputersByZone(null, start, start);
        if (availableComputers.isEmpty()) {
            PrintUtils.printWarning("Rất tiếc! Hiện tại không có máy trống nào.");
            return;
        }

        // Phân trang bảng máy
        int pageSize = 10;
        int totalPages = (int) Math.ceil((double) availableComputers.size() / pageSize);
        int currentPage = 1;

        while (true) {
            int startIdx = (currentPage - 1) * pageSize;
            int endIdx = Math.min(startIdx + pageSize, availableComputers.size());

            System.out.println("\n" + "=".repeat(100));
            System.out.println("  DANH SÁCH MÁY TRỐNG (Trang " + currentPage + "/" + totalPages + ")");
            System.out.println("=".repeat(100));
            System.out.printf("%-5s | %-15s | %-12s | %-35s | %-15s%n", "ID", "Tên Máy", "Khu Vực", "Cấu hình", "Đơn giá/h");
            System.out.println("-".repeat(100));
            for (int i = startIdx; i < endIdx; i++) {
                Computer c = availableComputers.get(i);
                System.out.printf("%-5d | %-15s | %-12s | %-35s | %-15s%n",
                    c.getComputerId(), 
                    FormatUtils.truncate(c.getName(), 15),
                    FormatUtils.formatValue(c.getZone()), 
                    FormatUtils.truncate(c.getHardwareConfig(), 35),
                    FormatUtils.formatVND(c.getPricePerHour()));
            }
            System.out.println("=".repeat(100));
            System.out.println("Tổng: " + availableComputers.size() + " máy trống | Trang " + currentPage + "/" + totalPages);

            if (totalPages > 1) {
                System.out.println("[N] Trang sau | [P] Trang trước | [S] Chọn máy");
                String nav = InputUtils.inputString("Lựa chọn: ").toUpperCase();
                if (nav.equals("N") && currentPage < totalPages) { currentPage++; continue; }
                else if (nav.equals("P") && currentPage > 1) { currentPage--; continue; }
                else if (!nav.equals("S")) continue;
            }
            break; // Chỉ 1 trang hoặc user chọn "S"
        }

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
                    targetComputer.getComputerId(), start, null, "PENDING", BigDecimal.ZERO, targetComputer.getPricePerHour());
            this.currentBookingId = bookingService.bookComputer(customerUser.getUserId(), newBooking);
            PrintUtils.printSuccess("Yêu cầu mở máy " + targetComputer.getName() + " đã được gửi!");
            PrintUtils.printWarning("Vui lòng chờ Nhân viên (Staff) phê duyệt. Máy sẽ được bật sau khi được duyệt.");
        } else {
            System.out.println("Đã hủy đặt máy.");
        }
    }

    // -------------------------------------------------------
    // Chế độ 2: Đặt máy trước (Reservation + Cọc 1h)
    // Bỏ zone, bỏ STT, chọn theo ID, chặn nếu có PENDING
    // -------------------------------------------------------

    private void reserveComputerFlow() throws BusinessException {
        System.out.println("\n--- ĐẶT MÁY TRƯỚC (CỌc 1 GIờ) ---");

        // Ràng buộc 1 user - 1 session
        List<Booking> activeBookings = bookingService.getActiveBookingsByUserId(customerUser.getUserId());
        if (!activeBookings.isEmpty()) {
            PrintUtils.printWarning("Bạn đang có máy đang chơi. Mỗi tài khoản chỉ được có 1 phiên hoạt động hoặc đặt trước duy nhất.");
            return;
        }
        List<Booking> allBookings = bookingService.getBookingHistoryByUserId(customerUser.getUserId());
        boolean hasReserved = allBookings.stream().anyMatch(b -> "RESERVED".equals(b.getStatus()));
        if (hasReserved) {
            PrintUtils.printWarning("Bạn đã có lịch đặt máy trước rồi. Mỗi tài khoản chỉ được 1 phiên duy nhất.");
            return;
        }

        // Ràng buộc mới: Nếu có PENDING thì không được đặt trước
        List<Booking> pendingBookings = bookingService.getPendingBookings().stream()
                .filter(b -> b.getUserId() == customerUser.getUserId())
                .collect(java.util.stream.Collectors.toList());
        if (!pendingBookings.isEmpty()) {
            PrintUtils.printWarning("Bạn đang có yêu cầu mở máy trạm (PENDING) chờ duyệt. Không thể đặt máy trước khi yêu cầu chưa được xử lý!");
            return;
        }

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

        // Tính thời gian kết thúc dự kiến = startTime + 1 giờ (3600000ms) để giữ chỗ
        java.sql.Timestamp endTime = new java.sql.Timestamp(startTime.getTime() + 3600000L);

        // Hiển thị toàn bộ máy (không chọn zone), có phân trang
        List<Computer> available = computerService.getAvailableComputersByZone(null, startTime, endTime);
        if (available.isEmpty()) {
            PrintUtils.printWarning("Không có máy nào khả dụng vào thời gian bạn chọn.");
            return;
        }

        // Phân trang
        int pageSize = 10;
        int totalPages = (int) Math.ceil((double) available.size() / pageSize);
        int currentPage = 1;

        while (true) {
            int startIdx = (currentPage - 1) * pageSize;
            int endIdx = Math.min(startIdx + pageSize, available.size());

            System.out.println("\n" + "=".repeat(100));
            System.out.println("  DANH SÁCH MÁY TRỐNG LÚC " + dateTimeStr + " (Trang " + currentPage + "/" + totalPages + ")");
            System.out.println("=".repeat(100));
            System.out.printf("%-5s | %-15s | %-12s | %-35s | %-15s%n", "ID", "Tên Máy", "Khu Vực", "Cấu hình", "Đơn giá/h");
            System.out.println("-".repeat(100));
            for (int i = startIdx; i < endIdx; i++) {
                Computer c = available.get(i);
                System.out.printf("%-5d | %-15s | %-12s | %-35s | %-15s%n",
                        c.getComputerId(),
                        FormatUtils.truncate(c.getName(), 15),
                        FormatUtils.formatValue(c.getZone()),
                        FormatUtils.truncate(c.getHardwareConfig(), 35),
                        FormatUtils.formatVND(c.getPricePerHour()));
            }
            System.out.println("=".repeat(100));
            System.out.println("Tổng: " + available.size() + " máy | Trang " + currentPage + "/" + totalPages);

            if (totalPages > 1) {
                System.out.println("[N] Trang sau | [P] Trang trước | [S] Chọn máy");
                String nav = InputUtils.inputString("Lựa chọn: ").toUpperCase();
                if (nav.equals("N") && currentPage < totalPages) { currentPage++; continue; }
                else if (nav.equals("P") && currentPage > 1) { currentPage--; continue; }
                else if (!nav.equals("S")) continue;
            }
            break;
        }

        int computerId = InputUtils.inputInt("Nhập ID máy muốn đặt trước (0 để hủy): ", 0, Integer.MAX_VALUE);
        if (computerId == 0) { System.out.println("Đã hủy."); return; }

        Computer targetComputer = available.stream()
                .filter(c -> c.getComputerId() == computerId).findFirst().orElse(null);
        if (targetComputer == null) { PrintUtils.printError("ID máy không hợp lệ."); return; }

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
        
        int pageSize = 10;
        int totalPages = (int) Math.ceil((double) list.size() / pageSize);
        int currentPage = 1;

        while (true) {
            int start = (currentPage - 1) * pageSize;
            int end = Math.min(start + pageSize, list.size());

            System.out.println("\n" + "=".repeat(120));
            System.out.println("  LỊCH SỬ ĐẶT MÁY (Trang " + currentPage + "/" + totalPages + ")");
            System.out.println("=".repeat(120));
            System.out.printf("%-5s | %-12s | %-20s | %-20s | %-15s | %-15s | %-15s%n",
                    "ID", "Máy", "Bắt đầu", "Kết thúc", "Tổng tiền", "Đơn giá/h", "Trạng thái");
            System.out.println("-".repeat(120));

            for (int i = start; i < end; i++) {
                Booking b = list.get(i);
                System.out.printf("%-5d | %-12s | %-20s | %-20s | %-15s | %-15s | %-24s%n",
                        b.getBookingId(),
                        FormatUtils.formatValue(b.getComputerName() != null ? b.getComputerName() : String.valueOf(b.getComputerId())),
                        FormatUtils.formatTimestamp(b.getStartTime()),
                        FormatUtils.formatTimestamp(b.getEndTime()),
                        FormatUtils.formatVND(b.getTotalFee()),
                        FormatUtils.formatVND(b.getHourlyRateSnapshot()),
                        FormatUtils.formatBookingStatus(b.getStatus()));
            }
            System.out.println("=".repeat(120));

            if (totalPages <= 1) break;
            System.out.println("[N] Trang sau | [P] Trang trước | [Q] Thoát");
            String nav = InputUtils.inputString("Lựa chọn: ").toUpperCase();
            if (nav.equals("N") && currentPage < totalPages) currentPage++;
            else if (nav.equals("P") && currentPage > 1) currentPage--;
            else if (nav.equals("Q")) break;
        }
    }

    private void viewCurrentStatus() throws BusinessException {
        System.out.println("\n--- TRẠNG THÁI DỊCH VỤ F&B HIỆN TẠI ---");

        List<FbOrder> allOrders = orderService.getAllOrdersByUserId(customerUser.getUserId());
        if (allOrders.isEmpty()) {
            System.out.println("\nĐồ ăn & Thức uống: Chưa có đơn hàng nào.");
        } else {
            System.out.println("\n" + "=".repeat(90));
            System.out.println("  TẤT CẢ ĐƠN HÀNG F&B");
            System.out.println("=".repeat(90));
            System.out.printf("%-8s | %-15s | %-15s | %-15s | %-20s%n",
                    "Đơn #", "Máy", "Tổng tiền", "Trạng thái", "Chi tiết món");
            System.out.println("-".repeat(90));

            for (FbOrder o : allOrders) {
                String statusVn = switch (o.getStatus().name()) {
                    case "PENDING"   -> com.cyber.util.ColorConst.YELLOW + "Chờ xử lý"    + com.cyber.util.ColorConst.RESET;
                    case "PREPARING" -> com.cyber.util.ColorConst.BLUE   + "Đang pha chế"  + com.cyber.util.ColorConst.RESET;
                    case "DELIVERED" -> com.cyber.util.ColorConst.GREEN  + "Đã giao"       + com.cyber.util.ColorConst.RESET;
                    case "CANCELLED" -> com.cyber.util.ColorConst.RED    + "Đã hủy"        + com.cyber.util.ColorConst.RESET;
                    default          -> o.getStatus().name();
                };

                List<Map<String, Object>> details = orderService.getOrderDetails(o.getOrderId());
                StringBuilder itemsStr = new StringBuilder();
                for (Map<String, Object> d : details) {
                    int qty = ((Number) d.get("quantity")).intValue();
                    String desc = (String) d.get("item_description");
                    if (!itemsStr.isEmpty()) itemsStr.append(", ");
                    itemsStr.append(desc).append(" x").append(qty);
                }

                System.out.printf("%-8d | %-15s | %-15s | %-24s | %-20s%n",
                        o.getOrderId(),
                        FormatUtils.formatValue(o.getComputerName()),
                        FormatUtils.formatVND(o.getTotalAmount()),
                        statusVn,
                        FormatUtils.truncate(itemsStr.toString()));
            }
            System.out.println("=".repeat(90));
        }
    }

    // =========================================================
    // ORDER FOOD FLOW — F&B ADVANCED (Phase 2)
    // Áp dụng: Composite + Decorator + Strategy Patterns
    // Phân trang menu, hiện lại bảng sau mỗi lần chọn
    // =========================================================

    private void orderFoodFlow() throws BusinessException {
        // --- Kiểm tra xem khách có đang ngồi máy (phiên ACTIVE) không ---
        List<Booking> activeBookings = bookingService.getActiveBookingsByUserId(customerUser.getUserId());
        if (activeBookings.isEmpty()) {
            PrintUtils.printWarning("Bạn cần phải mở máy (có phiên chơi ACTIVE) thì mới có thể gọi dịch vụ F&B.");
            return;
        }
        this.currentBookingId = activeBookings.get(0).getBookingId();

        // ---- Chọn Strategy giảm giá ----
        IDiscountStrategy strategy = selectDiscountStrategy();

        // ---- Tải menu ----
        List<FbMenuItem> menuItems = menuService.getAllActiveMenuItems();
        if (menuItems.isEmpty()) {
            PrintUtils.printWarning("Hiện không có món nào trong menu.");
            return;
        }

        // ---- Giỏ hàng ----
        List<FbCartItem> cart = new ArrayList<>();
        BigDecimal cartTotal = BigDecimal.ZERO;

        while (true) {
            // Hiển thị menu (phân trang) và giỏ hàng hiện tại sau mỗi lần chọn
            printMenuTablePaginated(menuItems);

            // Hiện giỏ hàng nếu có
            if (!cart.isEmpty()) {
                System.out.println("\n--- GIỎ HÀNG HIỆN TẠI ---");
                for (FbCartItem item : cart) {
                    System.out.printf("  %-35s x%-3d = %s%n",
                            truncate(item.getItemDescription(), 35),
                            item.getQuantity(),
                            FormatUtils.formatVND(item.getFinalPrice()));
                }
                System.out.println("  Tạm tính: " + FormatUtils.formatVND(cartTotal));
                System.out.println("--------------------------");
            }

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
            
            BigDecimal finalPrice = selectedItem.getBasePrice().multiply(BigDecimal.valueOf(qty));

            FbCartItem singleCartItem = new FbCartItem(
                    selectedItem.getMenuItemId(),
                    qty,
                    finalPrice,
                    selectedItem.getName(),
                    "{}",
                    BigDecimal.ZERO,
                    ""
            );
            
            cart.add(singleCartItem);
            cartTotal = cartTotal.add(finalPrice);
            PrintUtils.printSuccess("Đã thêm [%s] x%d vào giỏ. Đơn giá: %s",
                    singleCartItem.getItemDescription(), qty,
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
            orderService.orderFood(customerUser.getUserId(), currentBookingId, cart);
            customerUser.setBalance(customerUser.getBalance().subtract(finalPay));
            PrintUtils.printSuccess("Đặt đồ ăn thành công! Đơn hàng đang chờ xử lý.");
        } else {
            System.out.println("Đã hủy đơn F&B.");
        }
    }

    // =========================================================
    // Helpers
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
     * In bảng menu có phân trang.
     */
    private void printMenuTablePaginated(List<FbMenuItem> menuItems) {
        int pageSize = 10;
        int totalPages = (int) Math.ceil((double) menuItems.size() / pageSize);
        int currentPage = 1;

        while (true) {
            int startIdx = (currentPage - 1) * pageSize;
            int endIdx = Math.min(startIdx + pageSize, menuItems.size());

            System.out.println("\n" + "=".repeat(130));
            System.out.println("  MENU F&B (Trang " + currentPage + "/" + totalPages + ")");
            System.out.println("=".repeat(130));
            System.out.printf("%-5s | %-10s | %-22s | %-25s | %-12s | %-8s | %-6s | %-15s%n",
                    "ID", "Danh mục", "Tên món", "Mô tả", "Giá gốc", "Tồn kho", "T.gian", "Tags");
            System.out.println("-".repeat(130));
            for (int i = startIdx; i < endIdx; i++) {
                FbMenuItem m = menuItems.get(i);
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
            System.out.println("Tổng: " + menuItems.size() + " món | Trang " + currentPage + "/" + totalPages);

            if (totalPages <= 1) break;
            System.out.println("[N] Trang sau | [P] Trang trước | [Q] Chọn món");
            String nav = InputUtils.inputString("Lựa chọn: ").toUpperCase();
            if (nav.equals("N") && currentPage < totalPages) { currentPage++; continue; }
            else if (nav.equals("P") && currentPage > 1) { currentPage--; continue; }
            break; // "Q" or any other = exit to select
        }
    }

    private void printCartSummary(List<FbCartItem> cart, BigDecimal preDiscountTotal,
                                  IDiscountStrategy strategy) {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("  HOÁ ĐƠN DỰ KIẾN");
        System.out.println("=".repeat(80));
        
        for (FbCartItem item : cart) {
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
        BigDecimal balanceAfter = customerUser.getBalance().subtract(newTotal);
        System.out.println("  Số dư sau khi thanh toán: " + FormatUtils.formatVND(balanceAfter));
        System.out.println("=".repeat(80));
        
        // Cập nhật lại list cart với chiết khấu để lưu DB khớp doanh thu.
        // Gán TOÀN BỘ giảm giá vào item đầu tiên (để chênh lệch về 0)
        if (!cart.isEmpty() && totalDiscountAmt.compareTo(BigDecimal.ZERO) > 0) {
            FbCartItem first = cart.get(0);
            first.setDiscountApplied(totalDiscountAmt);
            first.setDiscountStrategyName(strategy.getStrategyName());
        }
    }



    private String truncate(String s, int maxLen) {
        if (s == null) return "";
        return s.length() <= maxLen ? s : s.substring(0, maxLen - 3) + "...";
    }
}
