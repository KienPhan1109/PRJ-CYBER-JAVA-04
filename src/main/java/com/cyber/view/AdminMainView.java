package com.cyber.view;

import com.cyber.model.User;
import com.cyber.util.InputUtils;
import com.cyber.util.PrintUtils;

public class AdminMainView {

    private final User adminUser;
    private final ComputerManagementView  computerView;
    private final FbMenuManagementView    fbMenuView;       // Quản lý Menu F&B nâng cao Phase 2
    private final UserManagementView      userView;

    public AdminMainView(User adminUser) {
        this.adminUser    = adminUser;
        this.computerView = new ComputerManagementView();
        this.fbMenuView   = new FbMenuManagementView();
        this.userView     = new UserManagementView();
    }

    public void displayMenu() {
        while (true) {
            System.out.println("\n==========================================");
            System.out.println("        QUẢN TRỊ VIÊN (ADMIN) PANEL       ");
            System.out.println("        Xin chào: " + adminUser.getFullName());
            System.out.println("==========================================");
            System.out.println("1. Quản lý Máy trạm");
            System.out.println("2. Quản lý Menu F&B");
            System.out.println("3. Quản lý hệ thống Người dùng");
            System.out.println("0. Đăng xuất");
            System.out.println("==========================================");

            int choice = InputUtils.inputInt("Vui lòng chọn chức năng (0-3): ", 0, 3);

            switch (choice) {
                case 1 -> computerView.displayMenu();
                case 2 -> fbMenuView.displayMenu();
                case 3 -> userView.displayMenu();
                case 0 -> {
                    PrintUtils.printWarning("Đang đăng xuất khỏi hệ thống Admin...");
                    return;
                }
            }
        }
    }
}
