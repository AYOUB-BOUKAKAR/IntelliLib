package com.intellilib.controllers;

import com.intellilib.models.User;
import com.intellilib.services.UserService;
import javafx.fxml.Initializable;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.util.ResourceBundle;

@Component
public abstract class BaseDashboardController implements Initializable {
    protected final UserService userService;
    protected User currentUser;

    protected BaseDashboardController(UserService userService) {
        this.userService = userService;
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Get current user from UserService (which uses SessionManager)
        this.currentUser = userService.getCurrentUser();
        if (currentUser == null) {
            // Optionally redirect to login if no user is logged in
            // For now, just return
            return;
        }
        loadDashboardData();
    }

    protected abstract void loadDashboardData();
    
    // Helper methods for subclasses
    protected boolean isCurrentUserAdmin() {
        return currentUser != null && currentUser.getRole() == User.UserRole.ADMIN;
    }
    
    protected boolean isCurrentUserLibrarian() {
        return currentUser != null && currentUser.getRole() == User.UserRole.LIBRARIAN;
    }
    
    protected boolean isCurrentUserMember() {
        return currentUser != null && currentUser.getRole() == User.UserRole.MEMBER;
    }
}