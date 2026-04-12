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
            System.out.println("0. Quay Lại");

            int choice = InputUtils.inputInt("Chọn (0-5): ", 0, 5);
            try {
                switch (choice) {
                    case 1: showAllUsers();    break;
                    case 2: addUser();          break;
                    case 3: editUser();         break;
                    case 4: toggleUserStatus(); break;
                    case 5: topUpBalance();     break;
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
        printUserTable(users, "DANH SÁCH NGƯỜI DÙNG");
    }

    /** In bảng User có phân trang (dùng chung) */
    private void printUserTable(List<User> users, String title) {
        int pageSize = 10;
        int totalPages = (int) Math.ceil((double) users.size() / pageSize);
        int currentPage = 1;

        while (true) {
            int start = (currentPage - 1) * pageSize;
            int end = Math.min(start + pageSize, users.size());

            System.out.println("\n" + "=".repeat(120));
            System.out.println("  " + title + " (Trang " + currentPage + "/" + totalPages + ")");
            System.out.println("=".repeat(120));
            System.out.printf("%-5s | %-15s | %-20s | %-12s | %-15s | %-12s | %-15s%n",
                    "ID", "Tài khoản", "Họ tên", "SĐT", "Số dư", "Quyền", "Trạng thái");
            System.out.println("-".repeat(120));

            for (int i = start; i < end; i++) {
                User u = users.get(i);
                System.out.printf("%-5d | %-15s | %-20s | %-12s | %-15s | %-12s | %-24s%n",
                        u.getUserId(),
                        FormatUtils.truncate(u.getUsername(), 15),
                        FormatUtils.truncate(u.getFullName(), 20),
                        FormatUtils.formatValue(u.getPhone()),
                        FormatUtils.formatVND(u.getBalance()),
                        u.getRole() != null ? u.getRole().getRoleName() : "---",
                        FormatUtils.formatUserStatus(u.getStatus()));
            }
            System.out.println("=".repeat(120));
            System.out.println("Tổng: " + users.size() + " người dùng | Trang " + currentPage + "/" + totalPages);

            if (totalPages <= 1) break;
            System.out.println("[N] Trang sau | [P] Trang trước | [Q] Thoát");
            String nav = InputUtils.inputString("Lựa chọn: ").toUpperCase();
            if (nav.equals("N") && currentPage < totalPages) currentPage++;
            else if (nav.equals("P") && currentPage > 1) currentPage--;
            else if (nav.equals("Q")) break;
        }
    }

    private void addUser() throws BusinessException {
        System.out.println("\n[THÊM NGƯỜI DÙNG MỚI]");
        User user = new User();
        String pw = user.inputRegisterData();
        
        System.out.println("Chọn Role (1. STAFF | 2. CUSTOMER): ");
        int roleChoice = InputUtils.inputInt("Nhập (1-2) [Mặc định 2]: ", 1, 2);
        String roleName = roleChoice == 1 ? "STAFF" : "CUSTOMER";
        int roleId = roleChoice == 1 ? 2 : 3;
        user.setRole(new Role(roleId, roleName));
        
        authService.register(user, pw);
        PrintUtils.printSuccess("Thêm người dùng thành công!");
    }

    private void editUser() throws BusinessException {
        System.out.println("\n[SỬA THÔNG TIN NGƯỜI DÙNG]");
        int id = InputUtils.inputInt("Nhập ID người dùng cần sửa: ", 1, Integer.MAX_VALUE);
        
        List<User> list = userService.getAllUsers();
        User existing = list.stream().filter(u -> u.getUserId() == id).findFirst().orElse(null);
        if (existing == null) {
            throw new BusinessException("NOT_FOUND", "Không tìm thấy User với ID " + id);
        }
        
        // Chặn sửa Admin
        if (existing.getRole() != null && existing.getRole().getRoleId() == 1) {
            PrintUtils.printWarning("Không được phép sửa thông tin của Quản trị viên (ADMIN).");
            return;
        }
        if (existing.getStatus() == UserStatus.LOCKED) {
            PrintUtils.printWarning("Tài khoản đang bị khóa, không được phép chỉnh sửa.");
            return;
        }

        System.out.println("Sửa thông tin cho Username: " + existing.getUsername());

        // Cho phép sửa tài khoản (Username)
        String newUsername = InputUtils.inputStringUpdate(
                "Tài khoản mới (Cũ: " + existing.getUsername() + ") [Enter để giữ nguyên]: ", existing.getUsername());
        existing.setUsername(newUsername);
        
        String newName = InputUtils.inputStringUpdate(
                "Nhập họ tên mới (Cũ: " + existing.getFullName() + ") [Enter để giữ nguyên]: ", existing.getFullName());
        existing.setFullName(newName);
        
        String oldPhone = existing.getPhone() != null ? existing.getPhone() : "";
        String newPhone = InputUtils.inputStringUpdate(
                "Nhập SĐT mới (Cũ: " + (oldPhone.isEmpty() ? "---" : oldPhone) + ") [Enter để giữ nguyên]: ", oldPhone);
        existing.setPhone(newPhone);
        
        // Chỉ cho phép chọn STAFF hoặc CUSTOMER
        int oldRoleId = existing.getRole() != null ? existing.getRole().getRoleId() : 3;
        System.out.println("Chọn Role (2. STAFF | 3. CUSTOMER - Cũ: " + oldRoleId + "): ");
        int rId = InputUtils.inputIntUpdate("Nhập Role mới [Enter để giữ nguyên]: ", oldRoleId, 2, 3);
        existing.setRole(new Role(rId, rId == 2 ? "STAFF" : "CUSTOMER"));

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
        String keyword = InputUtils.inputStringOptional("Nhập từ khóa tên/username (Để trống = hiện tất cả): ");
        List<User> list = userService.searchUsersByName(keyword);
        if (list.isEmpty()) {
            PrintUtils.printWarning("Không tìm thấy khách hàng nào khớp với từ khóa.");
            return;
        }

        // Hiển thị bằng bảng phân trang
        printUserTable(list, "KẾT QUẢ TÌM KIẾM");

        int id = InputUtils.inputInt("Nhập chính xác ID người dùng (0 để hủy): ", 0, Integer.MAX_VALUE);
        if (id == 0) return;
        
        User targetUser = list.stream().filter(u -> u.getUserId() == id).findFirst().orElse(null);
        if (targetUser == null) {
            PrintUtils.printWarning("ID không tồn tại trong danh sách tìm kiếm.");
            return;
        }
        if (targetUser.getStatus() == UserStatus.LOCKED) {
            PrintUtils.printWarning("Tài khoản đang bị khóa, không được phép nạp tiền.");
            return;
        }
        
        BigDecimal amount = InputUtils.inputBigDecimal("Nhập số tiền muốn nạp (VND): ", BigDecimal.ONE);
        
        userService.topUpUser(id, amount, adminUser);
        PrintUtils.printSuccess("Đã nạp " + FormatUtils.formatVND(amount) + " thành công cho User: " + targetUser.getUsername());
    }
}
