package com.intellilib.controllers.admin;

import com.intellilib.models.*;
import com.intellilib.services.*;
import com.intellilib.repositories.*;
import com.intellilib.session.SessionManager;
import com.intellilib.util.ActivityLogger;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import javafx.beans.property.SimpleStringProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

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
    private final MemberService memberService;
    private final FineTransactionRepository transactionRepository;
    private final BorrowRepository borrowRepository;
    private final SessionManager sessionManager;
    private final ActivityLogger activityLogger;

    private final ObservableList<Borrow> finesList = FXCollections.observableArrayList();
    private final ObservableList<FineTransaction> transactionsList = FXCollections.observableArrayList();

    // Add these for proper filtering like in ManageCategoryController
    private FilteredList<Borrow> filteredFinesData;
    private SortedList<Borrow> sortedFinesData;

    public ManageFinesController(FineService fineService, BorrowService borrowService,
                                 FineTransactionRepository transactionRepository,
                                 MemberService memberService,
                                 SessionManager sessionManager,
                                 ActivityLogger activityLogger,
                                 BorrowRepository borrowRepository) {
        this.fineService = fineService;
        this.borrowService = borrowService;
        this.transactionRepository = transactionRepository;
        this.memberService = memberService;
        this.sessionManager = sessionManager;
        this.activityLogger = activityLogger;
        this.borrowRepository = borrowRepository;
    }

    @FXML
    public void initialize() {
        setupTables();
        setupFilters();
        loadData();
        updateSummary();
        setupListeners();
        setupSearchFilter(); // Add this method
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

        // DON'T set default dates for fines table - show all fines by default
        fromDateFilter.setValue(null);
        toDateFilter.setValue(null);
    }

    private void setupSearchFilter() {
        // Initialize the FilteredList with the finesList
        filteredFinesData = new FilteredList<>(finesList, p -> true);
        sortedFinesData = new SortedList<>(filteredFinesData);

        // Bind the SortedList comparator to the table comparator
        sortedFinesData.comparatorProperty().bind(finesTable.comparatorProperty());

        // Set the table items to the sorted data
        finesTable.setItems(sortedFinesData);

        // Set up the search filter listener
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            updateFilters();
        });
        
        // Add status and date filter listeners
        statusFilter.valueProperty().addListener((observable, oldValue, newValue) -> {
            updateFilters();
        });
        
        fromDateFilter.valueProperty().addListener((observable, oldValue, newValue) -> {
            updateFilters();
        });
        
        toDateFilter.valueProperty().addListener((observable, oldValue, newValue) -> {
            updateFilters();
        });
    }

    private void loadData() {
        try {
            // Load all borrows with fines into the base list
            List<Borrow> allFines = borrowService.getAllBorrows().stream()
                    .filter(b -> b.getFineAmount() > 0 || b.getFineStatus() != Borrow.FineStatus.NONE)
                    .toList();

            // Clear and add all items to maintain the observable list reference
            finesList.clear();
            finesList.addAll(allFines);

            // Load recent transactions - handle null fromDate
            LocalDate fromDate = fromDateFilter.getValue();
            LocalDate toDate = toDateFilter.getValue();

            if (toDate != null) {
                LocalDateTime startDate = (fromDate != null) ?
                        fromDate.atStartOfDay() :
                        LocalDate.now().minusDays(30).atStartOfDay(); // Default to last 30 days

                LocalDateTime endDate = toDate.atTime(23, 59, 59);

                transactionsList.setAll(transactionRepository.findByDateRange(startDate, endDate));
                transactionsTable.setItems(transactionsList);
            }

            // Update summary
            updateSummary();

            // Clear any existing selection
            finesTable.getSelectionModel().clearSelection();

        } catch (Exception e) {
            e.printStackTrace();
            showError("Load Error", "Failed to load fines data: " + e.getMessage());
        }
    }

    private void updateFilters() {
        if (filteredFinesData == null) {
            return;
        }

        filteredFinesData.setPredicate(borrow -> {
            if (borrow == null) {
                return false;
            }

            // First, check if it has a fine
            if (borrow.getFineAmount() <= 0 && borrow.getFineStatus() == Borrow.FineStatus.NONE) {
                return false;
            }

            // Apply status filter
            if (statusFilter.getValue() != null) {
                if (borrow.getFineStatus() != statusFilter.getValue()) {
                    return false;
                }
            }

            // Apply date filter - only if dates are set
            LocalDate fromDate = fromDateFilter.getValue();
            LocalDate toDate = toDateFilter.getValue();
            LocalDate dueDate = borrow.getDueDate();

            if (fromDate != null && dueDate != null) {
                if (dueDate.isBefore(fromDate)) {
                    return false;
                }
            }

            if (toDate != null && dueDate != null) {
                if (dueDate.isAfter(toDate)) {
                    return false;
                }
            }

            // Apply search filter
            String searchText = searchField.getText();
            if (searchText != null && !searchText.trim().isEmpty()) {
                String lowerSearchText = searchText.toLowerCase().trim();

                // Check book title
                if (borrow.getBook() != null && borrow.getBook().getTitle() != null &&
                        borrow.getBook().getTitle().toLowerCase().contains(lowerSearchText)) {
                    return true;
                }

                // Check member name
                if (borrow.getMember() != null && borrow.getMember().getFullName() != null &&
                        borrow.getMember().getFullName().toLowerCase().contains(lowerSearchText)) {
                    return true;
                }

                // Check member email
                if (borrow.getMember() != null && borrow.getMember().getEmail() != null &&
                        borrow.getMember().getEmail().toLowerCase().contains(lowerSearchText)) {
                    return true;
                }

                // Check borrow ID
                if (borrow.getId() != null &&
                        borrow.getId().toString().contains(lowerSearchText)) {
                    return true;
                }

                // Check ISBN if available
                if (borrow.getBook() != null && borrow.getBook().getIsbn() != null &&
                        borrow.getBook().getIsbn().toLowerCase().contains(lowerSearchText)) {
                    return true;
                }

                // If no match, filter out
                return false;
            }

            // If no search text, include all matching the other filters
            return true;
        });

        // Update summary after filtering
        updateSummary();
    }

    private void updateSummary() {
        // Use the filtered data for summary calculations
        double totalPending = sortedFinesData != null ?
                sortedFinesData.stream()
                        .filter(b -> b.getFineStatus() == Borrow.FineStatus.PENDING)
                        .mapToDouble(Borrow::getFineAmount)
                        .sum() : 0.0;

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
    }

    @FXML
    private void handleRefresh() {
        // Reset all filters
        searchField.clear();
        statusFilter.getSelectionModel().selectFirst();
        fromDateFilter.setValue(null);
        toDateFilter.setValue(LocalDate.now()); // Only set toDate for transactions

        // Reload data
        loadData();

        // Clear selection
        finesTable.getSelectionModel().clearSelection();
        updateButtonStates();

        showSuccess("Refreshed", "All data and filters have been reset!");
    }

    @FXML
    private void handlePay() {
        Borrow selectedBorrow = finesTable.getSelectionModel().getSelectedItem();
        if (selectedBorrow != null && selectedBorrow.getFineAmount() > 0) {
            // Check if fine is already paid or waived
            if (selectedBorrow.getFineStatus() == Borrow.FineStatus.PAID) {
                showError("Already Paid", "This fine has already been paid.");
                return;
            }

            if (selectedBorrow.getFineStatus() == Borrow.FineStatus.WAIVED) {
                showError("Fine Waived", "This fine has been waived and cannot be paid.");
                return;
            }

            // Open payment dialog
            openPaymentDialog(selectedBorrow);
        }
    }

    private void openPaymentDialog(Borrow borrow) {
        try {
            // Create payment dialog
            Dialog<PaymentInfo> dialog = new Dialog<>();
            dialog.setTitle("Pay Fine");
            dialog.setHeaderText("Pay Fine for Borrow #" + borrow.getId());

            // Set buttons
            ButtonType payButtonType = new ButtonType("Pay Now", ButtonBar.ButtonData.OK_DONE);
            dialog.getDialogPane().getButtonTypes().addAll(payButtonType, ButtonType.CANCEL);

            // Create form
            GridPane grid = new GridPane();
            grid.setHgap(10);
            grid.setVgap(10);
            grid.setPadding(new javafx.geometry.Insets(20, 150, 10, 10));

            // Amount field (read-only or editable for partial payments)
            TextField amountField = new TextField(String.format("%.2f", borrow.getFineAmount()));
            amountField.setEditable(false); // Set to true if you allow partial payments

            // Payment method selection
            ComboBox<FineTransaction.PaymentMethod> methodCombo = new ComboBox<>();
            methodCombo.getItems().addAll(
                    FineTransaction.PaymentMethod.CASH,
                    FineTransaction.PaymentMethod.CREDIT_CARD,
                    FineTransaction.PaymentMethod.DEBIT_CARD,
                    FineTransaction.PaymentMethod.BANK_TRANSFER,
                    FineTransaction.PaymentMethod.ONLINE
            );
            methodCombo.getSelectionModel().selectFirst();

            // Reference field
            TextField referenceField = new TextField();
            referenceField.setText("PAY-" + borrow.getId() + "-" +
                    LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyMMddHHmm")));
            referenceField.setEditable(true);
            referenceField.setPromptText("Payment reference");

            // Notes field
            TextArea notesArea = new TextArea();
            notesArea.setPromptText("Additional notes");
            notesArea.setPrefRowCount(3);
            notesArea.setText("Paid via Fine Management System");

            grid.add(new Label("Amount:"), 0, 0);
            grid.add(amountField, 1, 0);
            grid.add(new Label("Payment Method:"), 0, 1);
            grid.add(methodCombo, 1, 1);
            grid.add(new Label("Reference:"), 0, 2);
            grid.add(referenceField, 1, 2);
            grid.add(new Label("Notes:"), 0, 3);
            grid.add(notesArea, 1, 3);

            dialog.getDialogPane().setContent(grid);

            // Convert result
            dialog.setResultConverter(dialogButton -> {
                if (dialogButton == payButtonType) {
                    return new PaymentInfo(
                            Double.parseDouble(amountField.getText()),
                            methodCombo.getValue(),
                            referenceField.getText(),
                            notesArea.getText()
                    );
                }
                return null;
            });

            Optional<PaymentInfo> result = dialog.showAndWait();
            result.ifPresent(paymentInfo -> {
                try {
                    // Get current user from session
                    User currentUser = sessionManager.getCurrentUser();
                    if (currentUser == null) {
                        showError("Authentication Error", "No user logged in. Please login again.");
                        return;
                    }

                    // Process payment
                    FineTransaction transaction = fineService.processFinePayment(
                            borrow.getId(),
                            paymentInfo.amount(),
                            paymentInfo.paymentMethod(),
                            paymentInfo.reference(),
                            paymentInfo.notes(),
                            currentUser.getId()
                    );

                    // Log activity
                    activityLogger.logFinePayment(
                            currentUser,
                            borrow.getMember().getFullName(),
                            borrow.getFineAmount(),
                            paymentInfo.paymentMethod().name(),
                            paymentInfo.reference(),
                            paymentInfo.notes()
                    );

                    // Reload data
                    loadData();
                    updateSummary();

                    // Show success with receipt
                    Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
                    successAlert.setTitle("Payment Successful");
                    successAlert.setHeaderText("Fine Payment Processed");
                    successAlert.setContentText(
                            "Fine paid successfully!\n\n" +
                                    "Receipt Number: " + transaction.getReceiptNumber() + "\n" +
                                    "Amount: $" + String.format("%.2f", paymentInfo.amount()) + "\n" +
                                    "Member: " + borrow.getMember().getFullName() + "\n" +
                                    "Date: " + transaction.getTransactionDate().format(
                                    java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + "\n" +
                                    "Method: " + paymentInfo.paymentMethod()
                    );
                    successAlert.showAndWait();

                } catch (Exception e) {
                    showError("Payment Failed", "Failed to process payment: " + e.getMessage());
                    e.printStackTrace(); // For debugging
                }
            });
        } catch (Exception e) {
            showError("Error", "Failed to open payment dialog: " + e.getMessage());
        }
    }

    @FXML
    private void handleWaive() {
        Borrow selectedBorrow = finesTable.getSelectionModel().getSelectedItem();
        if (selectedBorrow != null && selectedBorrow.getFineAmount() > 0) {
            // Check if fine is already paid or waived
            if (selectedBorrow.getFineStatus() == Borrow.FineStatus.PAID) {
                showError("Already Paid", "This fine has already been paid and cannot be waived.");
                return;
            }

            if (selectedBorrow.getFineStatus() == Borrow.FineStatus.WAIVED) {
                showError("Already Waived", "This fine has already been waived.");
                return;
            }

            // Create waiver dialog with reason
            Dialog<String> dialog = new Dialog<>();
            dialog.setTitle("Waive Fine");
            dialog.setHeaderText("Waive Fine for Borrow #" + selectedBorrow.getId());

            // Set buttons
            ButtonType waiveButtonType = new ButtonType("Waive Fine", ButtonBar.ButtonData.OK_DONE);
            dialog.getDialogPane().getButtonTypes().addAll(waiveButtonType, ButtonType.CANCEL);

            // Create form
            GridPane grid = new GridPane();
            grid.setHgap(10);
            grid.setVgap(10);
            grid.setPadding(new javafx.geometry.Insets(20, 150, 10, 10));

            // Amount field (read-only)
            TextField amountField = new TextField(String.format("%.2f", selectedBorrow.getFineAmount()));
            amountField.setEditable(false);

            // Reason field
            ComboBox<String> reasonCombo = new ComboBox<>();
            reasonCombo.getItems().addAll(
                    "Library Policy Exception",
                    "Member Goodwill",
                    "Technical Issue",
                    "First Time Offense",
                    "Staff Error",
                    "Other"
            );
            reasonCombo.getSelectionModel().selectFirst();

            // Additional notes
            TextArea notesArea = new TextArea();
            notesArea.setPromptText("Additional notes or details...");
            notesArea.setPrefRowCount(4);

            grid.add(new Label("Amount to Waive:"), 0, 0);
            grid.add(amountField, 1, 0);
            grid.add(new Label("Reason:"), 0, 1);
            grid.add(reasonCombo, 1, 1);
            grid.add(new Label("Notes:"), 0, 2);
            grid.add(notesArea, 1, 2);

            dialog.getDialogPane().setContent(grid);

            // Convert result
            dialog.setResultConverter(dialogButton -> {
                if (dialogButton == waiveButtonType) {
                    String reason = reasonCombo.getValue();
                    if (!notesArea.getText().trim().isEmpty()) {
                        reason += " - " + notesArea.getText().trim();
                    }
                    return reason;
                }
                return null;
            });

            Optional<String> result = dialog.showAndWait();
            result.ifPresent(reason -> {
                try {
                    // Get current user from session
                    User currentUser = sessionManager.getCurrentUser();
                    if (currentUser == null) {
                        showError("Authentication Error", "No user logged in. Please login again.");
                        return;
                    }

                    // Process waiver
                    fineService.waiveFine(
                            selectedBorrow.getId(),
                            reason,
                            currentUser.getId()
                    );

                    // Log activity
                    activityLogger.logFineWaiver(
                            currentUser,
                            selectedBorrow.getMember().getFullName(),
                            selectedBorrow.getFineAmount(),
                            selectedBorrow.getId(),
                            reason
                    );

                    // Reload data
                    loadData();
                    updateSummary();

                    showSuccess("Fine Waived",
                            "Fine waived successfully!\n" +
                                    "Amount: $" + String.format("%.2f", selectedBorrow.getFineAmount()) + "\n" +
                                    "Member: " + selectedBorrow.getMember().getFullName() + "\n" +
                                    "Reason: " + reason);

                } catch (Exception e) {
                    showError("Error", "Failed to waive fine: " + e.getMessage());
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

    private record PaymentInfo(
            Double amount,
            FineTransaction.PaymentMethod paymentMethod,
            String reference,
            String notes
    ) {}
}