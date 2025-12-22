package com.intellilib.controllers;

import com.intellilib.models.Activity;
import com.intellilib.models.User;
import com.intellilib.services.BookService;
import com.intellilib.services.BorrowService;
import com.intellilib.services.UserService;
import com.intellilib.services.ActivityService;
import com.intellilib.services.DatabaseService;
import com.intellilib.util.FXMLLoaderUtil;
import javafx.stage.Stage;
import javafx.scene.chart.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import org.springframework.stereotype.Controller;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Controller
public class AdminDashboardController extends BaseDashboardController {
    
    @FXML private Label welcomeLabel;
    @FXML private Label totalBooksLabel;
    @FXML private Label activeMembersLabel;
    @FXML private Label activeBorrowingsLabel;
    @FXML private Label overdueBooksLabel;
    @FXML private Label totalFinesLabel;

    // Statistics change labels
    @FXML private Label totalBooksChangeLabel;
    @FXML private Label activeMembersChangeLabel;
    @FXML private Label activeBorrowingsChangeLabel;
    @FXML private Label overdueBooksChangeLabel;
    @FXML private Label totalFinesChangeLabel;

    // Statistics change properties
    private final SimpleStringProperty totalBooksChange = new SimpleStringProperty("+0%");
    private final SimpleStringProperty activeMembersChange = new SimpleStringProperty("+0%");
    private final SimpleStringProperty activeBorrowingsChange = new SimpleStringProperty("+0%");
    private final SimpleStringProperty overdueBooksChange = new SimpleStringProperty("-0%");
    private final SimpleStringProperty totalFinesChange = new SimpleStringProperty("+0%");
    
    // Chart labels
    @FXML private Label totalActivitiesLabel;
    @FXML private Label avgDailyActivitiesLabel;
    @FXML private Label mostActiveDayLabel;
    @FXML private Label mostActiveUserLabel;
    
    // Charts
    @FXML private LineChart<String, Number> dailyActivityChart;
    @FXML private CategoryAxis xAxisDaily;
    @FXML private NumberAxis yAxisDaily;
    
    @FXML private BarChart<String, Number> activityTypeChart;
    @FXML private CategoryAxis xAxisTypes;
    @FXML private NumberAxis yAxisTypes;
    
    @FXML private BarChart<String, Number> userActivityChart;
    @FXML private CategoryAxis xAxisUsers;
    @FXML private NumberAxis yAxisUsers;
    
    // Tables
    @FXML private TableView<Activity> recentActivityTable;
    @FXML private TableColumn<Activity, String> userColumn;
    @FXML private TableColumn<Activity, String> actionColumn;
    @FXML private TableColumn<Activity, String> descriptionColumn;
    @FXML private TableColumn<Activity, String> timestampColumn;
    
    @FXML private TableView<User> recentUsersTable;
    @FXML private TableColumn<User, String> usernameColumn;
    @FXML private TableColumn<User, String> emailColumn;
    @FXML private TableColumn<User, String> roleColumn;
    @FXML private TableColumn<User, String> joinedDateColumn;
    
    private final BookService bookService;
    private final BorrowService borrowService;
    private final ActivityService activityService;
    private final DatabaseService databaseService;
    
    public AdminDashboardController(UserService userService, BookService bookService, 
                                  BorrowService borrowService, ActivityService activityService, 
                                  DatabaseService databaseService) {
        super(userService);
        this.bookService = bookService;
        this.borrowService = borrowService;
        this.activityService = activityService;
        this.databaseService = databaseService;
    }
    
