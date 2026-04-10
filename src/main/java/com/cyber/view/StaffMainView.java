package com.cyber.view;

import com.cyber.exception.BusinessException;
import com.cyber.model.Booking;
import com.cyber.model.FbOrder;
import com.cyber.model.SystemLog;
import com.cyber.model.User;
import com.cyber.model.enums.FbOrderStatus;
import com.cyber.model.enums.LogType;
import com.cyber.service.BookingService;
import com.cyber.service.FbOrderService;
import com.cyber.service.LogService;
import com.cyber.service.UserService;
import com.cyber.util.FormatUtils;
import com.cyber.util.InputUtils;
import com.cyber.util.PrintUtils;

import java.math.BigDecimal;
import java.util.List;

public class StaffMainView {

    private final User staffUser;
    private final UserService    userService;
    private final FbOrderService fbOrderService;
    private final BookingService bookingService;
    private final LogService     logService;

    public StaffMainView(User staffUser) {
        this.staffUser       = staffUser;
        this.userService     = UserService.getInstance();
        this.fbOrderService  = FbOrderService.getInstance();
        this.bookingService  = BookingService.getInstance();
        this.logService      = LogService.getInstance();
    }

    public void displayMenu() {
        while (true) {
            System.out.println("\n==========================================");
            System.out.println("          NHÂN VIÊN (STAFF) PANEL         ");
            System.out.println("          Xin chào: " + staffUser.getFullName());
            System.out.println("==========================================");
            System.out.println("1. Nạp tiền cho Khách Hàng");
            System.out.println("2. Quản lý Đơn hàng F&B");
            System.out.println("3. Quản lý Yêu cầu mở máy (Booking)");
            System.out.println("4. Xem Lịch sử Log (Audit)");
            System.out.println("0. Đăng xuất");
            System.out.println("==========================================");

            int choice = InputUtils.inputInt("Vui lòng chọn chức năng (0-4): ", 0, 4);

            try {
                switch (choice) {
                    case 1: topUpUser();              break;
                    case 2: manageFbOrders();         break;
                    case 3: manageBookingRequests();  break;
                    case 4: viewLogsMenu();           break;
                    case 0:
                        PrintUtils.printWarning("Đang đăng xuất khỏi hệ thống Staff...");
                        return;
                }
            } catch (BusinessException e) {
                PrintUtils.printError(e.getMessage());
            } catch (Exception e) {
                PrintUtils.printError("Lỗi hệ thống: " + e.getMessage());
            }
        }
    }

    private void topUpUser() throws BusinessException {
        System.out.println("\n--- NẠP TIỀN KHÁCH HÀNG ---");
        String keyword = InputUtils.inputStringOptional("Nhập từ khóa tên/username (Enter = hiện tất cả): ");
        List<User> list = userService.searchUsersByName(keyword);
        if (list.isEmpty()) {
            PrintUtils.printWarning("Không tìm thấy khách hàng nào khớp với từ khóa.");
            return;
        }
        System.out.println("Kết quả tìm kiếm:");
        for (User u : list) {
            System.out.printf(" - ID: %d | Username: %s | Tên: %s | SĐT: %s | Số dư: %s\n",
                    u.getUserId(), u.getUsername(), u.getFullName(), u.getPhone() != null ? u.getPhone() : "N/A", FormatUtils.formatVND(u.getBalance()));
        }

        int id = InputUtils.inputInt("Nhập chính xác ID người dùng cần nạp (0 để hủy): ", 0, Integer.MAX_VALUE);
        if (id == 0) return;
        
        BigDecimal amount = InputUtils.inputBigDecimal("Nhập số tiền muốn nạp (VND): ", BigDecimal.ONE);

        // Truyền staffUser (actor) để ghi vào system_logs
        userService.topUpUser(id, amount, staffUser);
        PrintUtils.printSuccess("Đã nạp " + FormatUtils.formatVND(amount) + " thành công cho User ID: " + id);
    }

