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
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;

import java.time.LocalDate;
import java.util.Optional;

@Controller
@RequiredArgsConstructor
public class BorrowController {

    @FXML private TableView<Borrow> borrowTable;
    @FXML private TableColumn<Borrow, Long> idColumn;
    @FXML private TableColumn<Borrow, String> bookColumn;
    @FXML private TableColumn<Borrow, String> memberColumn;
    @FXML private TableColumn<Borrow, LocalDate> borrowDateColumn;
    @FXML private TableColumn<Borrow, LocalDate> dueDateColumn;
    @FXML private TableColumn<Borrow, Boolean> returnedColumn;

    @FXML private ComboBox<Book> bookCombo;
    @FXML private ComboBox<Member> memberCombo;
    @FXML private DatePicker dueDatePicker;

    private final BorrowService borrowService;
    private final BookService bookService;
    private final MemberService memberService;
    
    private ObservableList<Borrow> borrowList = FXCollections.observableArrayList();
    private ObservableList<Book> availableBooks = FXCollections.observableArrayList();
    private ObservableList<Member> activeMembers = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        setupTableColumns();
        loadComboBoxData();
        refreshTable();
    }
    
    private void setupTableColumns() {
        idColumn.setCellValueFactory(data -> new SimpleLongProperty(data.getValue().getId()).asObject());
        bookColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getBook().getTitle()));
        memberColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getMember().getFullName()));
        borrowDateColumn.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().getBorrowDate()));
        dueDateColumn.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().getDueDate()));
        returnedColumn.setCellValueFactory(data -> new SimpleBooleanProperty(data.getValue().isReturned()));
    }
    
    private void loadComboBoxData() {
        availableBooks.setAll(bookService.getAllBooks().stream()
            .filter(Book::isAvailable)
            .toList());
        bookCombo.setItems(availableBooks);
        
        activeMembers.setAll(memberService.getActiveMembers());
        memberCombo.setItems(activeMembers);
        
        dueDatePicker.setValue(LocalDate.now().plusDays(14));
    }

    @FXML
    private void addBorrow() {
        try {
            Book book = bookCombo.getValue();
            Member member = memberCombo.getValue();
            LocalDate dueDate = dueDatePicker.getValue();
            
            if (book == null || member == null || dueDate == null) {
                showAlert("Erreur", "Veuillez remplir tous les champs");
                return;
            }
            
            if (dueDate.isBefore(LocalDate.now())) {
                showAlert("Erreur", "La date de retour doit être dans le futur");
                return;
            }
            
            Borrow borrow = borrowService.borrowBook(book.getId(), member.getId(), dueDate);
            if (borrow != null) {
                refreshTable();
                loadComboBoxData(); // Refresh available books
                showSuccess("Succès", "Emprunt enregistré avec succès");
            }
            
        } catch (Exception e) {
            showAlert("Erreur", "Impossible d'ajouter l'emprunt: " + e.getMessage());
        }
    }

    @FXML
    private void returnBorrow() {
        Borrow selected = borrowTable.getSelectionModel().getSelectedItem();
        if (selected != null && !selected.isReturned()) {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Confirmation");
            confirm.setContentText("Voulez-vous vraiment marquer cet emprunt comme retourné ?");
            Optional<ButtonType> result = confirm.showAndWait();
            
            if (result.isPresent() && result.get() == ButtonType.OK) {
                try {
                    boolean success = borrowService.returnBook(selected.getId());
                    if (success) {
                        refreshTable();
                        loadComboBoxData();
                        showSuccess("Succès", "Retour enregistré avec succès");
                    }
                } catch (Exception e) {
                    showAlert("Erreur", "Impossible de retourner l'emprunt: " + e.getMessage());
                }
            }
        } else {
            showAlert("Erreur", "Veuillez sélectionner un emprunt non retourné");
        }
    }

    private void refreshTable() {
        borrowList.setAll(borrowService.getActiveBorrows());
        borrowTable.setItems(borrowList);
    }
    
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    private void showSuccess(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.showAndWait();
    }
}