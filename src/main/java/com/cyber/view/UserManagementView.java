package com.cyber.view;

import com.cyber.exception.BusinessException;
import com.cyber.model.Role;
import com.cyber.model.User;
import com.cyber.model.enums.UserStatus;
import com.cyber.service.AuthService;
import com.cyber.service.UserService;
import com.cyber.util.FormatUtils;
import com.cyber.util.InputUtils;
import com.cyber.util.PrintUtils;

import java.math.BigDecimal;
import java.util.List;

public class UserManagementView {

    private final UserService userService;
    private final AuthService authService;

    public UserManagementView() {
        this.userService = UserService.getInstance();
        this.authService = AuthService.getInstance();
    }

    public void displayMenu() {
        while (true) {
            System.out.println("\n--- QUẢN LÝ NGƯỜI DÙNG ---");
            System.out.println("1. Danh sách người dùng");
            System.out.println("2. Thêm người dùng mới");
            System.out.println("3. Sửa thông tin người dùng");
            System.out.println("4. Khóa / Mở khóa tài khoản");
            System.out.println("5. Nạp tiền cho tài khoản");
            System.out.println("0. Quay Lại");

            int choice = InputUtils.inputInt("Chọn (0-5): ", 0, 5);
            try {
                switch (choice) {
                    case 1: showAllUsers(); break;
                    case 2: addUser(); break;
                    case 3: editUser(); break;
                    case 4: toggleUserStatus(); break;
                    case 5: topUpBalance(); break;
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
            userService.updateUserStatus(id, newStatus);
            PrintUtils.printSuccess("Đã cập nhật trạng thái thành công!");
        } else {
            System.out.println("Đã hủy thao tác.");
        }
    }

    private void topUpBalance() throws BusinessException {
        System.out.println("\n[NẠP TIỀN CHO KHÁCH HÀNG]");
        int id = InputUtils.inputInt("Nhập ID người dùng cần nạp: ", 1, Integer.MAX_VALUE);
        BigDecimal amount = InputUtils.inputBigDecimal("Nhập số tiền muốn nạp (VND): ", BigDecimal.ONE);
        
        userService.topUpUser(id, amount);
        PrintUtils.printSuccess("Đã nạp " + FormatUtils.formatVND(amount) + " thành công cho User ID: " + id);
    }
}