    private void manageFbOrders() throws BusinessException {
        while (true) {
            List<FbOrder> pendingOrders = fbOrderService.getPendingOrders();
            System.out.println("\n--- DANH SÁCH ĐƠN HÀNG PENDING & PREPARING ---");
            if (pendingOrders.isEmpty()) {
                System.out.println("Chưa có đơn hàng nào cần xử lý.");
                return;
            }

            System.out.printf("%-10s | %-20s | %-15s | %-15s | %-24s%n",
                    "Order ID", "Tên Khách Hàng", "Tên Máy", "Tổng Tiền", "Trạng thái");
            System.out.println("-".repeat(92));
            for (FbOrder order : pendingOrders) {
                String stStr = order.getStatus() != null ? order.getStatus().name() : "N/A";
                String coloredSt = switch (stStr) {
                    case "PENDING"   -> PrintUtils.colorText(stStr, "YELLOW");
                    case "PREPARING" -> PrintUtils.colorText(stStr, "CYAN");
                    case "DELIVERED" -> PrintUtils.colorText(stStr, "GREEN");
                    case "CANCELLED" -> PrintUtils.colorText(stStr, "RED");
                    default -> stStr;
                };
                System.out.printf("%-10d | %-20s | %-15s | %-15s | %-33s%n",
                        order.getOrderId(),
                        order.getUserName() != null ? truncate(order.getUserName(), 20) : "N/A",
                        order.getComputerName() != null ? truncate(order.getComputerName(), 15) : "Không có",
                        FormatUtils.formatVND(order.getTotalAmount()),
                        coloredSt);
            }
            System.out.println("-".repeat(92));
            System.out.println("Nhập Order ID để cập nhật trạng thái (0 để Quay Lại):");
            int orderId = InputUtils.inputInt("Order ID: ", 0, Integer.MAX_VALUE);
            if (orderId == 0) return;

            FbOrder target = pendingOrders.stream()
                    .filter(o -> o.getOrderId() == orderId).findFirst().orElse(null);
            if (target == null) {
                PrintUtils.printError("Order ID không hợp lệ trong danh sách.");
                continue;
            }

            System.out.println("Cập nhật trạng thái cho Order #" + orderId + ":");
            System.out.println("1. Xác nhận đang làm (PREPARING)");
            System.out.println("2. Đã giao xong   (DELIVERED)");
            System.out.println("3. Hủy bỏ         (CANCELLED)");
            int action = InputUtils.inputInt("Chọn thao tác (1-3): ", 1, 3);

            FbOrderStatus newStatus = action == 1 ? FbOrderStatus.PREPARING
                                    : (action == 2 ? FbOrderStatus.DELIVERED : FbOrderStatus.CANCELLED);

            // Truyền staffUser (actor) để ghi vào system_logs
            fbOrderService.updateOrderStatus(orderId, newStatus, staffUser);
            PrintUtils.printSuccess("Đã cập nhật Order #" + orderId + " -> " + newStatus);
        }
    }

    // -------------------------------------------------------
    // Quản lý Yêu cầu mở máy (Booking Approval)
    // -------------------------------------------------------

