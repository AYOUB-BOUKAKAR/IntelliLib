package com.intellilib.util;

import com.intellilib.models.User;
import com.intellilib.services.ActivityService;
import org.springframework.stereotype.Component;

@Component
public class ActivityLogger {
    
    private final ActivityService activityService;
    
    public ActivityLogger(ActivityService activityService) {
        this.activityService = activityService;
    }

    // User section
    public void logLogin(User user) {
        activityService.logActivity(user, "USER_LOGIN", 
            "User logged into the system");
    }
    
    public void logLogout(User user) {
        activityService.logActivity(user, "USER_LOGOUT", 
            "User logged out of the system");
    }

    public void logRegister(User user) {
        activityService.logActivity(user, "USER_REGISTERED",
            "User registered");
    }

    public void logUserAdd(User user, String targetUsername) {
        activityService.logActivity(user, "USER_ADDED",
            String.format("Added new user: %s", targetUsername));
    }

    public void logUserUpdate(User user, String targetUsername) {
        activityService.logActivity(user, "USER_UPDATED",
            String.format("Updated user: %s", targetUsername));
    }

    public void logUserDelete(User user, String targetUsername) {
        activityService.logActivity(user, "USER_DELETED",
            String.format("Deleted user: %s", targetUsername));
    }

    //Member section
    public void logMemberAdd(User user, String memberName) {
        activityService.logActivity(user, "MEMBER_ADDED",
            String.format("Added new member: %s", memberName));
    }

    public void logMemberUpdate(User user, String memberName) {
        activityService.logActivity(user, "MEMBER_UPDATED",
            String.format("Updated member: %s", memberName));
    }

    public void logMemberDelete(User user, String memberName) {
        activityService.logActivity(user, "MEMBER_DELETED",
            String.format("Deleted member: %s", memberName));
    }

    // Book section
    public void logBookBorrow(User user, String bookTitle) {
        activityService.logActivity(user, "BOOK_BORROWED", 
            String.format("Borrowed book: %s", bookTitle));
    }
    
    public void logBookReturn(User user, String bookTitle) {
        activityService.logActivity(user, "BOOK_RETURNED", 
            String.format("Returned book: %s", bookTitle));
    }
    
    public void logBookAdd(User user, String bookTitle) {
        activityService.logActivity(user, "BOOK_ADDED", 
            String.format("Added new book: %s", bookTitle));
    }
    
    public void logBookUpdate(User user, String bookTitle) {
        activityService.logActivity(user, "BOOK_UPDATED", 
            String.format("Updated book: %s", bookTitle));
    }

    public void logBookDelete(User user, String bookTitle) {
        activityService.logActivity(user, "BOOK_DELETED",
            String.format("Deleted book: %s", bookTitle));
    }

    // Category section
    public void logCategoryAdd(User user, String categoryName) {
        activityService.logActivity(user, "CATEGORY_ADDED",
            String.format("Added new category: %s", categoryName));
    }

    public void logCategoryUpdate(User user, String categoryName) {
        activityService.logActivity(user, "CATEGORY_UPDATED",
            String.format("Updated category: %s", categoryName));
    }

    public void logCategoryDelete(User user, String categoryName) {
        activityService.logActivity(user, "CATEGORY_DELETED",
            String.format("Deleted category: %s", categoryName));
    }

    // Borrow section
    public void logBorrowAdd(User user, String bookTitle){
        activityService.logActivity(user, "BORROW_ADDED",
            String.format("Added new borrow: %s", bookTitle));
    }

    public void logBorrowUpdate(User user, String bookTitle){
        activityService.logActivity(user, "BORROW_UPDATED",
            String.format("Updated borrow: %s", bookTitle));
    }

    public void logBorrowDelete(User user, String bookTitle) {
        activityService.logActivity(user, "BORROW_DELETED",
            String.format("Deleted borrow: %s", bookTitle));
    }

    public void logDatabaseBackup(User user) {
        activityService.logActivity(user, "DATABASE_BACKUP", 
            "Database backup created");
    }
    
    public void logFinePayment(User user, String memberName, double amount) {
        activityService.logActivity(user, "FINE_PAID", 
            String.format("Fine paid for %s: %.2f €", memberName, amount));
    }
}