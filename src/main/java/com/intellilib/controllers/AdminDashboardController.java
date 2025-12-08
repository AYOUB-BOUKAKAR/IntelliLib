package com.intellilib.controllers;

import com.intellilib.models.User;
import com.intellilib.services.BookService;
import com.intellilib.services.BorrowService;
import com.intellilib.services.UserService;
import com.intellilib.util.FXMLLoaderUtil;
import javafx.stage.Stage;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;

@Controller
public class AdminDashboardController extends BaseDashboardController {
    
    @FXML private Label welcomeLabel;
    @FXML private Label totalBooksLabel;
    @FXML private Label activeMembersLabel;
    @FXML private Label activeBorrowingsLabel;
    @FXML private Label overdueBooksLabel;
    @FXML private Label totalFinesLabel;
    
    @FXML private TableView<?> recentActivityTable;
    @FXML private TableColumn<?, ?> userColumn;
    @FXML private TableColumn<?, ?> actionColumn;
    @FXML private TableColumn<?, ?> timestampColumn;
    
    @FXML private TableView<?> recentUsersTable;
    @FXML private TableColumn<?, ?> usernameColumn;
    @FXML private TableColumn<?, ?> emailColumn;
    @FXML private TableColumn<?, ?> roleColumn;
    @FXML private TableColumn<?, ?> joinedDateColumn;
    
    private final BookService bookService;
    private final BorrowService BorrowService;
    
    public AdminDashboardController(UserService userService, BookService bookService, BorrowService BorrowService) {
        super(userService);
        this.bookService = bookService;
        this.BorrowService = BorrowService;
    }
    
    @Override
    protected void loadDashboardData() {
        if (currentUser == null) return;
        
        // Set welcome message
        welcomeLabel.setText("Tableau de Bord Admin - " + currentUser.getUsername());
        
        // Load dashboard statistics
        loadDashboardStatistics();
        
        // Load recent activity
        loadRecentActivity();
        
        // Load recent users
        loadRecentUsers();
    }
    
    private void loadDashboardStatistics() {
        try {
            // Get total books count
            long totalBooks = bookService.getTotalBooksCount();
            totalBooksLabel.setText(String.valueOf(totalBooks));
            
            // Get active members count
            long activeMembers = userService.countActiveMembers();
            activeMembersLabel.setText(String.valueOf(activeMembers));
            
            // Get active borrowings count
            long activeBorrowings = BorrowService.countActiveBorrowings();
            activeBorrowingsLabel.setText(String.valueOf(activeBorrowings));
            
            // Get overdue books count
            long overdueBooks = BorrowService.countOverdueBooks();
            overdueBooksLabel.setText(String.valueOf(overdueBooks));
            
            // Calculate total fines
            double totalFines = BorrowService.calculateTotalFines();
            totalFinesLabel.setText(String.format("%.2f €", totalFines));
            
        } catch (Exception e) {
            showError("Erreur", "Impossible de charger les statistiques");
            e.printStackTrace();
        }
    }
    
    private void loadRecentActivity() {
        try {
            // Initialize table columns
            userColumn.setCellValueFactory(new PropertyValueFactory<>("user"));
            actionColumn.setCellValueFactory(new PropertyValueFactory<>("action"));
            timestampColumn.setCellValueFactory(new PropertyValueFactory<>("timestamp"));
            
            // Load recent activity (last 20 actions)
            var recentActivity = userService.getRecentActivity(20);
            recentActivityTable.getItems().setAll(recentActivity);
            
        } catch (Exception e) {
            showError("Erreur", "Impossible de charger l'activité récente");
            e.printStackTrace();
        }
    }
    
    private void loadRecentUsers() {
        try {
            // Initialize table columns
            usernameColumn.setCellValueFactory(new PropertyValueFactory<>("username"));
            emailColumn.setCellValueFactory(new PropertyValueFactory<>("email"));
            roleColumn.setCellValueFactory(new PropertyValueFactory<>("role"));
            joinedDateColumn.setCellValueFactory(new PropertyValueFactory<>("createdAt"));
            
            // Load recent users (last 10 registrations)
            var recentUsers = userService.getRecentUsers(10);
            recentUsersTable.getItems().setAll(recentUsers);
            
        } catch (Exception e) {
            showError("Erreur", "Impossible de charger les utilisateurs récents");
            e.printStackTrace();
        }
    }
    
    @FXML
    private void manageBooks() {
        try {
            Stage stage = FXMLLoaderUtil.loadStage("/views/manage-books.fxml", "Gérer les Livres", true);
            stage.show();
        } catch (Exception e) {
            showError("Erreur", "Impossible d'ouvrir la gestion des livres");
            e.printStackTrace();
        }
    }
    
    @FXML
    private void manageUsers() {
        try {
            Stage stage = FXMLLoaderUtil.loadStage("/views/manage-users.fxml", "Gérer les Utilisateurs", true);
            stage.show();
        } catch (Exception e) {
            showError("Erreur", "Impossible d'ouvrir la gestion des utilisateurs");
            e.printStackTrace();
        }
    }
    
    @FXML
    private void manageBorrowings() {
        try {
            Stage stage = FXMLLoaderUtil.loadStage("/views/manage-borrowings.fxml", "Gérer les Emprunts", true);
            stage.show();
        } catch (Exception e) {
            showError("Erreur", "Impossible d'ouvrir la gestion des emprunts");
            e.printStackTrace();
        }
    }
    
    @FXML
    private void viewReports() {
        try {
            Stage stage = FXMLLoaderUtil.loadStage("/views/reports.fxml", "Rapports", true);
            stage.show();
        } catch (Exception e) {
            showError("Erreur", "Impossible d'ouvrir les rapports");
            e.printStackTrace();
        }
    }
    
    @FXML
    private void systemSettings() {
        try {
            Stage stage = FXMLLoaderUtil.loadStage("/views/system-settings.fxml", "Paramètres Système", true);
            stage.show();
        } catch (Exception e) {
            showError("Erreur", "Impossible d'ouvrir les paramètres");
            e.printStackTrace();
        }
    }
    
    @FXML
    private void backupDatabase() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Sauvegarde de la base de données");
        alert.setHeaderText("Sauvegarder la base de données ?");
        alert.setContentText("Cette action va créer une sauvegarde complète de la base de données.");
        
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    boolean success = userService.backupDatabase();
                    if (success) {
                        showSuccess("Succès", "Sauvegarde effectuée avec succès!");
                    } else {
                        showError("Erreur", "Échec de la sauvegarde");
                    }
                } catch (Exception e) {
                    showError("Erreur", "Erreur lors de la sauvegarde");
                    e.printStackTrace();
                }
            }
        });
    }
    
    @FXML
    private void logout() {
        try {
            // Clear current user
            userService.logout();
            
            // Go back to login
            Stage stage = FXMLLoaderUtil.loadStage("/views/login.fxml", "Connexion - IntelliLib", false);
            stage.show();
            
            // Close dashboard
            Stage dashboardStage = (Stage) welcomeLabel.getScene().getWindow();
            dashboardStage.close();
            
        } catch (Exception e) {
            showError("Erreur", "Impossible de se déconnecter");
            e.printStackTrace();
        }
    }
    
    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    private void showSuccess(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}