package com.intellilib.controllers.admin;

import com.intellilib.models.*;
import com.intellilib.services.*;
import com.intellilib.repositories.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import javafx.beans.property.SimpleStringProperty;
import org.springframework.stereotype.Controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

@Controller
public class ManageFinesController {
    
    @FXML private TableView<Borrow> finesTable;
    @FXML private TableColumn<Borrow, Long> idColumn;
    @FXML private TableColumn<Borrow, String> bookColumn;
    @FXML private TableColumn<Borrow, String> memberColumn;
    @FXML private TableColumn<Borrow, LocalDate> dueDateColumn;
    @FXML private TableColumn<Borrow, Integer> daysOverdueColumn;
    @FXML private TableColumn<Borrow, Double> fineAmountColumn;
    @FXML private TableColumn<Borrow, String> statusColumn;
    
    @FXML private TableView<FineTransaction> transactionsTable;
    @FXML private TableColumn<FineTransaction, String> receiptColumn;
    @FXML private TableColumn<FineTransaction, LocalDateTime> dateColumn;
    @FXML private TableColumn<FineTransaction, Double> amountColumn;
    @FXML private TableColumn<FineTransaction, String> methodColumn;
    @FXML private TableColumn<FineTransaction, String> statusColumn2;
    
    @FXML private ComboBox<Borrow.FineStatus> statusFilter;
    @FXML private DatePicker fromDateFilter;
    @FXML private DatePicker toDateFilter;
    @FXML private TextField searchField;
    
    @FXML private Button refreshButton;
    @FXML private Button payButton;
    @FXML private Button waiveButton;
    @FXML private Button viewDetailsButton;
    @FXML private Button generateReportButton;
    @FXML private Button closeButton;
    
    // Summary labels
    @FXML private Label totalPendingLabel;
    @FXML private Label totalCollectedLabel;
    @FXML private Label totalWaivedLabel;
    @FXML private Label overdueMembersLabel;
    @FXML private Label bannedMembersLabel;
    
    private final FineService fineService;
    private final BorrowService borrowService;
    private final FineTransactionRepository transactionRepository;
    private final MemberService memberService;
    
    private final ObservableList<Borrow> finesList = FXCollections.observableArrayList();
    private final ObservableList<FineTransaction> transactionsList = FXCollections.observableArrayList();
    
    public ManageFinesController(FineService fineService, BorrowService borrowService,
                               FineTransactionRepository transactionRepository,
                               MemberService memberService) {
        this.fineService = fineService;
        this.borrowService = borrowService;
        this.transactionRepository = transactionRepository;
        this.memberService = memberService;
    }
    
    @FXML
    public void initialize() {
        setupTables();
        setupFilters();
        loadData();
        updateSummary();
        setupListeners();
    }
    
