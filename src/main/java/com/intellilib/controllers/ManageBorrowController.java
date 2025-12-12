package com.intellilib.controllers;

import com.intellilib.models.Borrow;
import com.intellilib.models.Book;
import com.intellilib.models.Member;
import com.intellilib.services.BorrowService;
import com.intellilib.services.BookService;
import com.intellilib.services.MemberService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
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
    
    @FXML private ComboBox<Book> bookCombo;
    @FXML private ComboBox<Member> memberCombo;
    @FXML private DatePicker dueDatePicker;
    
    @FXML private Button saveButton;
    @FXML private Button cancelButton;
    @FXML private Button returnButton;
    
    private final BorrowService borrowService;
    private final BookService bookService;
    private final MemberService memberService;
    private final ObservableList<Borrow> borrowList = FXCollections.observableArrayList();
    private final ObservableList<Book> availableBooks = FXCollections.observableArrayList();
    private final ObservableList<Member> activeMembers = FXCollections.observableArrayList();
    
    private Borrow currentBorrow;
    
    public ManageBorrowController(BorrowService borrowService, BookService bookService, 
                                MemberService memberService) {
        this.borrowService = borrowService;
        this.bookService = bookService;
        this.memberService = memberService;
    }
    
    @FXML
    public void initialize() {
        setupTableColumns();
        loadAvailableBooks();
        loadActiveMembers();
        loadBorrows();
        setupListeners();
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
    }
    
    private void loadAvailableBooks() {
        availableBooks.setAll(bookService.findAvailableBooks());
        bookCombo.setItems(availableBooks);
    }
    
    private void loadActiveMembers() {
        activeMembers.setAll(memberService.getActiveMembers());
        memberCombo.setItems(activeMembers);
    }
    
    private void loadBorrows() {
        borrowList.setAll(borrowService.getAllBorrows());
        borrowTable.setItems(borrowList);
    }
    
    private void setupListeners() {
        borrowTable.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldSelection, newSelection) -> {
                if (newSelection != null) {
                    populateForm(newSelection);
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
            Borrow borrow = currentBorrow != null ? currentBorrow : new Borrow();
            borrow.setBook(bookCombo.getValue());
            borrow.setMember(memberCombo.getValue());
            borrow.setBorrowDate(LocalDate.now());
            borrow.setDueDate(dueDatePicker.getValue());
            
            borrowService.saveBorrow(borrow);
            clearForm();
            loadBorrows();
        }
    }
    
    @FXML
    private void handleReturn() {
        Borrow selectedBorrow = borrowTable.getSelectionModel().getSelectedItem();
        if (selectedBorrow != null && !selectedBorrow.isReturned()) {
            selectedBorrow.setReturned(true);
            selectedBorrow.setReturnDate(LocalDate.now());
            borrowService.saveBorrow(selectedBorrow);
            loadBorrows();
        }
    }
    
    @FXML
    private void handleDelete() {
        Borrow selectedBorrow = borrowTable.getSelectionModel().getSelectedItem();
        if (selectedBorrow != null) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Confirmation");
            alert.setHeaderText("Supprimer l'emprunt");
            alert.setContentText("Êtes-vous sûr de vouloir supprimer cet emprunt ?");
            
            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                borrowService.deleteBorrow(selectedBorrow.getId());
                loadBorrows();
                clearForm();
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
        if (bookCombo.getValue() == null) {
            showError("Erreur de validation", "Veuillez sélectionner un livre");
            return false;
        }
        if (memberCombo.getValue() == null) {
            showError("Erreur de validation", "Veuillez sélectionner un membre");
            return false;
        }
        if (dueDatePicker.getValue() == null || dueDatePicker.getValue().isBefore(LocalDate.now())) {
            showError("Erreur de validation", "La date d'échéance doit être dans le futur");
            return false;
        }
        return true;
    }
    
    private void clearForm() {
        currentBorrow = null;
        bookCombo.getSelectionModel().clearSelection();
        memberCombo.getSelectionModel().clearSelection();
        dueDatePicker.setValue(null);
    }
    
    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}