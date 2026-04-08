package com.cyber.view;

import com.cyber.model.User;
import com.cyber.util.InputUtils;

public class AdminMainView {

    private final User adminUser;
    private final ComputerManagementView computerView;
    private final ServiceManagementView serviceView;

    public AdminMainView(User adminUser) {
        this.adminUser = adminUser;
        this.computerView = new ComputerManagementView();
        this.serviceView = new ServiceManagementView();
    }

    public void displayMenu() {
        while (true) {
            System.out.println("\n==========================================");
            System.out.println("        QUẢN TRỊ VIÊN (ADMIN) PANEL       ");
            System.out.println("        Xin chào: " + adminUser.getFullName());
            System.out.println("==========================================");
            System.out.println("1. Quản lý Máy trạm");
            System.out.println("2. Quản lý Dịch vụ F&B");
            System.out.println("0. Đăng xuất");
            System.out.println("==========================================");

            int choice = InputUtils.inputInt("Vui lòng chọn chức năng (0-2): ", 0, 2);

            switch (choice) {
                case 1:
                    computerView.displayMenu();
                    break;
                case 2:
                    serviceView.displayMenu();
                    break;
                case 0:
                    System.out.println("\033[33mĐang đăng xuất khỏi hệ thống Admin...\033[0m");
                    return;
            }
        }
    }
}
