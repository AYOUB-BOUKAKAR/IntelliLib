package com.intellilib.controllers.member;


import com.intellilib.models.Borrow;
import com.intellilib.models.User;
import com.intellilib.services.UserService;
import com.intellilib.services.BorrowService;
import com.intellilib.util.FXMLLoaderUtil;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import org.springframework.stereotype.Controller;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Controller
public class MyBorrowingsController {

    @FXML private TabPane borrowingsTabPane;

    @FXML private TableView<Borrow> activeBorrowingsTable;
    @FXML private TableColumn<Borrow, String> activeBookColumn;
    @FXML private TableColumn<Borrow, LocalDate> borrowDateColumn;
    @FXML private TableColumn<Borrow, LocalDate> dueDateColumn;
    @FXML private TableColumn<Borrow, String> daysLeftColumn;
    @FXML private TableColumn<Borrow, String> returnActionColumn;

    @FXML private TableView<Borrow> historyBorrowingsTable;
    @FXML private TableColumn<Borrow, String> historyBookColumn;
    @FXML private TableColumn<Borrow, LocalDate> historyBorrowDateColumn;
    @FXML private TableColumn<Borrow, LocalDate> returnDateColumn;
    @FXML private TableColumn<Borrow, String> historyStatusColumn;

    @FXML private TableView<Borrow> overdueBorrowingsTable;
    @FXML private TableColumn<Borrow, String> overdueBookColumn;
    @FXML private TableColumn<Borrow, LocalDate> overdueBorrowDateColumn;
    @FXML private TableColumn<Borrow, LocalDate> overdueDueDateColumn;
    @FXML private TableColumn<Borrow, String> overdueDaysColumn;
    @FXML private TableColumn<Borrow, String> fineColumn;

    @FXML private Label activeCountLabel;
    @FXML private Label overdueCountLabel;
    @FXML private Label totalFineLabel;

    private final UserService userService;
    private final BorrowService borrowService;

    private User currentUser;
    private ObservableList<Borrow> allBorrowings = FXCollections.observableArrayList();
    private ObservableList<Borrow> activeBorrowings = FXCollections.observableArrayList();
    private ObservableList<Borrow> historyBorrowings = FXCollections.observableArrayList();
    private ObservableList<Borrow> overdueBorrowings = FXCollections.observableArrayList();

    public MyBorrowingsController(UserService userService, BorrowService borrowService) {
        this.userService = userService;
        this.borrowService = borrowService;
    }

    @FXML
    public void initialize() {
        currentUser = userService.getCurrentUser();
        if (currentUser == null) {
            showError("Erreur", "Utilisateur non connecté");
            goBack();
            return;
        }

        // setupTables();
        loadBorrowings();
        updateSummary();
    }

    // private void setupTables() {
    //     // Active borrowings table
    //     activeBookColumn.setCellValueFactory(cellData ->
    //         new SimpleStringProperty(cellData.getValue().getBook().getTitle()));
    //     borrowDateColumn.setCellValueFactory(new PropertyValueFactory<>("borrowDate"));
    //     dueDateColumn.setCellValueFactory(new PropertyValueFactory<>("dueDate"));
    //     daysLeftColumn.setCellValueFactory(cellData -> {
    //         long days = ChronoUnit.DAYS.between(LocalDate.now(), cellData.getValue().getDueDate());
    //         return new SimpleStringProperty(days >= 0 ? String.valueOf(days) : "En retard");
    //     });

    //     // Return action column
    //     returnActionColumn.setCellFactory(col -> new TableCell<>() {
    //         private final Button returnButton = new Button("Retourner");

    //         @Override
    //         protected void updateItem(Borrow borrow, boolean empty) {
    //             super.updateItem(borrow, empty);
    //             if (empty || borrow == null) {
    //                 setGraphic(null);
    //             } else {
    //                 returnButton.setOnAction(e -> returnBook(borrow));
    //                 setGraphic(returnButton);
    //             }
    //         }
    //     });

