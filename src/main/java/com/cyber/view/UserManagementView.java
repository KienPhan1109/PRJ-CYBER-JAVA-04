package com.cyber.view;

import com.cyber.exception.BusinessException;
import com.cyber.model.Role;
import com.cyber.model.SystemLog;
import com.cyber.model.User;
import com.cyber.model.enums.LogType;
import com.cyber.model.enums.UserStatus;
import com.cyber.service.AuthService;
import com.cyber.service.LogService;
import com.cyber.service.UserService;
import com.cyber.util.FormatUtils;
import com.cyber.util.InputUtils;
import com.cyber.util.PrintUtils;

import java.math.BigDecimal;
import java.util.List;

public class UserManagementView {

    private final User        adminUser;    // Actor cho audit log
    private final UserService userService;
    private final AuthService authService;
    private final LogService  logService;

    public UserManagementView(User adminUser) {
        this.adminUser   = adminUser;
        this.userService = UserService.getInstance();
        this.authService = AuthService.getInstance();
        this.logService  = LogService.getInstance();
    }

    public void displayMenu() {
        while (true) {
            System.out.println("--- QUẢN LÝ NGƯỜI DÙNG ---");
            System.out.println("1. Danh sách người dùng");
            System.out.println("2. Thêm người dùng mới");
            System.out.println("3. Sửa thông tin người dùng");
            System.out.println("4. Khóa / Mở khóa tài khoản");
            System.out.println("5. Nạp tiền cho tài khoản");
            System.out.println("6. Xem Lịch sử Log (Audit)");
            System.out.println("0. Quay Lại");

            int choice = InputUtils.inputInt("Chọn (0-6): ", 0, 6);
            try {
                switch (choice) {
                    case 1: showAllUsers();    break;
                    case 2: addUser();          break;
                    case 3: editUser();         break;
                    case 4: toggleUserStatus(); break;
                    case 5: topUpBalance();     break;
                    case 6: viewLogsMenu();     break;
                    case 0: return;
                }
            } catch (BusinessException e) {
                PrintUtils.printError(e.getMessage());
            } catch (Exception e) {
                PrintUtils.printError("Lỗi hệ thống: " + e.getMessage());
            }
        }
    }

    private void showAllUsers() throws BusinessException {
        List<User> users = userService.getAllUsers();
        if (users.isEmpty()) {
            PrintUtils.printWarning("Không có người dùng nào.");
            return;
        }
        System.out.println("\n--- DANH SÁCH NGƯỜI DÙNG ---");
        System.out.printf("%-5s | %-15s | %-20s | %-12s | %-15s | %-15s | %-10s\n", 
            "ID", "Tài khoản", "Họ tên", "SĐT", "Số dư", "Quyền", "Trạng thái");
        System.out.println("-----------------------------------------------------------------------------------------------------");
        for (User u : users) {
             String statusStr = u.getStatus() == UserStatus.ACTIVE ? "\033[32mACTIVE\033[0m" : "\033[31mLOCKED\033[0m";
             System.out.printf("%-5d | %-15s | %-20s | %-12s | %-15s | %-15s | %-10s\n",
                 u.getUserId(), u.getUsername(), u.getFullName(), u.getPhone() != null ? u.getPhone() : "N/A", 
                 FormatUtils.formatVND(u.getBalance()), u.getRole() != null ? u.getRole().getRoleName() : "N/A",
                 statusStr);
        }
        System.out.println("-----------------------------------------------------------------------------------------------------");
    }

    private void addUser() throws BusinessException {
        System.out.println("\n[THÊM NGƯỜI DÙNG MỚI]");
        User user = new User();
        String pw = user.inputRegisterData();
        
        System.out.println("Chọn Role (1. ADMIN | 2. STAFF | 3. CUSTOMER): ");
        int roleChoice = InputUtils.inputInt("Nhập (1-3) [Mặc định 3]: ", 1, 3);
        String roleName = roleChoice == 1 ? "ADMIN" : (roleChoice == 2 ? "STAFF" : "CUSTOMER");
        user.setRole(new Role(roleChoice, roleName));
        
        authService.register(user, pw);
        PrintUtils.printSuccess("Thêm người dùng thành công! ID = " + user.getUserId());
    }

