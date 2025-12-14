package com.intellilib.controllers;

import com.intellilib.models.Borrow;
import com.intellilib.models.User;
import com.intellilib.services.UserService;
import com.intellilib.services.BookService;
import com.intellilib.services.BorrowService;
import com.intellilib.util.FXMLLoaderUtil;
import javafx.stage.Stage;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.Alert;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import org.springframework.stereotype.Controller;

import java.time.LocalDate;
import java.util.List;

@Controller
public class MemberDashboardController extends BaseDashboardController {

    @FXML private Label welcomeLabel;
    @FXML private Label activeBorrowingsLabel;
    @FXML private Label overdueBooksLabel;
    @FXML private Label finesLabel;
    @FXML private Label totalBorrowedLabel;

    @FXML private TableView<Borrow> recentBooksTable;
    @FXML private TableColumn<Borrow, String> bookTitleColumn;
    @FXML private TableColumn<Borrow, LocalDate> borrowDateColumn;
    @FXML private TableColumn<Borrow, LocalDate> dueDateColumn;
    @FXML private TableColumn<Borrow, LocalDate> returnDateColumn;
    @FXML private TableColumn<Borrow, String> statusColumn;

    private final BookService bookService;
    private final BorrowService borrowService;

    public MemberDashboardController(UserService userService, BookService bookService, BorrowService borrowService) {
        super(userService);
        this.bookService = bookService;
        this.borrowService = borrowService;
    }

    @Override
    protected void loadDashboardData() {
        if (currentUser == null) return;

        // Set welcome message
        welcomeLabel.setText("Bienvenue, " + currentUser.getUsername() + "!");

        // Load member statistics
        loadMemberStatistics();

        // Load recent borrowings
        loadRecentBorrowings();
    }

    private void loadMemberStatistics() {
        try {
            if (currentUser == null) return;

            // Get active borrowings count
            long activeBorrowings = borrowService.countActiveBorrowingsForMember(currentUser.getId());
            activeBorrowingsLabel.setText(String.valueOf(activeBorrowings));

            // Get overdue books count
            long overdueBooks = borrowService.countOverdueBooksForMember(currentUser.getId());
            overdueBooksLabel.setText(String.valueOf(overdueBooks));

            // Calculate fines
            double fines = borrowService.calculateFinesForMember(currentUser.getId());
            finesLabel.setText(String.format("%.2f €", fines));

            // Get total books borrowed
            long totalBorrowed = borrowService.countTotalBorrowsForMember(currentUser.getId());
            totalBorrowedLabel.setText(String.valueOf(totalBorrowed));

        } catch (Exception e) {
            showError("Erreur", "Impossible de charger les statistiques");
            e.printStackTrace();
        }
    }

    private void loadRecentBorrowings() {
        try {
            if (currentUser == null) return;

            // Initialize table columns
            bookTitleColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getBook().getTitle()));
            borrowDateColumn.setCellValueFactory(new PropertyValueFactory<>("borrowDate"));
            dueDateColumn.setCellValueFactory(new PropertyValueFactory<>("dueDate"));
            returnDateColumn.setCellValueFactory(new PropertyValueFactory<>("returnDate"));
            statusColumn.setCellValueFactory(cellData -> {
                Borrow borrow = cellData.getValue();
                if (borrow.isReturned()) {
                    return new SimpleStringProperty("Retourné");
                } else if (borrow.getDueDate().isBefore(LocalDate.now())) {
                    return new SimpleStringProperty("En retard");
                } else {
                    return new SimpleStringProperty("En cours");
                }
            });

            // Load data
            List<Borrow> recentBorrowings = borrowService.getRecentBorrowingsForMember(currentUser.getId(), 10);
            recentBooksTable.getItems().setAll(recentBorrowings);

        } catch (Exception e) {
            showError("Erreur", "Impossible de charger les emprunts récents");
            e.printStackTrace();
        }
    }

    @FXML
    private void browseBooks() {
        try {
            Stage stage = FXMLLoaderUtil.loadStage("/views/browse-books.fxml", "Parcourir les Livres", true);
            stage.show();
        } catch (Exception e) {
            showError("Erreur", "Impossible d'ouvrir le catalogue");
            e.printStackTrace();
        }
    }

    @FXML
    private void viewMyBorrowings() {
        try {
            Stage stage = FXMLLoaderUtil.loadStage("/views/my-borrowings.fxml", "Mes Emprunts", true);
            stage.show();
        } catch (Exception e) {
            showError("Erreur", "Impossible d'ouvrir mes emprunts");
            e.printStackTrace();
        }
    }

    @FXML
    private void viewMyFines() {
        try {
            Stage stage = FXMLLoaderUtil.loadStage("/views/my-fines.fxml", "Mes Amendes", true);
            stage.show();
        } catch (Exception e) {
            showError("Erreur", "Impossible d'ouvrir mes amendes");
            e.printStackTrace();
        }
    }

    @FXML
    private void updateProfile() {
        try {
            Stage stage = FXMLLoaderUtil.loadStage("/views/update-profile.fxml", "Modifier Mon Profil", true);
            stage.show();
        } catch (Exception e) {
            showError("Erreur", "Impossible d'ouvrir la modification du profil");
            e.printStackTrace();
        }
    }

    @FXML
    private void viewRecommendations() {
        try {
            Stage stage = FXMLLoaderUtil.loadStage("/views/recommendations.fxml", "Recommandations", true);
            stage.show();
        } catch (Exception e) {
            showError("Erreur", "Impossible d'ouvrir les recommandations");
            e.printStackTrace();
        }
    }

    @FXML
    private void viewReadingHistory() {
        try {
            Stage stage = FXMLLoaderUtil.loadStage("/views/reading-history.fxml", "Historique de Lecture", true);
            stage.show();
        } catch (Exception e) {
            showError("Erreur", "Impossible d'ouvrir l'historique de lecture");
            e.printStackTrace();
        }
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

    @FXML
    private void refreshDashboard() {
        loadMemberStatistics();
        loadRecentBorrowings();
    }
}