    private void manageBookingRequests() throws BusinessException {
        while (true) {
            java.util.List<Booking> pendingList = bookingService.getPendingBookings();
            System.out.println("\n--- DANH SÁCH YÊU CẦU MỞ MÁY (PENDING / RESERVED) ---");
            if (pendingList.isEmpty()) {
                System.out.println("Không có yêu cầu nào đang chờ duyệt.");
                return;
            }

            System.out.printf("%-8s | %-10s | %-18s | %-12s | %-12s | %-12s | %-16s%n",
                    "ID", "Trạng thái", "Tên Khách Hàng", "Tên Máy", "Đơn giá/h", "Tiền cọc", "Giờ đặt");
            System.out.println("-".repeat(100));
            for (Booking b : pendingList) {
                String statusLabel = "RESERVED".equals(b.getStatus()) ? "ĐẶT TRƯỚC" : "MỞ MÁY";
                String depositStr = "RESERVED".equals(b.getStatus()) && b.getTotalFee() != null
                        ? FormatUtils.formatVND(b.getTotalFee()) : "---";
                System.out.printf("%-8d | %-10s | %-18s | %-12s | %-12s | %-12s | %-16s%n",
                        b.getBookingId(),
                        statusLabel,
                        b.getUserName() != null ? truncate(b.getUserName(), 18) : "N/A",
                        b.getComputerName() != null ? truncate(b.getComputerName(), 12) : "N/A",
                        b.getHourlyRateSnapshot() != null ? FormatUtils.formatVND(b.getHourlyRateSnapshot()) : "N/A",
                        depositStr,
                        b.getStartTime() != null ? b.getStartTime().toString().substring(0, 16) : "N/A");
            }
            System.out.println("-".repeat(100));

            int bookingId = InputUtils.inputInt("Nhập Booking ID để xử lý (0 để Quay Lại): ", 0, Integer.MAX_VALUE);
            if (bookingId == 0) return;

            Booking target = pendingList.stream()
                    .filter(b -> b.getBookingId() == bookingId).findFirst().orElse(null);
            if (target == null) {
                PrintUtils.printError("Booking ID không hợp lệ trong danh sách.");
                continue;
            }

            String typeLabel = "RESERVED".equals(target.getStatus()) ? " (ĐẶT TRƯỚC — có cọc)" : "";
            System.out.println("Xử lý yêu cầu Booking #" + bookingId + typeLabel
                    + " — Khách: " + target.getUserName() + " — Máy: " + target.getComputerName());
            System.out.println("1. Phê duyệt (Approve) — Bật máy cho khách" 
                    + ("RESERVED".equals(target.getStatus()) ? " + Hoàn cọc" : ""));
            System.out.println("2. Từ chối   (Reject)  — Hủy yêu cầu"
                    + ("RESERVED".equals(target.getStatus()) ? " + Hoàn cọc" : ""));
            int action = InputUtils.inputInt("Chọn thao tác (1-2): ", 1, 2);

            if (action == 1) {
                bookingService.approveBooking(bookingId, staffUser);
                PrintUtils.printSuccess("Đã PHÊ DUYỆT Booking #" + bookingId + ". Máy " + target.getComputerName() + " đã được bật cho khách."
                        + ("RESERVED".equals(target.getStatus()) ? " Tiền cọc đã hoàn lại." : ""));
            } else {
                bookingService.rejectBooking(bookingId, staffUser);
                PrintUtils.printWarning("Đã TỪ CHỐI Booking #" + bookingId + ". Yêu cầu đã bị hủy."
                        + ("RESERVED".equals(target.getStatus()) ? " Tiền cọc đã hoàn lại." : ""));
            }
        }
    }

    // -------------------------------------------------------
    // Xem Audit Log (Staff - phân quyền)
    // -------------------------------------------------------

    private void viewLogsMenu() throws BusinessException {
        System.out.println("\n--- XEM LỊCH SỬ LOG ---");
        System.out.println("1. USER log     (chỉ log do bạn thực hiện)");
        System.out.println("2. FB log       (chỉ log do bạn thực hiện)");
        System.out.println("3. COMPUTER log (log máy trạm & booking)");
        System.out.println("0. Quay lại");

        int choice = InputUtils.inputInt("Chọn (0-3): ", 0, 3);
        if (choice == 0) return;

        LogType logType = switch (choice) {
            case 1 -> LogType.USER;
            case 3 -> LogType.COMPUTER;
            default -> LogType.FB;
        };

        List<SystemLog> logs = logService.getLogsWithPermission(logType, staffUser);
        printLogTable(logs, logType.name());
    }

    private void printLogTable(List<SystemLog> logs, String title) {
        System.out.println("\n" + "=".repeat(110));
        System.out.println("  AUDIT LOG — " + title);
        System.out.println("=".repeat(110));
        System.out.printf("%-6s | %-8s | %-10s | %-60s | %-20s%n",
                "ID", "Type", "Actor ID", "Hành động", "Thời gian");
        System.out.println("-".repeat(110));
        if (logs.isEmpty()) {
            System.out.println("  Không có bản ghi nào.");
        } else {
            for (SystemLog log : logs) {
                System.out.printf("%-6d | %-8s | %-10d | %-60s | %-20s%n",
                        log.getId(),
                        log.getLogType().name(),
                        log.getActorId(),
                        truncate(log.getAction(), 60),
                        log.getCreatedAt() != null ? log.getCreatedAt().toString().substring(0, 19) : "N/A");
            }
        }
        System.out.println("=".repeat(110));
    }

    private String truncate(String s, int maxLen) {
        if (s == null) return "";
        return s.length() <= maxLen ? s : s.substring(0, maxLen - 3) + "...";
    }
}
