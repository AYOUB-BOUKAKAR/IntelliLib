package com.intellilib.controllers;

import com.intellilib.models.Book;
import com.intellilib.models.Category;
import com.intellilib.services.BookService;
import com.intellilib.services.CategoryService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import java.time.Year;

@Controller
public class ManageBookController {

    @FXML private TableView<Book> bookTable;
    @FXML private TableColumn<Book, String> isbnColumn;
    @FXML private TableColumn<Book, String> titleColumn;
    @FXML private TableColumn<Book, String> authorColumn;
    @FXML private TableColumn<Book, Integer> yearColumn;
    @FXML private TableColumn<Book, String> categoryColumn;
    @FXML private TableColumn<Book, Integer> quantityColumn;
    
    @FXML private TextField isbnField;
    @FXML private TextField titleField;
    @FXML private TextField authorField;
    @FXML private TextField yearField;
    @FXML private TextField categoryField;
    @FXML private TextField quantityField;
    @FXML private TextField searchField;
    
    @FXML private Button addButton;
    @FXML private Button updateButton;
    @FXML private Button deleteButton;
    @FXML private Button clearButton;
    
    @FXML private Label statusLabel;
    
    private final BookService bookService;
    private final CategoryService categoryService;
    private final ObservableList<Book> bookList = FXCollections.observableArrayList();
    
    @Autowired
    public ManageBookController(BookService bookService, CategoryService categoryService) {
        this.bookService = bookService;
        this.categoryService = categoryService;
    }
    
    @FXML
    public void initialize() {
        setupTableColumns();
        loadBooks();
        setupTableSelection();
        setupSearch();
        disableForm(false);
        updateButton.setDisable(true);
        deleteButton.setDisable(true);
    }
    
    private void setupTableColumns() {
        isbnColumn.setCellValueFactory(new PropertyValueFactory<>("isbn"));
        titleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        authorColumn.setCellValueFactory(new PropertyValueFactory<>("author"));
        yearColumn.setCellValueFactory(new PropertyValueFactory<>("publicationYear"));
        categoryColumn.setCellValueFactory(new PropertyValueFactory<>("category"));
        quantityColumn.setCellValueFactory(new PropertyValueFactory<>("quantity"));
    }
    
    private void loadBooks() {
        bookList.setAll(bookService.getAllBooks());
        bookTable.setItems(bookList);
    }
    
