package com.cyber.view;

import com.cyber.model.SystemLog;
import com.cyber.model.User;
import com.cyber.model.enums.LogType;
import com.cyber.service.LogService;
import com.cyber.util.InputUtils;
import com.cyber.util.PrintUtils;

import java.util.List;

public class AdminMainView {

    private final User adminUser;
    private final ComputerManagementView  computerView;
    private final FbMenuManagementView    fbMenuView;
    private final UserManagementView      userView;
    private final LogService              logService;

    public AdminMainView(User adminUser) {
        this.adminUser = adminUser;
        this.computerView = new ComputerManagementView(adminUser);
        this.fbMenuView = new FbMenuManagementView(adminUser);
        this.userView = new UserManagementView(adminUser);
        this.logService = LogService.getInstance();
    }

    public void displayMenu() {
        while (true) {
            System.out.println("\n==========================================");
            System.out.println("      QUẢN TRỊ VIÊN (ADMIN) PANEL       ");
            System.out.println("      Xin chào: " + adminUser.getFullName());
            System.out.println("==========================================");
            System.out.println("1. Quản lý Máy trạm");
            System.out.println("2. Quản lý Menu F&B");
            System.out.println("3. Quản lý hệ thống Người dùng");
            System.out.println("4. Xem Lịch sử Log (Audit)");
            System.out.println("0. Đăng xuất");
            System.out.println("==========================================");

            int choice = InputUtils.inputInt("Vui lòng chọn chức năng (0-4): ", 0, 4);

            switch (choice) {
                case 1 -> computerView.displayMenu();
                case 2 -> fbMenuView.displayMenu();
                case 3 -> userView.displayMenu();
                case 4 -> viewLogsMenu();
                case 0 -> {
                    PrintUtils.printWarning("Đang đăng xuất khỏi hệ thống Admin...");
                    return;
                }
            }
        }
    }

    // -------------------------------------------------------
    // Xem Audit Log (Admin — xem toàn bộ)
    // -------------------------------------------------------
    private void viewLogsMenu() {
        try {
            System.out.println("\n--- XEM LỊCH SỬ LOG (ADMIN) ---");
            System.out.println("1. USER log   (toàn bộ hành động user)");
            System.out.println("2. FB log     (toàn bộ đơn F&B)");
            System.out.println("3. COMPUTER log");
            System.out.println("0. Quay lại");

            int choice = InputUtils.inputInt("Chọn (0-3): ", 0, 3);
            if (choice == 0) return;

            LogType logType = switch (choice) {
                case 1 -> LogType.USER;
                case 2 -> LogType.FB;
                default -> LogType.COMPUTER;
            };

            // Admin xem toàn bộ
            List<SystemLog> logs = logService.getLogsWithPermission(logType, adminUser);
            printLogTable(logs, logType.name());
        } catch (Exception e) {
            PrintUtils.printError("Lỗi xem log: " + e.getMessage());
        }
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
