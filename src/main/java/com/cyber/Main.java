package com.cyber;

import com.cyber.model.User;
import com.cyber.view.AdminMainView;
import com.cyber.view.AuthView;

public class Main {
    public static void main(String[] args) {
        while (true) {
            AuthView authView = new AuthView();
            authView.displayAuthMenu();
            
            User loggedInUser = authView.getCurrentUser();
            if (loggedInUser != null) {
                String roleName = loggedInUser.getRole() != null ? loggedInUser.getRole().getRoleName().toUpperCase() : "CUSTOMER";
                
                if ("ADMIN".equals(roleName)) {
                    AdminMainView adminView = new AdminMainView(loggedInUser);
                    adminView.displayMenu(); // Loops inside until choice == 0
                } else {
                    System.out.println("\n[THÔNG BÁO] Các tính năng Client/Staff đang được phát triển.");
                    break;
                }
            } else {
                break; // If somehow AuthView exits without exiting the program
            }
        }
    }
}
