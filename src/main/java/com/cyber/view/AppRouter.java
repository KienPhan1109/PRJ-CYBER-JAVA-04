package com.cyber.view;

import com.cyber.model.User;
import com.cyber.model.enums.UserRole;
import com.cyber.util.PrintUtils;

public class AppRouter {
    
    public void start() {
        while (true) {
            AuthView authView = new AuthView();
            authView.displayAuthMenu();
            
            User loggedInUser = authView.getCurrentUser();
            if (loggedInUser != null) {
                // Route navigation based on User Role
                UserRole role = loggedInUser.getRole() != null ? loggedInUser.getRole() : UserRole.CUSTOMER;
                
                if (role == UserRole.ADMIN) {
                    AdminMainView adminView = new AdminMainView(loggedInUser);
                    adminView.displayMenu(); // Loops inside until choice == 0 (Logout)
                } 
                else if (role == UserRole.STAFF) {
                    StaffMainView staffView = new StaffMainView(loggedInUser);
                    staffView.displayMenu();
                }
                else {
                    CustomerMainView customerView = new CustomerMainView(loggedInUser);
                    customerView.displayMenu();
                }
            } else {
                // If AuthView exits via "0" or returns null
                break;
            }
        }
    }
}
