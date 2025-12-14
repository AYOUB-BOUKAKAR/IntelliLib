package com.intellilib.controllers;

import com.intellilib.models.*;
import com.intellilib.services.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import javafx.beans.property.SimpleStringProperty;
import org.springframework.stereotype.Controller;

import java.time.LocalDate;
import java.util.Optional;

@Controller
public class ManageBorrowController {
    @FXML private TableView<Borrow> borrowTable;
    @FXML private TableColumn<Borrow, Long> idColumn;
    @FXML private TableColumn<Borrow, String> bookTitleColumn;
    @FXML private TableColumn<Borrow, String> memberNameColumn;
    @FXML private TableColumn<Borrow, LocalDate> borrowDateColumn;
    @FXML private TableColumn<Borrow, LocalDate> dueDateColumn;
    @FXML private TableColumn<Borrow, LocalDate> returnDateColumn;
    @FXML private TableColumn<Borrow, Boolean> returnedColumn;
    
    // New columns for fine management
    @FXML private TableColumn<Borrow, Double> fineAmountColumn;
    @FXML private TableColumn<Borrow, Integer> daysOverdueColumn;
    @FXML private TableColumn<Borrow, String> fineStatusColumn;
    
    @FXML private ComboBox<Book> bookCombo;
    @FXML private ComboBox<Member> memberCombo;
    @FXML private DatePicker dueDatePicker;
    
    @FXML private Button saveButton;
    @FXML private Button cancelButton;
    @FXML private Button returnButton;
    @FXML private Button viewFineButton;
    @FXML private Button payFineButton;
    
    private final BorrowService borrowService;
    private final BookService bookService;
    private final MemberService memberService;
    private final FineService fineService;
    
    private final ObservableList<Borrow> borrowList = FXCollections.observableArrayList();
    private final ObservableList<Book> availableBooks = FXCollections.observableArrayList();
    private final ObservableList<Member> activeMembers = FXCollections.observableArrayList();
    
    private Borrow currentBorrow;
    
    public ManageBorrowController(BorrowService borrowService, BookService bookService, 
                                MemberService memberService, FineService fineService) {
        this.borrowService = borrowService;
        this.bookService = bookService;
        this.memberService = memberService;
        this.fineService = fineService;
    }
    
    @FXML
    public void initialize() {
        setupTableColumns();
        loadAvailableBooks();
        loadActiveMembers();
        loadBorrows();
        setupListeners();
        updateButtonStates();
    }
    
