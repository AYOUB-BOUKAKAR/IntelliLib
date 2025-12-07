package com.intellilib.controllers;

import com.intellilib.services.*;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.control.Alert;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Controller
@RequiredArgsConstructor  // Spring injects all these services
public class DashboardController {

    @FXML private Label totalAdminsLabel;
    @FXML private Label totalBooksLabel;
    @FXML private Label totalMembersLabel;
    @FXML private Label totalCategoriesLabel;
    @FXML private Label totalBorrowsLabel;
    @FXML private VBox dashboardContent;

    // Spring injects these automatically
    private final UserService userService;
    private final BookService bookService;
    private final MemberService memberService;
    private final CategoryService categoryService;
    private final BorrowService borrowService;
    
    private final ExecutorService executor = Executors.newCachedThreadPool();

    @FXML
    public void initialize() {
        loadDashboardDataAsync();
    }

    private void loadDashboardDataAsync() {
        executor.submit(() -> {
            try {
                // Use injected services
                long admins = userService.getAllUsers().stream()
                        .filter(user -> user.getRole() == com.intellilib.models.User.UserRole.ADMIN)
                        .count();
                long books = bookService.getAllBooks().size();
                long members = memberService.getAllMembers().size();
                long categories = categoryService.getAllCategories().size();
                long borrows = borrowService.getActiveBorrows().size();

                // Update UI on JavaFX Application Thread
                javafx.application.Platform.runLater(() -> {
                    totalAdminsLabel.setText(String.valueOf(admins));
                    totalBooksLabel.setText(String.valueOf(books));
                    totalMembersLabel.setText(String.valueOf(members));
                    totalCategoriesLabel.setText(String.valueOf(categories));
                    totalBorrowsLabel.setText(String.valueOf(borrows));
                });
            } catch (Exception e) {
                javafx.application.Platform.runLater(() -> 
                    showError("Error Loading Data", "Failed to load dashboard data: " + e.getMessage())
                );
            }
        });
    }

    private void showError(String title, String message) {
        javafx.application.Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    @FXML
    private void shutdown() {
        executor.shutdown();
    }
}