    @Override
    protected void loadDashboardData() {
        if (currentUser == null) return;

        totalBooksChangeLabel.textProperty().bind(totalBooksChange);
        activeMembersChangeLabel.textProperty().bind(activeMembersChange);
        activeBorrowingsChangeLabel.textProperty().bind(activeBorrowingsChange);
        overdueBooksChangeLabel.textProperty().bind(overdueBooksChange);
        totalFinesChangeLabel.textProperty().bind(totalFinesChange);
        
        // Set welcome message
        welcomeLabel.setText("Tableau de Bord Admin - " + currentUser.getUsername());
        
        // Load dashboard statistics
        loadDashboardStatistics();
        
        // Load all charts
        loadActivityCharts();
        
        // Load recent activity
        loadRecentActivity();
        
        // Load recent users
        loadRecentUsers();
    }
    
    private void loadDashboardStatistics() {
        try {
            // Get total books count and change
            long totalBooks = bookService.getTotalBooksCount();
            totalBooksLabel.setText(String.valueOf(totalBooks));
            double booksChange = bookService.getTotalBooksChangeFromLastMonth();
            updateChangeLabelStyle(totalBooksChangeLabel, booksChange);

            // Get active members count and change
            long activeMembers = userService.countActiveMembers();
            activeMembersLabel.setText(String.valueOf(activeMembers));
            double membersChange = userService.getActiveMembersChangeFromLastMonth();
            updateChangeLabelStyle(activeMembersChangeLabel, membersChange);

            // Get active borrowings count and change
            long activeBorrowings = borrowService.countActiveBorrowings();
            activeBorrowingsLabel.setText(String.valueOf(activeBorrowings));
            double borrowingsChange = borrowService.getActiveBorrowingsChangeFromLastMonth();
            updateChangeLabelStyle(activeBorrowingsChangeLabel, borrowingsChange);

            // Get overdue books count and change
            long overdueBooks = borrowService.countOverdueBooks();
            overdueBooksLabel.setText(String.valueOf(overdueBooks));
            double overdueChange = borrowService.getOverdueBooksChangeFromLastMonth();
            updateChangeLabelStyle(overdueBooksChangeLabel, overdueChange);

            // Calculate total fines and change
            double totalFines = borrowService.calculateTotalFines();
            totalFinesLabel.setText(String.format("%.2f €", totalFines));
            double finesChange = borrowService.getTotalFinesChangeFromLastMonth();
            updateChangeLabelStyle(totalFinesChangeLabel, finesChange);

            // Update properties instead of directly setting text
            totalBooksChange.set(String.format("%+.1f%%", booksChange) + " from last month");
            updateChangeLabelStyle(totalBooksChangeLabel, booksChange);

            activeMembersChange.set(String.format("%+.1f%%", membersChange) + " from last month");
            updateChangeLabelStyle(activeMembersChangeLabel, membersChange);
            
        } catch (Exception e) {
            showError("Erreur", "Impossible de charger les statistiques");
            e.printStackTrace();
        }
    }
    