    private void editUser() throws BusinessException {
        System.out.println("\n[SỬA THÔNG TIN NGƯỜI DÙNG]");
        int id = InputUtils.inputInt("Nhập ID người dùng cần sửa: ", 1, Integer.MAX_VALUE);
        
        List<User> list = userService.getAllUsers();
        User existing = list.stream().filter(u -> u.getUserId() == id).findFirst().orElse(null);
        if (existing == null) {
            throw new BusinessException("NOT_FOUND", "Không tìm thấy User với ID " + id);
        }

        System.out.println("Sửa thông tin cho Username: " + existing.getUsername());
        
        String newName = InputUtils.inputStringUpdate("Nhập họ tên mới (Cũ: " + existing.getFullName() + ") [Enter để giữ nguyên]: ", existing.getFullName());
        existing.setFullName(newName);
        
        String newPhone = InputUtils.inputStringUpdate("Nhập SDT mới (Cũ: " + existing.getPhone() + ") [Enter để giữ nguyên]: ", existing.getPhone());
        existing.setPhone(newPhone);
        
        int oldRoleId = existing.getRole() != null ? existing.getRole().getRoleId() : 3;
        System.out.println("Chọn Role (1. ADMIN | 2. STAFF | 3. CUSTOMER - Cũ: " + oldRoleId + "): ");
        String roleInput = InputUtils.inputStringUpdate("Nhập Role mới [Enter để giữ nguyên]: ", String.valueOf(oldRoleId)).trim();
        if(!roleInput.isEmpty()) {
            try {
                int rId = Integer.parseInt(roleInput);
                existing.setRole(new Role(rId, rId == 1 ? "ADMIN" : (rId == 2 ? "STAFF" : "CUSTOMER")));
            } catch (Exception e) {}
        }

        userService.updateUser(existing);
        PrintUtils.printSuccess("Cập nhật thông tin thành công!");
    }

    private void toggleUserStatus() throws BusinessException {
        System.out.println("\n[KHÓA / MỞ KHÓA TÀI KHOẢN]");
        int id = InputUtils.inputInt("Nhập ID người dùng: ", 1, Integer.MAX_VALUE);
        
        List<User> list = userService.getAllUsers();
        User existing = list.stream().filter(u -> u.getUserId() == id).findFirst().orElse(null);
        if (existing == null) {
            throw new BusinessException("NOT_FOUND", "Không tìm thấy User với ID " + id);
        }

        if (existing.getRole() != null && existing.getRole().getRoleId() == 1) {
            PrintUtils.printWarning("Không thể khóa tài khoản ADMIN.");
            return;
        }

        UserStatus newStatus = existing.getStatus() == UserStatus.ACTIVE ? UserStatus.LOCKED : UserStatus.ACTIVE;
        System.out.println("Trạng thái hiện tại: " + existing.getStatus() + " -> Sẽ chuyển thành: " + newStatus);
        String yN = InputUtils.inputString("Xác nhận thay đổi? (Y/N): ");
        if (yN.equalsIgnoreCase("y")) {
            // Truyền adminUser (actor) để ghi vào system_logs
            userService.updateUserStatus(id, newStatus, adminUser);
            PrintUtils.printSuccess("Đã cập nhật trạng thái thành công!");
        } else {
            System.out.println("Đã hủy thao tác.");
        }
    }

    private void topUpBalance() throws BusinessException {
        System.out.println("\n[NẠP TIỀN CHO KHÁCH HÀNG]");
        int id = InputUtils.inputInt("Nhập ID người dùng cần nạp: ", 1, Integer.MAX_VALUE);
        BigDecimal amount = InputUtils.inputBigDecimal("Nhập số tiền muốn nạp (VND): ", BigDecimal.ONE);
        
        // Truyền adminUser (actor) để ghi vào system_logs
        userService.topUpUser(id, amount, adminUser);
        PrintUtils.printSuccess("Đã nạp " + FormatUtils.formatVND(amount) + " thành công cho User ID: " + id);
    }

    // -------------------------------------------------------
    // Xem Audit Log (Admin — xem toàn bộ)
    // -------------------------------------------------------

    private void viewLogsMenu() throws BusinessException {
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