    //     // History table
    //     historyBookColumn.setCellValueFactory(cellData ->
    //         new SimpleStringProperty(cellData.getValue().getBook().getTitle()));
    //     historyBorrowDateColumn.setCellValueFactory(new PropertyValueFactory<>("borrowDate"));
    //     returnDateColumn.setCellValueFactory(new PropertyValueFactory<>("returnDate"));
    //     historyStatusColumn.setCellValueFactory(cellData ->
    //         new SimpleStringProperty(cellData.getValue().isReturned() ? "Retourné" : "Perdu"));

    //     // Overdue table
    //     overdueBookColumn.setCellValueFactory(cellData ->
    //         new SimpleStringProperty(cellData.getValue().getBook().getTitle()));
    //     overdueBorrowDateColumn.setCellValueFactory(new PropertyValueFactory<>("borrowDate"));
    //     overdueDueDateColumn.setCellValueFactory(new PropertyValueFactory<>("dueDate"));
    //     overdueDaysColumn.setCellValueFactory(cellData -> {
    //         long days = ChronoUnit.DAYS.between(cellData.getValue().getDueDate(), LocalDate.now());
    //         return new SimpleStringProperty(String.valueOf(Math.max(0, days)));
    //     });
    //     fineColumn.setCellValueFactory(cellData ->
    //         new SimpleStringProperty(String.format("%.2f €", cellData.getValue().getFineAmount())));
    // }

    private void loadBorrowings() {
        List<Borrow> memberBorrowings = borrowService.getBorrowsByMember(currentUser.getId());
        allBorrowings.setAll(memberBorrowings);

        // Filter into categories
        activeBorrowings.setAll(allBorrowings.stream()
            .filter(borrow -> !borrow.isReturned() &&
                     borrow.getDueDate().isAfter(LocalDate.now()))
            .toList());

        historyBorrowings.setAll(allBorrowings.stream()
            .filter(Borrow::isReturned)
            .toList());

        overdueBorrowings.setAll(allBorrowings.stream()
            .filter(borrow -> !borrow.isReturned() &&
                     borrow.getDueDate().isBefore(LocalDate.now()))
            .toList());

        // Update tables
        activeBorrowingsTable.getItems().setAll(activeBorrowings);
        historyBorrowingsTable.getItems().setAll(historyBorrowings);
        overdueBorrowingsTable.getItems().setAll(overdueBorrowings);
    }

    private void updateSummary() {
        activeCountLabel.setText(String.valueOf(activeBorrowings.size()));
        overdueCountLabel.setText(String.valueOf(overdueBorrowings.size()));

        double totalFine = overdueBorrowings.stream()
            .mapToDouble(Borrow::getFineAmount)
            .sum();
        totalFineLabel.setText(String.format("%.2f €", totalFine));
    }

    private void returnBook(Borrow borrow) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Retourner un livre");
        confirm.setHeaderText("Retourner '" + borrow.getBook().getTitle() + "'");
        confirm.setContentText("Êtes-vous sûr de vouloir retourner ce livre ?");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    boolean success = borrowService.returnBook(borrow.getId());
                    if (success) {
                        showSuccess("Succès", "Livre retourné avec succès !");
                        loadBorrowings();
                        updateSummary();
                    } else {
                        showError("Erreur", "Impossible de retourner le livre");
                    }
                } catch (Exception e) {
                    showError("Erreur", "Erreur lors du retour: " + e.getMessage());
                }
            }
        });
    }

    @FXML
    private void refreshData() {
        loadBorrowings();
        updateSummary();
    }

    @FXML
    private void goBack() {
        try {
            Stage stage = (Stage) borrowingsTabPane.getScene().getWindow();
            stage.setScene(FXMLLoaderUtil.loadScene("/views/member-dashboard.fxml"));
            stage.setTitle("Tableau de Bord Membre");
        } catch (Exception e) {
            showError("Erreur", "Impossible de retourner au tableau de bord");
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