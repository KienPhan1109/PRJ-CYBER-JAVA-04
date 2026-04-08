package com.cyber.view;

import com.cyber.model.User;
import com.cyber.util.PrintUtils;

public class AppRouter {
    
    public void start() {
        while (true) {
            AuthView authView = new AuthView();
            authView.displayAuthMenu();
            
            User loggedInUser = authView.getCurrentUser();
            if (loggedInUser != null) {
                // Route navigation based on User Role
                String roleName = loggedInUser.getRole() != null ? loggedInUser.getRole().getRoleName().toUpperCase() : "CUSTOMER";
                
                if ("ADMIN".equals(roleName)) {
                    AdminMainView adminView = new AdminMainView(loggedInUser);
                    adminView.displayMenu(); // Loops inside until choice == 0 (Logout)
                } 
                else if ("STAFF".equals(roleName)) {
                    PrintUtils.printWarning("Các tính năng dành cho Staff đang được phát triển.");
                    break;
                }
                else {
                    PrintUtils.printWarning("Các tính năng dành cho Customer đang được phát triển.");
                    break;
                }
            } else {
                // If AuthView exits via "0" or returns null
                break;
            }
        }
    }
}
