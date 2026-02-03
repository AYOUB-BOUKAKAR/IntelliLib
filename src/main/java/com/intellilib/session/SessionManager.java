package com.intellilib.session;

import com.intellilib.models.User;
import org.springframework.stereotype.Component;

@Component
public class SessionManager {
    
    private User currentUser;
    
    public void login(User user) {
        this.currentUser = user;
    }
    
    public void logout() {
        if (currentUser != null) {
            System.out.println("User logged out: " + currentUser.getUsername());
        }
        this.currentUser = null;
    }
    
    public User getCurrentUser() {
        return currentUser;
    }
    
    public boolean isLoggedIn() {
        return currentUser != null;
    }
    
    public boolean hasRole(User.UserRole role) {
        return currentUser != null && currentUser.getRole() == role;
    }
    
    public boolean isAdmin() {
        return hasRole(User.UserRole.ADMIN);
    }
    
    public boolean isLibrarian() {
        return hasRole(User.UserRole.LIBRARIAN);
    }
    
    public boolean isMember() {
        return hasRole(User.UserRole.MEMBER);
    }
}