    private void setupTableSelection() {
        bookTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                populateForm(newSelection);
                addButton.setDisable(true);
                updateButton.setDisable(false);
                deleteButton.setDisable(false);
            } else {
                clearForm();
                addButton.setDisable(false);
                updateButton.setDisable(true);
                deleteButton.setDisable(true);
            }
        });
    }
    
    private void setupSearch() {
        FilteredList<Book> filteredData = new FilteredList<>(bookList, p -> true);
        
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(book -> {
                if (newValue == null || newValue.isEmpty()) {
                    return true;
                }
                
                String lowerCaseFilter = newValue.toLowerCase();
                
                if (book.getTitle().toLowerCase().contains(lowerCaseFilter)) {
                    return true;
                } else if (book.getAuthor().toLowerCase().contains(lowerCaseFilter)) {
                    return true;
                } else if (book.getIsbn().toLowerCase().contains(lowerCaseFilter)) {
                    return true;
                } else if (book.getCategory().getName().toLowerCase().contains(lowerCaseFilter)) {
                    return true;
                }
                return false;
            });
        });
        
        SortedList<Book> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(bookTable.comparatorProperty());
        bookTable.setItems(sortedData);
    }
    
    @FXML
    private void handleAddBook() {
        if (validateInput()) {
            Book book = createBookFromForm();
            bookService.saveBook(book);
            loadBooks();
            clearForm();
            showStatus("Book added successfully!");
        }
    }
    
    @FXML
    private void handleUpdateBook() {
        Book selectedBook = bookTable.getSelectionModel().getSelectedItem();
        if (selectedBook != null && validateInput()) {
            updateBookFromForm(selectedBook);
            bookService.saveBook(selectedBook);
            loadBooks();
            clearForm();
            showStatus("Book updated successfully!");
        }
    }
    
    @FXML
    private void handleDeleteBook() {
        Book selectedBook = bookTable.getSelectionModel().getSelectedItem();
        if (selectedBook != null) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Delete Book");
            alert.setHeaderText("Delete " + selectedBook.getTitle());
            alert.setContentText("Are you sure you want to delete this book?");
            
            alert.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    bookService.deleteBook(selectedBook.getId());
                    loadBooks();
                    clearForm();
                    showStatus("Book deleted successfully!");
                }
            });
        }
    }
    
    @FXML
    private void handleClearForm() {
        clearForm();
        bookTable.getSelectionModel().clearSelection();
    }
    
    private void populateForm(Book book) {
        isbnField.setText(book.getIsbn());
        titleField.setText(book.getTitle());
        authorField.setText(book.getAuthor());
        yearField.setText(String.valueOf(book.getPublicationYear()));
        categoryField.setText(book.getCategory().getName());
        quantityField.setText(String.valueOf(book.getQuantity()));
        disableForm(false);
    }
    
    private void clearForm() {
        isbnField.clear();
        titleField.clear();
        authorField.clear();
        yearField.clear();
        categoryField.clear();
        quantityField.clear();
        disableForm(false);
        addButton.setDisable(false);
        updateButton.setDisable(true);
        deleteButton.setDisable(true);
    }
    
    private void disableForm(boolean disable) {
        isbnField.setDisable(disable);
        titleField.setDisable(disable);
        authorField.setDisable(disable);
        yearField.setDisable(disable);
        categoryField.setDisable(disable);
        quantityField.setDisable(disable);
    }
    
    private Book createBookFromForm() {
        Book book = new Book();
        updateBookFromForm(book);
        return book;
    }
    
    private void updateBookFromForm(Book book) {
        book.setIsbn(isbnField.getText().trim());
        book.setTitle(titleField.getText().trim());
        book.setAuthor(authorField.getText().trim());
        book.setPublicationYear(Integer.parseInt(yearField.getText().trim()));
        
        String categoryName = categoryField.getText().trim();
            Category category = categoryService.getCategoryByName(categoryName);
            if (category == null) {
                category = new Category(categoryName);
                categoryService.createCategory(category);
            }
        book.setCategory(category);
        book.setQuantity(Integer.parseInt(quantityField.getText().trim()));
    }
    
    private boolean validateInput() {
        StringBuilder errorMessage = new StringBuilder();
        
        if (isbnField.getText() == null || isbnField.getText().trim().isEmpty()) {
            errorMessage.append("ISBN is required!\n");
        }
        
        if (titleField.getText() == null || titleField.getText().trim().isEmpty()) {
            errorMessage.append("Title is required!\n");
        }
        
        if (authorField.getText() == null || authorField.getText().trim().isEmpty()) {
            errorMessage.append("Author is required!\n");
        }
        
        try {
            int year = Integer.parseInt(yearField.getText().trim());
            int currentYear = Year.now().getValue();
            if (year < 1000 || year > currentYear + 1) {
                errorMessage.append("Please enter a valid publication year!\n");
            }
        } catch (NumberFormatException e) {
            errorMessage.append("Please enter a valid year!\n");
        }
        
        if (categoryField.getText() == null || categoryField.getText().trim().isEmpty()) {
            errorMessage.append("Category is required!\n");
        }
        
        try {
            int quantity = Integer.parseInt(quantityField.getText().trim());
            if (quantity < 0) {
                errorMessage.append("Quantity cannot be negative!\n");
            }
        } catch (NumberFormatException e) {
            errorMessage.append("Please enter a valid quantity!\n");
        }
        
        if (errorMessage.length() > 0) {
            showStatus(errorMessage.toString());
            return false;
        }
        
        return true;
    }
    
    private void showStatus(String message) {
        statusLabel.setText(message);
        statusLabel.setStyle("-fx-text-fill: #2e7d32;"); // Green color for success
        
        if (message.toLowerCase().contains("error") || message.toLowerCase().contains("required")) {
            statusLabel.setStyle("-fx-text-fill: #c62828;"); // Red color for errors
        }
    }
}