    @SuppressWarnings("unchecked")
    private void loadActivityCharts() {
    try {
        // Get chart data for last 7 days
        Map<String, Object> chartData = activityService.getActivityChartData(7);
        
        // Extract data with null checks
        List<String> labels = (List<String>) chartData.getOrDefault("labels", new ArrayList<String>());
        List<Long> dailyActivity = (List<Long>) chartData.getOrDefault("dailyActivity", new ArrayList<Long>());
        List<String> activityTypes = (List<String>) chartData.getOrDefault("activityTypes", new ArrayList<String>());
        List<Long> typeCounts = (List<Long>) chartData.getOrDefault("typeCounts", new ArrayList<Long>());
        List<String> topUsers = (List<String>) chartData.getOrDefault("topUsers", new ArrayList<String>());
        List<Long> userActivityCounts = (List<Long>) chartData.getOrDefault("userActivityCounts", new ArrayList<Long>());
        
        // Update statistics labels with safe defaults
        long totalActivities = chartData.get("totalActivities") != null ? 
            ((Number) chartData.get("totalActivities")).longValue() : 0;
        long averageDaily = chartData.get("averageDaily") != null ? 
            ((Number) chartData.get("averageDaily")).longValue() : 0;
        
        totalActivitiesLabel.setText(String.valueOf(totalActivities));
        avgDailyActivitiesLabel.setText(String.valueOf(averageDaily));
            
            // Find most active day
            if (!dailyActivity.isEmpty()) {
                long maxActivity = 0;
                int maxIndex = 0;
                for (int i = 0; i < dailyActivity.size(); i++) {
                    if (dailyActivity.get(i) > maxActivity) {
                        maxActivity = dailyActivity.get(i);
                        maxIndex = i;
                    }
                }
                mostActiveDayLabel.setText(labels.get(maxIndex) + " (" + maxActivity + ")");
            }
            
            // Find most active user
            if (!userActivityCounts.isEmpty() && !topUsers.isEmpty()) {
                mostActiveUserLabel.setText(topUsers.get(0) + " (" + userActivityCounts.get(0) + ")");
            }
            
            // 1. Load Daily Activity Chart (Line Chart)
            loadDailyActivityChart(labels, dailyActivity);
            
            // 2. Load Activity Types Chart (Bar Chart)
            loadActivityTypesChart(activityTypes, typeCounts);
            
            // 3. Load User Activity Chart (Bar Chart)
            loadUserActivityChart(topUsers, userActivityCounts);
            
        } catch (Exception e) {
            showError("Erreur", "Impossible de charger les graphiques des activités");
            e.printStackTrace();
        }
    }
    
    private void loadDailyActivityChart(List<String> labels, List<Long> dailyActivity) {
        dailyActivityChart.getData().clear();
        
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Daily Activities");
        
        for (int i = 0; i < labels.size(); i++) {
            String label = labels.get(i);
            Long count = dailyActivity.get(i);
            series.getData().add(new XYChart.Data<>(label, count));
        }
        
        dailyActivityChart.getData().add(series);
        
        // Style the chart
        dailyActivityChart.setAnimated(true);
        dailyActivityChart.setLegendVisible(true);
        dailyActivityChart.getStyleClass().add("line-chart");
    }
    
    private void loadActivityTypesChart(List<String> types, List<Long> counts) {
        activityTypeChart.getData().clear();
        
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Activity Types");
        
        for (int i = 0; i < Math.min(types.size(), 8); i++) { // Show top 8 types
            String type = types.get(i);
            Long count = counts.get(i);
            series.getData().add(new XYChart.Data<>(type, count));
        }
        
        activityTypeChart.getData().add(series);
        
        // Style the chart
        activityTypeChart.setAnimated(true);
        activityTypeChart.setLegendVisible(false);
        activityTypeChart.getStyleClass().add("bar-chart");
    }
    
    private void loadUserActivityChart(List<String> users, List<Long> counts) {
        userActivityChart.getData().clear();
        
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("User Activity");
        
        for (int i = 0; i < Math.min(users.size(), 10); i++) { // Show top 10 users
            String user = users.get(i);
            Long count = counts.get(i);
            series.getData().add(new XYChart.Data<>(user, count));
        }
        
        userActivityChart.getData().add(series);
        
        // Style the chart
        userActivityChart.setAnimated(true);
        userActivityChart.setLegendVisible(false);
        userActivityChart.getStyleClass().add("bar-chart");
    }

    private void loadRecentActivity() {
        try {
            // Initialize table columns to match Activity model
            userColumn.setCellValueFactory(cellData -> {
                User user = cellData.getValue().getUser();
                if (user != null && user.getUsername() != null) {
                    return new SimpleStringProperty(user.getUsername());
                }
                return new SimpleStringProperty("System");
            });

            actionColumn.setCellValueFactory(new PropertyValueFactory<>("action"));
            descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));

            timestampColumn.setCellValueFactory(cellData ->
                    new SimpleStringProperty(
                            cellData.getValue().getTimestamp()
                                    .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                    )
            );

