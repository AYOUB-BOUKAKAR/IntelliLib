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
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Controller
public class MyFinesController {

    @FXML private Label totalFineAmount;
    @FXML private Label paidFineAmount;
    @FXML private Label pendingFineAmount;
    @FXML private Label overdueBooksCount;

    @FXML private TableView<Borrow> currentFinesTable;
    @FXML private TableColumn<Borrow, String> fineBookColumn;
    @FXML private TableColumn<Borrow, LocalDate> fineBorrowDateColumn;
    @FXML private TableColumn<Borrow, LocalDate> fineDueDateColumn;
    @FXML private TableColumn<Borrow, String> lateDaysColumn;
    @FXML private TableColumn<Borrow, String> rateColumn;
    @FXML private TableColumn<Borrow, String> fineTotalColumn;
    @FXML private TableColumn<Borrow, String> payActionColumn;

    @FXML private TableView<Payment> paymentHistoryTable;
    @FXML private TableColumn<Payment, String> paymentDateColumn;
    @FXML private TableColumn<Payment, String> paymentAmountColumn;
    @FXML private TableColumn<Payment, String> paymentMethodColumn;
    @FXML private TableColumn<Payment, String> paymentRefColumn;
    @FXML private TableColumn<Payment, String> paymentStatusColumn;

    private final UserService userService;
    private final BorrowService borrowService;

    private User currentUser;
    private ObservableList<Borrow> overdueBorrowings = FXCollections.observableArrayList();
    private ObservableList<Payment> paymentHistory = FXCollections.observableArrayList();

    // Payment record class
    public static class Payment {
        private LocalDate date;
        private double amount;
        private String method;
        private String reference;
        private String status;

        public Payment(LocalDate date, double amount, String method, String reference, String status) {
            this.date = date;
            this.amount = amount;
            this.method = method;
            this.reference = reference;
            this.status = status;
        }

        public LocalDate getDate() { return date; }
        public double getAmount() { return amount; }
        public String getMethod() { return method; }
        public String getReference() { return reference; }
        public String getStatus() { return status; }
    }

    public MyFinesController(UserService userService, BorrowService borrowService) {
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
        loadData();
        updateSummary();
        loadPaymentHistory();
    }

    // private void setupTables() {
    //     // Current fines table
    //     fineBookColumn.setCellValueFactory(cellData ->
    //         new SimpleStringProperty(cellData.getValue().getBook().getTitle()));
    //     fineBorrowDateColumn.setCellValueFactory(new PropertyValueFactory<>("borrowDate"));
    //     fineDueDateColumn.setCellValueFactory(new PropertyValueFactory<>("dueDate"));
    //     lateDaysColumn.setCellValueFactory(cellData -> {
    //         long days = ChronoUnit.DAYS.between(cellData.getValue().getDueDate(), LocalDate.now());
    //         return new SimpleStringProperty(String.valueOf(Math.max(0, days)));
    //     });
    //     rateColumn.setCellValueFactory(cellData ->
    //         new SimpleStringProperty("0.50 €/jour")); // Fixed rate
    //     fineTotalColumn.setCellValueFactory(cellData ->
    //         new SimpleStringProperty(String.format("%.2f €", cellData.getValue().getFineAmount())));

    //     // Pay action column
    //     payActionColumn.setCellFactory(col -> new TableCell<>() {
    //         private final Button payButton = new Button("Payer");

    //         @Override
    //         protected void updateItem(Borrow borrow, boolean empty) {
    //             super.updateItem(borrow, empty);
    //             if (empty || borrow == null) {
    //                 setGraphic(null);
    //             } else {
    //                 payButton.setOnAction(e -> payFine(borrow));
    //                 setGraphic(payButton);
    //             }
    //         }
    //     });

    //     // Payment history table
    //     paymentDateColumn.setCellValueFactory(new PropertyValueFactory<>("date"));
    //     paymentAmountColumn.setCellValueFactory(cellData ->
    //         new SimpleStringProperty(String.format("%.2f €", cellData.getValue().getAmount())));
    //     paymentMethodColumn.setCellValueFactory(new PropertyValueFactory<>("method"));
    //     paymentRefColumn.setCellValueFactory(new PropertyValueFactory<>("reference"));
    //     paymentStatusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
    // }

    private void loadData() {
        List<Borrow> memberBorrowings = borrowService.getBorrowsByMember(currentUser.getId());

        overdueBorrowings.setAll(memberBorrowings.stream()
            .filter(borrow -> !borrow.isReturned() &&
                     borrow.getDueDate().isBefore(LocalDate.now()))
            .toList());

        currentFinesTable.getItems().setAll(overdueBorrowings);
    }