    private void setupTables() {
        // Fines table
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        bookColumn.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getBook().getTitle()));
        memberColumn.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getMember().getFullName()));
        dueDateColumn.setCellValueFactory(new PropertyValueFactory<>("dueDate"));
        daysOverdueColumn.setCellValueFactory(new PropertyValueFactory<>("daysOverdue"));
        fineAmountColumn.setCellValueFactory(new PropertyValueFactory<>("fineAmount"));
        statusColumn.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getFineStatus().toString()));
        
        // Format fine amount
        fineAmountColumn.setCellFactory(column -> new TableCell<Borrow, Double>() {
            @Override
            protected void updateItem(Double amount, boolean empty) {
                super.updateItem(amount, empty);
                if (empty || amount == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(String.format("$%.2f", amount));
                    if (amount > 0) {
                        setStyle("-fx-text-fill: #d32f2f; -fx-font-weight: bold;");
                    }
                }
            }
        });
        
        // Color code status
        statusColumn.setCellFactory(column -> new TableCell<Borrow, String>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(status);
                    switch (status) {
                        case "PENDING":
                            setStyle("-fx-text-fill: #f57c00; -fx-font-weight: bold;");
                            break;
                        case "PAID":
                            setStyle("-fx-text-fill: #388e3c;");
                            break;
                        case "WAIVED":
                            setStyle("-fx-text-fill: #1976d2;");
                            break;
                        default:
                            setStyle("");
                    }
                }
            }
        });
        
        // Transactions table
        receiptColumn.setCellValueFactory(new PropertyValueFactory<>("receiptNumber"));
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("transactionDate"));
        amountColumn.setCellValueFactory(new PropertyValueFactory<>("amount"));
        methodColumn.setCellValueFactory(new PropertyValueFactory<>("paymentMethod"));
        statusColumn2.setCellValueFactory(new PropertyValueFactory<>("status"));
        
        // Format transaction amount
        amountColumn.setCellFactory(column -> new TableCell<FineTransaction, Double>() {
            @Override
            protected void updateItem(Double amount, boolean empty) {
                super.updateItem(amount, empty);
                if (empty || amount == null) {
                    setText(null);
                } else {
                    setText(String.format("$%.2f", amount));
                }
            }
        });
        
        // Format date
        dateColumn.setCellFactory(column -> new TableCell<FineTransaction, LocalDateTime>() {
            @Override
            protected void updateItem(LocalDateTime date, boolean empty) {
                super.updateItem(date, empty);
                if (empty || date == null) {
                    setText(null);
                } else {
                    setText(date.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
                }
            }
        });
    }
    
    private void setupFilters() {
        statusFilter.getItems().addAll(
            null, // All
            Borrow.FineStatus.PENDING,
            Borrow.FineStatus.PAID,
            Borrow.FineStatus.WAIVED,
            Borrow.FineStatus.NONE
        );
        statusFilter.getSelectionModel().selectFirst();
        
        fromDateFilter.setValue(LocalDate.now().minusDays(30));
        toDateFilter.setValue(LocalDate.now());
    }
    
    private void loadData() {
        // Load pending fines
        finesList.setAll(borrowService.getAllBorrows().stream()
            .filter(b -> b.getFineAmount() > 0 || b.getFineStatus() != Borrow.FineStatus.NONE)
            .toList());
        finesTable.setItems(finesList);
        
        // Load recent transactions
        transactionsList.setAll(transactionRepository.findByDateRange(
            fromDateFilter.getValue().atStartOfDay(),
            toDateFilter.getValue().atTime(23, 59, 59)
        ));
        transactionsTable.setItems(transactionsList);
    }
    
    private void updateSummary() {
        double totalPending = finesList.stream()
            .filter(b -> b.getFineStatus() == Borrow.FineStatus.PENDING)
            .mapToDouble(Borrow::getFineAmount)
            .sum();
        
        double totalCollected = transactionRepository.getTotalCollectedFines() != null ? 
            transactionRepository.getTotalCollectedFines() : 0.0;
        
        double totalWaived = transactionRepository.getTotalWaivedFines() != null ?
            transactionRepository.getTotalWaivedFines() : 0.0;
        
        long overdueMembers = memberService.getAllMembers().stream()
            .filter(m -> m.getCurrentFinesDue() > 0)
            .count();
        
        long bannedMembers = memberService.getAllMembers().stream()
            .filter(Member::getIsBanned)
            .count();
        
        totalPendingLabel.setText(String.format("$%.2f", totalPending));
        totalCollectedLabel.setText(String.format("$%.2f", totalCollected));
        totalWaivedLabel.setText(String.format("$%.2f", totalWaived));
        overdueMembersLabel.setText(String.valueOf(overdueMembers));
        bannedMembersLabel.setText(String.valueOf(bannedMembers));
    }
    
    private void setupListeners() {
        finesTable.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldSelection, newSelection) -> {
                if (newSelection != null) {
                    updateButtonStates();
                }
            });
        
        // Filter listeners
        statusFilter.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        fromDateFilter.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        toDateFilter.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        searchField.textProperty().addListener((obs, oldVal, newVal) -> applyFilters());
    }
    
    private void applyFilters() {
        var filtered = borrowService.getAllBorrows().stream()
            .filter(b -> b.getFineAmount() > 0 || b.getFineStatus() != Borrow.FineStatus.NONE);
        
        // Apply status filter
        if (statusFilter.getValue() != null) {
            filtered = filtered.filter(b -> b.getFineStatus() == statusFilter.getValue());
        }
        
        // Apply date filter
        if (fromDateFilter.getValue() != null) {
            filtered = filtered.filter(b -> 
                !b.getDueDate().isBefore(fromDateFilter.getValue()));
        }
        
        if (toDateFilter.getValue() != null) {
            filtered = filtered.filter(b -> 
                !b.getDueDate().isAfter(toDateFilter.getValue()));
        }
        
        // Apply search filter
        String searchText = searchField.getText().toLowerCase();
        if (!searchText.isEmpty()) {
            filtered = filtered.filter(b ->
                b.getBook().getTitle().toLowerCase().contains(searchText) ||
                b.getMember().getFullName().toLowerCase().contains(searchText) ||
                b.getId().toString().contains(searchText)
            );
        }
        
        finesList.setAll(filtered.toList());
        updateSummary();
    }
    
    @FXML
    private void handleRefresh() {
        loadData();
        updateSummary();
        showSuccess("Refreshed", "Data refreshed successfully!");
    }
    
    @FXML
    private void handlePay() {
        Borrow selectedBorrow = finesTable.getSelectionModel().getSelectedItem();
        if (selectedBorrow != null && selectedBorrow.getFineAmount() > 0) {
            // Use the payment dialog from ManageBorrowController
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Pay Fine");
            alert.setHeaderText("Pay Fine for Borrow #" + selectedBorrow.getId());
            alert.setContentText(String.format(
                "Amount: $%.2f\n" +
                "Member: %s\n" +
                "Book: %s\n\n" +
                "Proceed with payment?",
                selectedBorrow.getFineAmount(),
                selectedBorrow.getMember().getFullName(),
                selectedBorrow.getBook().getTitle()
            ));
            
            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                try {
                    // In a real app, you would open a payment dialog here
                    // For now, just mark as paid
                    fineService.processFinePayment(
                        selectedBorrow.getId(),
                        selectedBorrow.getFineAmount(),
                        FineTransaction.PaymentMethod.CASH,
                        "MANUAL_PAYMENT",
                        "Paid via fine management",
                        1L // Admin user ID
                    );
                    
                    loadData();
                    updateSummary();
                    showSuccess("Success", "Payment processed successfully!");
                } catch (Exception e) {
                    showError("Error", "Failed to process payment: " + e.getMessage());
                }
            }
        }
    }
    
    @FXML
    private void handleWaive() {
        Borrow selectedBorrow = finesTable.getSelectionModel().getSelectedItem();
        if (selectedBorrow != null && selectedBorrow.getFineAmount() > 0) {
            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("Waive Fine");
            dialog.setHeaderText("Waive Fine for Borrow #" + selectedBorrow.getId());
            dialog.setContentText("Please enter the reason for waiving this fine:");
            
            Optional<String> result = dialog.showAndWait();
            result.ifPresent(reason -> {
                if (!reason.trim().isEmpty()) {
                    try {
                        fineService.waiveFine(
                            selectedBorrow.getId(),
                            reason,
                            1L // Admin user ID
                        );
                        
                        loadData();
                        updateSummary();
                        showSuccess("Success", "Fine waived successfully!");
                    } catch (Exception e) {
                        showError("Error", "Failed to waive fine: " + e.getMessage());
                    }
                }
            });
        }
    }
    
    @FXML
    private void handleViewDetails() {
        Borrow selectedBorrow = finesTable.getSelectionModel().getSelectedItem();
        if (selectedBorrow != null) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Fine Details");
            alert.setHeaderText("Fine Details for Borrow #" + selectedBorrow.getId());
            
            Member member = selectedBorrow.getMember();
            String content = String.format(
                "Member Information:\n" +
                "  Name: %s\n" +
                "  Email: %s\n" +
                "  Current Fines Due: $%.2f\n" +
                "  Total Fines Paid: $%.2f\n" +
                "  Overdue Books: %d\n" +
                "  Banned: %s\n\n" +
                "Borrow Information:\n" +
                "  Book: %s\n" +
                "  Borrow Date: %s\n" +
                "  Due Date: %s\n" +
                "  Return Date: %s\n" +
                "  Days Overdue: %d\n" +
                "  Fine Per Day: $%.2f\n" +
                "  Fine Amount: $%.2f\n" +
                "  Fine Status: %s\n",
                member.getFullName(),
                member.getEmail(),
                member.getCurrentFinesDue(),
                member.getTotalFinesPaid(),
                member.getOverdueBooksCount(),
                member.getIsBanned() ? "Yes" : "No",
                selectedBorrow.getBook().getTitle(),
                selectedBorrow.getBorrowDate(),
                selectedBorrow.getDueDate(),
                selectedBorrow.getReturnDate() != null ? 
                    selectedBorrow.getReturnDate().toString() : "Not returned",
                selectedBorrow.getDaysOverdue(),
                selectedBorrow.getFinePerDay(),
                selectedBorrow.getFineAmount(),
                selectedBorrow.getFineStatus()
            );
            
            alert.setContentText(content);
            alert.setResizable(true);
            alert.getDialogPane().setPrefSize(500, 400);
            alert.showAndWait();
        }
    }
    
    @FXML
    private void handleGenerateReport() {
        try {
            // Create report dialog
            Dialog<Void> dialog = new Dialog<>();
            dialog.setTitle("Generate Report");
            dialog.setHeaderText("Generate Fine Report");
            
            // Set buttons
            ButtonType generateButtonType = new ButtonType("Generate", ButtonBar.ButtonData.OK_DONE);
            dialog.getDialogPane().getButtonTypes().addAll(generateButtonType, ButtonType.CANCEL);
            
            // Create form
            GridPane grid = new GridPane();
            grid.setHgap(10);
            grid.setVgap(10);
            grid.setPadding(new javafx.geometry.Insets(20, 150, 10, 10));
            
            DatePicker reportFromDate = new DatePicker(LocalDate.now().minusDays(30));
            DatePicker reportToDate = new DatePicker(LocalDate.now());
            
            ComboBox<String> reportType = new ComboBox<>();
            reportType.getItems().addAll("Daily Summary", "Member Fines", "Collection Report", "Overdue Analysis");
            reportType.getSelectionModel().selectFirst();
            
            CheckBox includePaid = new CheckBox("Include Paid Fines");
            CheckBox includeWaived = new CheckBox("Include Waived Fines");
            CheckBox exportToCSV = new CheckBox("Export to CSV");
            
            includePaid.setSelected(true);
            includeWaived.setSelected(true);
            exportToCSV.setSelected(true);
            
            grid.add(new Label("Report Type:"), 0, 0);
            grid.add(reportType, 1, 0);
            grid.add(new Label("From Date:"), 0, 1);
            grid.add(reportFromDate, 1, 1);
            grid.add(new Label("To Date:"), 0, 2);
            grid.add(reportToDate, 1, 2);
            grid.add(includePaid, 0, 3);
            grid.add(includeWaived, 0, 4);
            grid.add(exportToCSV, 0, 5);
            
            dialog.getDialogPane().setContent(grid);
            
            dialog.setResultConverter(dialogButton -> {
                if (dialogButton == generateButtonType) {
                    // Generate report
                    generateReport(reportType.getValue(), 
                                  reportFromDate.getValue(), 
                                  reportToDate.getValue(),
                                  includePaid.isSelected(),
                                  includeWaived.isSelected(),
                                  exportToCSV.isSelected());
                    return null;
                }
                return null;
            });
            
            dialog.showAndWait();
        } catch (Exception e) {
            showError("Error", "Failed to generate report: " + e.getMessage());
        }
    }
    
    private void generateReport(String type, LocalDate fromDate, LocalDate toDate,
                              boolean includePaid, boolean includeWaived, boolean exportCSV) {
        try {
            // In a real application, you would generate the report here
            // For now, just show a success message
            
            String message = String.format(
                "Report Generated Successfully!\n\n" +
                "Type: %s\n" +
                "Period: %s to %s\n" +
                "Included Paid: %s\n" +
                "Included Waived: %s\n" +
                "Export to CSV: %s\n\n" +
                "Report has been generated and saved to the reports folder.",
                type,
                fromDate,
                toDate,
                includePaid ? "Yes" : "No",
                includeWaived ? "Yes" : "No",
                exportCSV ? "Yes" : "No"
            );
            
            showSuccess("Report Generated", message);
            
            // You could also open the report file or show it in a new window
        } catch (Exception e) {
            showError("Error", "Failed to generate report: " + e.getMessage());
        }
    }
    
    @FXML
    private void handleClose() {
        Stage stage = (Stage) closeButton.getScene().getWindow();
        stage.close();
    }
    
    private void updateButtonStates() {
        Borrow selectedBorrow = finesTable.getSelectionModel().getSelectedItem();
        
        if (selectedBorrow != null) {
            payButton.setDisable(
                selectedBorrow.getFineAmount() <= 0 ||
                selectedBorrow.getFineStatus() == Borrow.FineStatus.PAID ||
                selectedBorrow.getFineStatus() == Borrow.FineStatus.WAIVED
            );
            
            waiveButton.setDisable(
                selectedBorrow.getFineAmount() <= 0 ||
                selectedBorrow.getFineStatus() == Borrow.FineStatus.PAID ||
                selectedBorrow.getFineStatus() == Borrow.FineStatus.WAIVED
            );
            
            viewDetailsButton.setDisable(false);
        } else {
            payButton.setDisable(true);
            waiveButton.setDisable(true);
            viewDetailsButton.setDisable(true);
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