    private void setupTableColumns() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        bookTitleColumn.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getBook().getTitle()));
        memberNameColumn.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getMember().getFullName()));
        borrowDateColumn.setCellValueFactory(new PropertyValueFactory<>("borrowDate"));
        dueDateColumn.setCellValueFactory(new PropertyValueFactory<>("dueDate"));
        returnDateColumn.setCellValueFactory(new PropertyValueFactory<>("returnDate"));
        returnedColumn.setCellValueFactory(new PropertyValueFactory<>("returned"));
        
        // Fine columns
        fineAmountColumn.setCellValueFactory(new PropertyValueFactory<>("fineAmount"));
        daysOverdueColumn.setCellValueFactory(new PropertyValueFactory<>("daysOverdue"));
        fineStatusColumn.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getFineStatus().toString()));
        
        // Add cell factory for fine amount (color coding)
        fineAmountColumn.setCellFactory(column -> new TableCell<Borrow, Double>() {
            @Override
            protected void updateItem(Double amount, boolean empty) {
                super.updateItem(amount, empty);
                
                if (empty || amount == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(String.format("$%.2f", amount));
                    
                    // Color code based on amount
                    if (amount > 0) {
                        setStyle("-fx-text-fill: #d32f2f; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-text-fill: #388e3c;");
                    }
                }
            }
        });
        
        // Color code for days overdue
        daysOverdueColumn.setCellFactory(column -> new TableCell<Borrow, Integer>() {
            @Override
            protected void updateItem(Integer days, boolean empty) {
                super.updateItem(days, empty);
                
                if (empty || days == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(days.toString());
                    
                    if (days > 30) {
                        setStyle("-fx-text-fill: #d32f2f; -fx-font-weight: bold;");
                    } else if (days > 7) {
                        setStyle("-fx-text-fill: #f57c00;");
                    } else if (days > 0) {
                        setStyle("-fx-text-fill: #ff9800;");
                    } else {
                        setStyle("");
                    }
                }
            }
        });
    }
    
    private void loadAvailableBooks() {
        availableBooks.setAll(bookService.findAvailableBooks());
        bookCombo.setItems(availableBooks);
        bookCombo.setCellFactory(lv -> new ListCell<Book>() {
            @Override
            protected void updateItem(Book book, boolean empty) {
                super.updateItem(book, empty);
                setText(empty ? "" : book.getTitle() + " by " + book.getAuthor());
            }
        });
        bookCombo.setButtonCell(new ListCell<Book>() {
            @Override
            protected void updateItem(Book book, boolean empty) {
                super.updateItem(book, empty);
                setText(empty ? "" : book.getTitle() + " by " + book.getAuthor());
            }
        });
    }
    
    private void loadActiveMembers() {
        // Only load members who can borrow (not banned, not exceeding credit limit)
        activeMembers.setAll(memberService.getActiveMembers().stream()
            .filter(Member::canBorrow)
            .toList());
        memberCombo.setItems(activeMembers);
        memberCombo.setCellFactory(lv -> new ListCell<Member>() {
            @Override
            protected void updateItem(Member member, boolean empty) {
                super.updateItem(member, empty);
                if (empty || member == null) {
                    setText(null);
                } else {
                    setText(member.getFullName() + " (" + member.getEmail() + ")");
                    
                    // Show warning for members with fines
                    if (member.getCurrentFinesDue() > 0) {
                        setStyle("-fx-text-fill: #f57c00;");
                    } else {
                        setStyle("");
                    }
                }
            }
        });
        memberCombo.setButtonCell(new ListCell<Member>() {
            @Override
            protected void updateItem(Member member, boolean empty) {
                super.updateItem(member, empty);
                if (empty || member == null) {
                    setText(null);
                } else {
                    setText(member.getFullName() + " (" + member.getEmail() + ")");
                }
            }
        });
    }
    
    private void loadBorrows() {
        borrowList.setAll(borrowService.getAllBorrows());
        borrowTable.setItems(borrowList);
        
        // Sort by due date (oldest first)
        borrowTable.getSortOrder().add(dueDateColumn);
        dueDateColumn.setSortType(TableColumn.SortType.ASCENDING);
        borrowTable.sort();
    }
    
    private void setupListeners() {
        borrowTable.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldSelection, newSelection) -> {
                if (newSelection != null) {
                    populateForm(newSelection);
                    updateButtonStates();
                }
            });
        
        // Listen to due date changes
        dueDatePicker.valueProperty().addListener((obs, oldDate, newDate) -> {
            if (newDate != null && newDate.isBefore(LocalDate.now())) {
                dueDatePicker.setStyle("-fx-border-color: #d32f2f;");
            } else {
                dueDatePicker.setStyle("");
            }
        });
    }
    
    private void populateForm(Borrow borrow) {
        currentBorrow = borrow;
        bookCombo.getSelectionModel().select(borrow.getBook());
        memberCombo.getSelectionModel().select(borrow.getMember());
        dueDatePicker.setValue(borrow.getDueDate());
    }
    
    @FXML
    private void handleSave() {
        if (validateInput()) {
            try {
                Borrow borrow = currentBorrow != null ? currentBorrow : new Borrow();
                borrow.setBook(bookCombo.getValue());
                borrow.setMember(memberCombo.getValue());
                borrow.setDueDate(dueDatePicker.getValue());
                
                if (currentBorrow == null) {
                    borrow.setBorrowDate(LocalDate.now());
                    borrow.setReturned(false);
                }
                
                borrowService.saveBorrow(borrow);
                clearForm();
                loadBorrows();
                
                showSuccess("Success", "Borrow saved successfully!");
            } catch (Exception e) {
                showError("Error", "Failed to save borrow: " + e.getMessage());
            }
        }
    }
    
    @FXML
    private void handleReturn() {
        Borrow selectedBorrow = borrowTable.getSelectionModel().getSelectedItem();
        if (selectedBorrow != null && !selectedBorrow.isReturned()) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Return Book");
            alert.setHeaderText("Return Confirmation");
            alert.setContentText("Are you sure you want to return '" + 
                selectedBorrow.getBook().getTitle() + "'?\n" +
                (selectedBorrow.isOverdue() ? 
                    "This book is overdue by " + selectedBorrow.getDaysOverdue() + " days.\n" +
                    "Fine amount: $" + selectedBorrow.getFineAmount() : 
                    "No fines applicable."));
            
            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                try {
                    // Calculate fine before returning
                    if (selectedBorrow.isOverdue()) {
                        fineService.updateFineForBorrow(selectedBorrow);
                    }
                    
                    selectedBorrow.setReturned(true);
                    selectedBorrow.setReturnDate(LocalDate.now());
                    borrowService.saveBorrow(selectedBorrow);
                    
                    // Update member's overdue count
                    Member member = selectedBorrow.getMember();
                    if (selectedBorrow.isOverdue()) {
                        member.setOverdueBooksCount(
                            Math.max(0, member.getOverdueBooksCount() - 1));
                        memberService.createMember(member);
                    }
                    
                    loadBorrows();
                    loadActiveMembers(); // Refresh member list
                    
                    showSuccess("Success", "Book returned successfully!" + 
                        (selectedBorrow.getFineAmount() > 0 ? 
                            "\nFine amount: $" + selectedBorrow.getFineAmount() : ""));
                } catch (Exception e) {
                    showError("Error", "Failed to return book: " + e.getMessage());
                }
            }
        }
    }
    
    @FXML
    private void handleViewFine() {
        Borrow selectedBorrow = borrowTable.getSelectionModel().getSelectedItem();
        if (selectedBorrow != null && selectedBorrow.getFineAmount() > 0) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Fine Details");
            alert.setHeaderText("Fine Information for Borrow #" + selectedBorrow.getId());
            
            String content = String.format(
                "Book: %s\n" +
                "Member: %s\n" +
                "Borrow Date: %s\n" +
                "Due Date: %s\n" +
                "Days Overdue: %d\n" +
                "Fine Per Day: $%.2f\n" +
                "Total Fine: $%.2f\n" +
                "Fine Status: %s\n\n" +
                "Calculation: %d days × $%.2f = $%.2f",
                selectedBorrow.getBook().getTitle(),
                selectedBorrow.getMember().getFullName(),
                selectedBorrow.getBorrowDate(),
                selectedBorrow.getDueDate(),
                selectedBorrow.getDaysOverdue(),
                selectedBorrow.getFinePerDay(),
                selectedBorrow.getFineAmount(),
                selectedBorrow.getFineStatus(),
                selectedBorrow.getDaysOverdue(),
                selectedBorrow.getFinePerDay(),
                selectedBorrow.getFineAmount()
            );
            
            alert.setContentText(content);
            alert.showAndWait();
        }
    }
    
    @FXML
    private void handlePayFine() {
        Borrow selectedBorrow = borrowTable.getSelectionModel().getSelectedItem();
        if (selectedBorrow != null && selectedBorrow.getFineAmount() > 0) {
            // Open fine payment dialog
            openFinePaymentDialog(selectedBorrow);
        }
    }
    
    @FXML
    private void handleDelete() {
        Borrow selectedBorrow = borrowTable.getSelectionModel().getSelectedItem();
        if (selectedBorrow != null) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Confirmation");
            alert.setHeaderText("Delete Borrow");
            alert.setContentText("Are you sure you want to delete this borrow record?\n" +
                               "This action cannot be undone.");
            
            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                try {
                    borrowService.deleteBorrow(selectedBorrow.getId());
                    loadBorrows();
                    clearForm();
                    
                    showSuccess("Success", "Borrow deleted successfully!");
                } catch (Exception e) {
                    showError("Error", "Failed to delete borrow: " + e.getMessage());
                }
            }
        }
    }
    
    @FXML
    private void handleCalculateFines() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Calculate Fines");
        alert.setHeaderText("Recalculate All Fines");
        alert.setContentText("This will recalculate fines for all overdue books.\n" +
                           "Do you want to proceed?");
        
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                // Get all overdue borrows
                var overdueBorrows = borrowService.getOverdueBorrows();
                int count = 0;
                
                for (Borrow borrow : overdueBorrows) {
                    fineService.updateFineForBorrow(borrow);
                    count++;
                }
                
                loadBorrows();
                showSuccess("Success", "Recalculated fines for " + count + " borrow(s)!");
            } catch (Exception e) {
                showError("Error", "Failed to calculate fines: " + e.getMessage());
            }
        }
    }
    
    @FXML
    private void handleCancel() {
        clearForm();
        Stage stage = (Stage) cancelButton.getScene().getWindow();
        stage.close();
    }
    
    private boolean validateInput() {
        StringBuilder errors = new StringBuilder();
        
        if (bookCombo.getValue() == null) {
            errors.append("• Please select a book\n");
        }
        
        if (memberCombo.getValue() == null) {
            errors.append("• Please select a member\n");
        }
        
        if (dueDatePicker.getValue() == null) {
            errors.append("• Please select a due date\n");
        } else if (dueDatePicker.getValue().isBefore(LocalDate.now())) {
            errors.append("• Due date must be in the future\n");
        }
        
        // Check if member can borrow
        if (memberCombo.getValue() != null) {
            Member member = memberCombo.getValue();
            if (!member.canBorrow()) {
                errors.append("• Member cannot borrow books. Reason: ");
                if (member.getIsBanned()) {
                    errors.append("Member is banned\n");
                } else if (member.getCurrentFinesDue() > member.getCreditLimit()) {
                    errors.append(String.format("Fines exceed limit ($%.2f > $%.2f)\n", 
                        member.getCurrentFinesDue(), member.getCreditLimit()));
                } else if (member.getOverdueBooksCount() >= 5) {
                    errors.append("Too many overdue books (" + member.getOverdueBooksCount() + ")\n");
                }
            }
        }
        
        // Check if book is available
        if (bookCombo.getValue() != null) {
            Book book = bookCombo.getValue();
            if (!book.isAvailable() || book.getQuantity() <= 0) {
                errors.append("• Selected book is not available\n");
            }
        }
        
        if (errors.length() > 0) {
            showError("Validation Error", errors.toString());
            return false;
        }
        
        return true;
    }
    
    private void openFinePaymentDialog(Borrow borrow) {
        try {
            // Create payment dialog
            Dialog<FineTransaction> dialog = new Dialog<>();
            dialog.setTitle("Pay Fine");
            dialog.setHeaderText("Pay Fine for Borrow #" + borrow.getId());
            
            // Set buttons
            ButtonType payButtonType = new ButtonType("Pay", ButtonBar.ButtonData.OK_DONE);
            dialog.getDialogPane().getButtonTypes().addAll(payButtonType, ButtonType.CANCEL);
            
            // Create form
            GridPane grid = new GridPane();
            grid.setHgap(10);
            grid.setVgap(10);
            grid.setPadding(new javafx.geometry.Insets(20, 150, 10, 10));
            
            TextField amountField = new TextField(String.format("%.2f", borrow.getFineAmount()));
            amountField.setEditable(false);
            
            ComboBox<FineTransaction.PaymentMethod> methodCombo = new ComboBox<>();
            methodCombo.getItems().addAll(
                FineTransaction.PaymentMethod.CASH,
                FineTransaction.PaymentMethod.CREDIT_CARD,
                FineTransaction.PaymentMethod.DEBIT_CARD,
                FineTransaction.PaymentMethod.BANK_TRANSFER,
                FineTransaction.PaymentMethod.ONLINE
            );
            methodCombo.getSelectionModel().selectFirst();
            
            TextField referenceField = new TextField();
            referenceField.setPromptText("Payment reference");
            
            TextArea notesArea = new TextArea();
            notesArea.setPromptText("Additional notes");
            notesArea.setPrefRowCount(3);
            
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
                    return FineTransaction.builder()
                        .borrow(borrow)
                        .member(borrow.getMember())
                        .amount(borrow.getFineAmount())
                        .paymentMethod(methodCombo.getValue())
                        .paymentReference(referenceField.getText())
                        .notes(notesArea.getText())
                        .build();
                }
                return null;
            });
            
            Optional<FineTransaction> result = dialog.showAndWait();
            result.ifPresent(transaction -> {
                try {
                    // Get current user (you'll need to inject user service)
                    // For now, use a dummy user or get from session
                    User currentUser = new User(); // Replace with actual user
                    currentUser.setId(1L); // Admin user
                    
                    fineService.processFinePayment(
                        borrow.getId(),
                        transaction.getAmount(),
                        transaction.getPaymentMethod(),
                        transaction.getPaymentReference(),
                        transaction.getNotes(),
                        currentUser.getId()
                    );
                    
                    loadBorrows();
                    showSuccess("Success", "Fine paid successfully!\nReceipt: " + 
                        (transaction.getReceiptNumber() != null ? 
                         transaction.getReceiptNumber() : "Generated"));
                } catch (Exception e) {
                    showError("Error", "Failed to process payment: " + e.getMessage());
                }
            });
        } catch (Exception e) {
            showError("Error", "Failed to open payment dialog: " + e.getMessage());
        }
    }
    
    private void updateButtonStates() {
        Borrow selectedBorrow = borrowTable.getSelectionModel().getSelectedItem();
        
        if (selectedBorrow != null) {
            returnButton.setDisable(selectedBorrow.isReturned());
            viewFineButton.setDisable(selectedBorrow.getFineAmount() <= 0);
            payFineButton.setDisable(
                selectedBorrow.getFineAmount() <= 0 || 
                selectedBorrow.getFineStatus() == Borrow.FineStatus.PAID ||
                selectedBorrow.getFineStatus() == Borrow.FineStatus.WAIVED
            );
        } else {
            returnButton.setDisable(true);
            viewFineButton.setDisable(true);
            payFineButton.setDisable(true);
        }
    }
    
    private void clearForm() {
        currentBorrow = null;
        bookCombo.getSelectionModel().clearSelection();
        memberCombo.getSelectionModel().clearSelection();
        dueDatePicker.setValue(null);
        updateButtonStates();
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