    private void loadPaymentHistory() {
        // Mock payment history - in real app, this would come from a PaymentService
        paymentHistory.add(new Payment(LocalDate.now().minusDays(15), 5.50, "Carte", "PAY-001", "Payé"));
        paymentHistory.add(new Payment(LocalDate.now().minusDays(30), 3.00, "Espèces", "PAY-002", "Payé"));
        paymentHistory.add(new Payment(LocalDate.now().minusDays(45), 2.50, "Carte", "PAY-003", "Payé"));

        paymentHistoryTable.getItems().setAll(paymentHistory);
    }

    private void updateSummary() {
        double totalFine = overdueBorrowings.stream()
            .mapToDouble(Borrow::getFineAmount)
            .sum();

        double paidAmount = paymentHistory.stream()
            .mapToDouble(Payment::getAmount)
            .sum();

        double pendingAmount = totalFine - paidAmount;

        totalFineAmount.setText(String.format("%.2f €", totalFine));
        paidFineAmount.setText(String.format("%.2f €", paidAmount));
        pendingFineAmount.setText(String.format("%.2f €", Math.max(0, pendingAmount)));
        overdueBooksCount.setText(String.valueOf(overdueBorrowings.size()));
    }

    private void payFine(Borrow borrow) {
        double amount = borrow.getFineAmount();

        // Show payment dialog
        ChoiceDialog<String> methodDialog = new ChoiceDialog<>("Carte", "Carte", "Espèces", "Chèque");
        methodDialog.setTitle("Payer l'amende");
        methodDialog.setHeaderText("Payer l'amende pour '" + borrow.getBook().getTitle() + "'");
        methodDialog.setContentText("Sélectionnez la méthode de paiement:");

        methodDialog.showAndWait().ifPresent(method -> {
            if (method != null) {
                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                confirm.setTitle("Confirmation de paiement");
                confirm.setHeaderText(String.format("Payer %.2f € par %s ?", amount, method));
                confirm.setContentText("Cette action est irréversible.");

                confirm.showAndWait().ifPresent(response -> {
                    if (response == ButtonType.OK) {
                        // Record payment
                        String ref = "PAY-" + String.format("%03d", paymentHistory.size() + 1);
                        paymentHistory.add(new Payment(
                            LocalDate.now(),
                            amount,
                            method,
                            ref,
                            "Payé"
                        ));

                        // Update tables
                        paymentHistoryTable.getItems().setAll(paymentHistory);
                        updateSummary();

                        showSuccess("Paiement réussi",
                            String.format("Paiement de %.2f € enregistré. Référence: %s", amount, ref));
                    }
                });
            }
        });
    }

    @FXML
    private void payAllFines() {
        double totalAmount = overdueBorrowings.stream()
            .mapToDouble(Borrow::getFineAmount)
            .sum();

        if (totalAmount <= 0) {
            showInfo("Information", "Vous n'avez pas d'amendes à payer.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Payer toutes les amendes");
        confirm.setHeaderText(String.format("Payer le montant total de %.2f € ?", totalAmount));
        confirm.setContentText("Cette action paiera toutes vos amendes en attente.");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                // Record payment for all fines
                String ref = "PAY-ALL-" + DateTimeFormatter.ofPattern("yyyyMMdd").format(LocalDate.now());
                paymentHistory.add(new Payment(
                    LocalDate.now(),
                    totalAmount,
                    "Carte",
                    ref,
                    "Payé"
                ));

                // Update tables
                paymentHistoryTable.getItems().setAll(paymentHistory);
                updateSummary();

                showSuccess("Paiement réussi",
                    String.format("Paiement total de %.2f € enregistré. Référence: %s", totalAmount, ref));
            }
        });
    }

    @FXML
    private void printReceipt() {
        double totalAmount = paymentHistory.stream()
            .filter(p -> p.getStatus().equals("Payé"))
            .mapToDouble(Payment::getAmount)
            .sum();

        Alert info = new Alert(Alert.AlertType.INFORMATION);
        info.setTitle("Reçu de paiement");
        info.setHeaderText("Reçu des paiements");
        info.setContentText(String.format(
            "Total des paiements: %.2f €\n" +
            "Nombre de transactions: %d\n" +
            "Dernier paiement: %s",
            totalAmount,
            paymentHistory.size(),
            paymentHistory.isEmpty() ? "Aucun" :
                DateTimeFormatter.ofPattern("dd/MM/yyyy").format(paymentHistory.get(paymentHistory.size()-1).getDate())
        ));
        info.showAndWait();
    }

    @FXML
    private void refreshData() {
        loadData();
        updateSummary();
    }

    @FXML
    private void goBack() {
        try {
            Stage stage = (Stage) totalFineAmount.getScene().getWindow();
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

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}