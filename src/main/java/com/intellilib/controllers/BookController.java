package com.intellilib.controllers;

import com.intellilib.models.Book;
import com.intellilib.models.Category;
import com.intellilib.services.BookService;
import com.intellilib.services.CategoryService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.SimpleBooleanProperty;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import java.util.Optional;

@Controller
@RequiredArgsConstructor
public class BookController{

    @FXML private TableView<Book> bookTable;
    @FXML private TableColumn<Book, Long> idColumn;
    @FXML private TableColumn<Book, String> titleColumn;
    @FXML private TableColumn<Book, String> authorColumn;
    @FXML private TableColumn<Book, Boolean> availableColumn;

    @FXML private TextField titleField, authorField, isbnField, publisherField, yearField;
    @FXML private TextArea descriptionArea;
    @FXML private ComboBox<Category> categoryCombo;

    // Spring injects these
    private final BookService bookService;
    private final CategoryService categoryService;
    
    private ObservableList<Book> bookList = FXCollections.observableArrayList();
    private ObservableList<Category> categoryList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        // Setup table columns
        idColumn.setCellValueFactory(data -> new SimpleLongProperty(data.getValue().getId()).asObject());
        titleColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getTitle()));
        authorColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getAuthor()));
        availableColumn.setCellValueFactory(data -> new SimpleBooleanProperty(data.getValue().isAvailable()));
        
        // Load categories
        categoryList.setAll(categoryService.getAllCategories());
        categoryCombo.setItems(categoryList);
        
        refreshTable();
    }

    @FXML
    private void addBook() {
        try {
            String title = titleField.getText().trim();
            String author = authorField.getText().trim();
            String isbn = isbnField.getText().trim();
            String publisher = publisherField.getText().trim();
            Integer year = yearField.getText().isEmpty() ? null : Integer.parseInt(yearField.getText());
            String description = descriptionArea.getText().trim();
            Category category = categoryCombo.getValue();
            
            if (title.isEmpty() || author.isEmpty()) {
                showAlert("Erreur", "Le titre et l'auteur sont obligatoires");
                return;
            }
            
            Book book = new Book(title, author, isbn);
            book.setPublisher(publisher);
            book.setPublicationYear(year);
            book.setDescription(description);
            book.setCategory(category);
            
            bookService.saveBook(book);
            clearFields();
            refreshTable();
            
        } catch (NumberFormatException e) {
            showAlert("Erreur", "L'année doit être un nombre valide");
        } catch (Exception e) {
            showAlert("Erreur", "Impossible d'ajouter le livre: " + e.getMessage());
        }
    }

    @FXML
    private void deleteBook() {
        Book selected = bookTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Confirmation");
            confirm.setContentText("Voulez-vous vraiment supprimer ce livre ?");
            Optional<ButtonType> result = confirm.showAndWait();
            
            if (result.isPresent() && result.get() == ButtonType.OK) {
                try {
                    bookService.deleteBook(selected.getId());
                    refreshTable();
                } catch (Exception e) {
                    showAlert("Erreur", "Impossible de supprimer le livre: " + e.getMessage());
                }
            }
        } else {
            showAlert("Erreur", "Veuillez sélectionner un livre à supprimer");
        }
    }
    
    @FXML
    private void editBook() {
        Book selected = bookTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            // Load book data into fields for editing
            titleField.setText(selected.getTitle());
            authorField.setText(selected.getAuthor());
            isbnField.setText(selected.getIsbn());
            publisherField.setText(selected.getPublisher());
            yearField.setText(selected.getPublicationYear() != null ? 
                selected.getPublicationYear().toString() : "");
            descriptionArea.setText(selected.getDescription());
            categoryCombo.setValue(selected.getCategory());
        }
    }

    private void refreshTable() {
        bookList.setAll(bookService.getAllBooks());
        bookTable.setItems(bookList);
    }
    
    private void clearFields() {
        titleField.clear();
        authorField.clear();
        isbnField.clear();
        publisherField.clear();
        yearField.clear();
        descriptionArea.clear();
        categoryCombo.setValue(null);
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.showAndWait();
    }
}