            // Load recent activity (last 20 actions)
            List<Activity> recentActivities = activityService.getRecentActivities(20);
            recentActivityTable.getItems().setAll(recentActivities);

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
            roleColumn.setCellValueFactory(cellData -> 
                new SimpleStringProperty(cellData.getValue().getRole().toString()));
            joinedDateColumn.setCellValueFactory(cellData -> 
                new SimpleStringProperty(
                    cellData.getValue().getCreatedAt()
                        .format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                )
            );
            
            // Get recent registered users
            List<User> recentUsers = activityService.getRecentRegisteredUsers(10);
            
            // If no users from activities, get from user service
            if (recentUsers.isEmpty()) {
                // Fallback: get users sorted by creation date
                recentUsers = userService.getAllUsers().stream()
                    .sorted((u1, u2) -> u2.getCreatedAt().compareTo(u1.getCreatedAt()))
                    .limit(10)
                    .toList();
            }
            
            // Update the table
            recentUsersTable.getItems().setAll(recentUsers);
            
        } catch (Exception e) {
            showError("Erreur", "Impossible de charger les utilisateurs récents");
            e.printStackTrace();
        }
    }
    
    @FXML
    private void refreshDashboard() {
        try {
            loadDashboardData();
            
            // Log activity
            activityService.logActivity(currentUser, "DASHBOARD_REFRESH", "Dashboard refreshed");

        } catch (Exception e) {
            showError("Erreur", "Impossible d'actualiser le dashboard");
            e.printStackTrace();
        }
    }
    
    @FXML
    private void manageBooks() {
        try {
            Stage stage = FXMLLoaderUtil.loadStage("/views/admin/manage-books.fxml", "Gérer les Livres", true);
            stage.show();
        } catch (Exception e) {
            showError("Erreur", "Impossible d'ouvrir la gestion des livres");
            e.printStackTrace();
        }
    }
    
    @FXML
    private void manageMembers() {
        try {
            Stage stage = FXMLLoaderUtil.loadStage("/views/admin/manage-members.fxml", "Gérer les Membres", true);
            stage.show();
        } catch (Exception e) {
            showError("Erreur", "Impossible d'ouvrir la gestion des utilisateurs");
            e.printStackTrace();
        }
    }
    
    @FXML
    private void manageUsers() {
        try {
            Stage stage = FXMLLoaderUtil.loadStage("/views/admin/manage-users.fxml", "Gérer les Utilisateurs", true);
            stage.show();
        } catch (Exception e) {
            showError("Erreur", "Impossible d'ouvrir la gestion des utilisateurs");
            e.printStackTrace();
        }
    }
    
    @FXML
    private void manageBorrowings() {
        try {
            Stage stage = FXMLLoaderUtil.loadStage("/views/admin/manage-borrows.fxml", "Gérer les Emprunts", true);
            stage.show();
        } catch (Exception e) {
            showError("Erreur", "Impossible d'ouvrir la gestion des emprunts");
            e.printStackTrace();
        }
    }

    @FXML
    private void manageCategories() {
        try {
            Stage stage = FXMLLoaderUtil.loadStage("/views/admin/manage-categories.fxml", "Gérer les Categories", true);
            stage.show();
        } catch (Exception e) {
            showError("Erreur", "Impossible d'ouvrir la gestion des categories");
            e.printStackTrace();
        }
    }

    @FXML
    private void manageFines() {
        try {
            Stage stage = FXMLLoaderUtil.loadStage("/views/admin/manage-fines.fxml", "Gérer les Pénalités", true);
            stage.show();
        } catch (Exception e) {
            showError("Erreur", "Impossible d'ouvrir la gestion des pénalités");
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
                    boolean success = databaseService.backupDatabase();
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

    private void updateChangeLabelStyle(Label label, double change) {
        label.getStyleClass().removeAll("change-positive", "change-negative", "change-neutral");

        if (change > 0) {
            label.getStyleClass().add("change-positive");
        } else if (change < 0) {
            label.getStyleClass().add("change-negative");
        } else {
            label.getStyleClass().add("change-neutral");
        }
    }
}