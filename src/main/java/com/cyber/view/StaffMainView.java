package com.cyber.view;

import com.cyber.exception.BusinessException;
import com.cyber.model.FbOrder;
import com.cyber.model.SystemLog;
import com.cyber.model.User;
import com.cyber.model.enums.FbOrderStatus;
import com.cyber.model.enums.LogType;
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
    private final LogService     logService;

    public StaffMainView(User staffUser) {
        this.staffUser      = staffUser;
        this.userService    = UserService.getInstance();
        this.fbOrderService = FbOrderService.getInstance();
        this.logService     = LogService.getInstance();
    }

    public void displayMenu() {
        while (true) {
            System.out.println("\n==========================================");
            System.out.println("          NHÂN VIÊN (STAFF) PANEL         ");
            System.out.println("          Xin chào: " + staffUser.getFullName());
            System.out.println("==========================================");
            System.out.println("1. Nạp tiền cho Khách Hàng");
            System.out.println("2. Quản lý Đơn hàng F&B");
            System.out.println("3. Xem Lịch sử Log (Audit)");
            System.out.println("0. Đăng xuất");
            System.out.println("==========================================");

            int choice = InputUtils.inputInt("Vui lòng chọn chức năng (0-3): ", 0, 3);

            try {
                switch (choice) {
                    case 1: topUpUser();      break;
                    case 2: manageFbOrders(); break;
                    case 3: viewLogsMenu();   break;
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
        int id = InputUtils.inputInt("Nhập ID người dùng cần nạp: ", 1, Integer.MAX_VALUE);
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
    // Xem Audit Log (Staff - phân quyền)
    // -------------------------------------------------------

    private void viewLogsMenu() throws BusinessException {
        System.out.println("\n--- XEM LỊCH SỬ LOG ---");
        System.out.println("1. USER log  (chỉ log do bạn thực hiện)");
        System.out.println("2. FB log    (toàn bộ đơn hàng)");
        System.out.println("3. COMPUTER log (toàn bộ máy trạm)");
        System.out.println("0. Quay lại");

        int choice = InputUtils.inputInt("Chọn (0-3): ", 0, 3);
        if (choice == 0) return;

        LogType logType = switch (choice) {
            case 1 -> LogType.USER;
            case 2 -> LogType.FB;
            default -> LogType.COMPUTER;
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
