package com.cyber.view;

import com.cyber.util.PrintUtils;

import com.cyber.exception.BusinessException;
import com.cyber.model.User;
import com.cyber.service.AuthService;
import com.cyber.util.InputUtils;

/**
 * Presentation layer responsible for handling the Authentication console UI.
 */
public class AuthView {
    
    private final AuthService authService;
    private User currentUser;

    public AuthView() {
        this.authService = AuthService.getInstance();
    }

    /**
     * Entry method to display the core Authentication menu.
     * Locks the user in this menu until a valid login context is captured or the user exits.
     */
    public void displayAuthMenu() {
        while (currentUser == null) {
            System.out.println("\n==========================================");
            System.out.println("   HỆ THỐNG QUẢN LÝ CYBER GAMING & F&B    ");
            System.out.println("==========================================");
            System.out.println("1. Đăng nhập");
            System.out.println("2. Đăng ký");
            System.out.println("0. Thoát hệ thống");
            System.out.println("==========================================");
            
            int choice = InputUtils.inputInt("Vui lòng chọn chức năng (0-2): ", 0, 2);
            
            switch (choice) {
                case 1:
                    handleLogin();
                    break;
                case 2:
                    handleRegister();
                    break;
                case 0:
                    System.out.println("Cảm ơn bạn đã sử dụng hệ thống. Hẹn gặp lại!");
                    System.exit(0);
            }
        }
    }

    /**
     * Sub-routine handling the user login data input process.
     * Contains built-in try-catch logic allowing continuous retry on failure.
     */
    private void handleLogin() {
        System.out.println("\n--- ĐĂNG NHẬP ---");
        while (true) {
            String username = InputUtils.inputString("Tài khoản: ");
            String password = InputUtils.inputPassword("Mật khẩu: ");
            
            try {
                // Throws BusinessException if credentials fail logic
                currentUser = authService.login(username, password);
                
                // ANSI Green indicating Success
                PrintUtils.printSuccess("Đăng nhập thành công!");
                
                // Route mapping based on the User's Role capability
                String role = currentUser.getRole() != null ? currentUser.getRole().getRoleName().toUpperCase() : "CUSTOMER";
                String name = currentUser.getFullName() != null ? currentUser.getFullName() : username;

                if ("ADMIN".equals(role)) {
                    System.out.println("Xin chào Admin " + name + ", đang chuyển vào màn hình Quản trị...");
                } else if ("STAFF".equals(role)) {
                    System.out.println("Xin chào Nhân viên " + name + ", đang chuyển vào màn hình Thu ngân/Phục vụ...");
                } else {
                    System.out.println("Xin chào " + name + ", chúc bạn trải nghiệm dịch vụ vui vẻ!");
                }
                
                break; // Break loop, return control back to Main or displayAuthMenu (which collapses since currentUser != null)
                
            } catch (BusinessException e) {
                // ANSI Red indicating Error bounds
                PrintUtils.printError(e.getMessage());
                System.out.println("Vui lòng nhập lại thông tin!\n");
            }
        }
    }

    /**
     * Sub-routine handling the logic for new user registration.
     */
    private void handleRegister() {
        System.out.println("\n--- ĐĂNG KÝ TÀI KHOẢN ---");
        while (true) {
            User newUser = new User();
            String rawPassword = newUser.inputRegisterData();
            
            try {
                authService.register(newUser, rawPassword);
                PrintUtils.printSuccess("Đăng ký tài khoản thành công! Bạn có thể đăng nhập ngay bây giờ.");
                break;
            } catch (BusinessException e) {
                PrintUtils.printError(e.getMessage());
                System.out.println("Vui lòng thử lại!\n");
            }
        }
    }

    /**
     * Exposes the active logged-in user to the main application orchestrator context.
     * 
     * @return Validated User session state object.
     */
    public User getCurrentUser() {
        return currentUser;
    }
}
