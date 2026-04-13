package com.cyber.view;

import com.cyber.exception.BusinessException;
import com.cyber.model.User;
import com.cyber.model.enums.LogType;
import com.cyber.model.enums.UserRole;
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
            System.out.println("\n--- QUẢN LÝ NGƯỜI DÙNG ---");
            System.out.println("1. Danh sách người dùng");
            System.out.println("2. Thêm người dùng mới");
            System.out.println("3. Sửa thông tin người dùng");
            System.out.println("4. Khóa / Mở khóa tài khoản");
            System.out.println("5. Nạp tiền cho tài khoản");
            System.out.println("6. Trừ tiền / Rút tiền");
            System.out.println("7. Xóa tài khoản");
            System.out.println("0. Quay Lại");

            int choice = InputUtils.inputInt("Chọn (0-7): ", 0, 7);
            try {
                switch (choice) {
                    case 1: showAllUsers();    break;
                    case 2: addUser();          break;
                    case 3: editUser();         break;
                    case 4: toggleUserStatus(); break;
                    case 5: topUpBalance();     break;
                    case 6: deductBalance();    break;
                    case 7: handleDeleteUser(); break;
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
                        u.getRole() != null ? u.getRole().name() : "---",
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
        user.setRole(roleChoice == 1 ? UserRole.STAFF : UserRole.CUSTOMER);
        
        authService.register(user, pw, adminUser);
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
        if (existing.getRole() == UserRole.ADMIN) {
            PrintUtils.printWarning("Không được phép sửa thông tin của Quản trị viên (ADMIN).");
            return;
        }
        if (existing.getStatus() == UserStatus.LOCKED) {
            PrintUtils.printWarning("Tài khoản đang bị khóa, không được phép chỉnh sửa.");
            return;
        }

        System.out.println("Đang thao tác với Username: " + existing.getUsername());
        System.out.println("1. Sửa thông tin cơ bản (Họ tên, SĐT, Quyền)");
        System.out.println("2. Đổi thông tin đăng nhập (Username, Mật khẩu)");
        int choice = InputUtils.inputInt("Chọn chức năng (1-2): ", 1, 2);

        if (choice == 1) {
            String newName = InputUtils.inputStringUpdate(
                    "Nhập họ tên mới (Cũ: " + existing.getFullName() + ") [Enter để giữ nguyên]: ", existing.getFullName());
            existing.setFullName(newName);
            
            String oldPhone = existing.getPhone() != null ? existing.getPhone() : "";
            String newPhone = InputUtils.inputStringUpdate(
                    "Nhập SĐT mới (Cũ: " + (oldPhone.isEmpty() ? "---" : oldPhone) + ") [Enter để giữ nguyên]: ", 
                    oldPhone, User.PHONE_REGEX, User.PHONE_ERROR_MSG);
            existing.setPhone(newPhone);
            
            // Chỉ cho phép chọn STAFF hoặc CUSTOMER
            String oldRoleName = existing.getRole() != null ? existing.getRole().name() : "CUSTOMER";
            System.out.println("Chọn Role (1. STAFF | 2. CUSTOMER - Cũ: " + oldRoleName + "): ");
            int oldRoleInt = existing.getRole() == UserRole.STAFF ? 1 : 2;
            int rChoice = InputUtils.inputIntUpdate("Nhập Role mới [Enter để giữ nguyên]: ", oldRoleInt, 1, 2);
            existing.setRole(rChoice == 1 ? UserRole.STAFF : UserRole.CUSTOMER);

            userService.updateUser(existing, adminUser);
            PrintUtils.printSuccess("Cập nhật thông tin cơ bản thành công!");
        } else {
            // Username
            while (true) {
                String newUsername = InputUtils.inputStringUpdate(
                        "Tài khoản mới (Cũ: " + existing.getUsername() + ") [Enter để giữ nguyên]: ", existing.getUsername());
                if (!newUsername.equalsIgnoreCase(existing.getUsername()) && userService.checkUsernameDuplicate(newUsername, existing.getUserId())) {
                    PrintUtils.printError("Username đã tồn tại, vui lòng chọn tên khác!");
                } else {
                    existing.setUsername(newUsername);
                    break;
                }
            }
            userService.updateUser(existing, adminUser); // Update username info
            
            // Password
            String updatePwd = InputUtils.inputString("Bạn có muốn đổi mật khẩu không? (Y/N): ");
            if (updatePwd.equalsIgnoreCase("Y")) {
                String oldRaw = InputUtils.inputPassword("Nhập mật khẩu cũ hiện tại (bắt buộc): ");
                String newRaw;
                while (true) {
                    newRaw = InputUtils.inputRegisterPassword("Nhập mật khẩu mới: ");
                    String confirm = InputUtils.inputPassword("Xác nhận mật khẩu mới: ");
                    if (newRaw.equals(confirm)) break;
                    PrintUtils.printError("Mật khẩu xác nhận không khớp!");
                }
                authService.changeUserPassword(existing.getUserId(), oldRaw, newRaw);
                PrintUtils.printSuccess("Đổi thông tin đăng nhập thành công!");
            }
        }
    }


    private void toggleUserStatus() throws BusinessException {
        System.out.println("\n[KHÓA / MỞ KHÓA TÀI KHOẢN]");
        int id = InputUtils.inputInt("Nhập ID người dùng: ", 1, Integer.MAX_VALUE);
        
        List<User> list = userService.getAllUsers();
        User existing = list.stream().filter(u -> u.getUserId() == id).findFirst().orElse(null);
        if (existing == null) {
            throw new BusinessException("NOT_FOUND", "Không tìm thấy User với ID " + id);
        }

        if (existing.getRole() == UserRole.ADMIN) {
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

    private void deductBalance() throws BusinessException {
        System.out.println("\n[TRỪ TIỀN / RÚT TIỀN]");
        int id = InputUtils.inputInt("Nhập ID người dùng cần trừ tiền: ", 1, Integer.MAX_VALUE);

        User targetUser = userService.getUserById(id);

        if (targetUser.getRole() == UserRole.ADMIN) {
            PrintUtils.printWarning("Không được phép trừ tiền tài khoản ADMIN.");
            return;
        }

        System.out.println("Tài khoản: " + targetUser.getUsername() + " | Số dư hiện tại: " + FormatUtils.formatVND(targetUser.getBalance()));

        if (targetUser.getBalance().compareTo(BigDecimal.ZERO) == 0) {
            PrintUtils.printWarning("Tài khoản đã có số dư = 0đ, không cần trừ thêm.");
            return;
        }

        BigDecimal amount = InputUtils.inputBigDecimal("Nhập số tiền muốn trừ (VND): ", BigDecimal.ONE);
        
        userService.deductBalanceManual(id, amount, adminUser);
        PrintUtils.printSuccess("Đã trừ " + FormatUtils.formatVND(amount) + " thành công cho User: " + targetUser.getUsername());
    }

    private void handleDeleteUser() throws BusinessException {
        System.out.println("\n[XÓA TÀI KHOẢN (VĨNH VIỄN)]");
        PrintUtils.printWarning("Lưu ý: Tài khoản bị xóa sẽ không thể khôi phục. Dữ liệu lịch sử giao dịch vẫn được giữ.");
        int id = InputUtils.inputInt("Nhập ID người dùng cần xóa (0 để hủy): ", 0, Integer.MAX_VALUE);
        if (id == 0) return;

        User targetUser = userService.getUserById(id);
        System.out.println("Bạn sắp XÓA tài khoản: " + targetUser.getUsername() + " (" + targetUser.getFullName() + ")");
        System.out.println("Số dư hiện tại: " + FormatUtils.formatVND(targetUser.getBalance()));

        String confirm = InputUtils.inputString("Xác nhận XÓA? (Nhập 'DELETE' để xác nhận): ");
        if (confirm.equals("DELETE")) {
            userService.deleteUser(id, adminUser);
            PrintUtils.printSuccess("Đã xóa tài khoản " + targetUser.getUsername() + " thành công!");
        } else {
            System.out.println("Đã hủy thao tác.");
        }
    }
}

