package com.intellilib.controllers;

import com.intellilib.models.Book;
import com.intellilib.models.Category;
import com.intellilib.services.BookService;
import com.intellilib.services.CategoryService;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import java.io.File;
import java.io.IOException;
import java.util.Comparator;

@Controller
public class ManageBookController {

    @FXML private TableView<Book> bookTable;
    @FXML private TableColumn<Book, String> isbnColumn;
    @FXML private TableColumn<Book, String> titleColumn;
    @FXML private TableColumn<Book, String> authorColumn;
    @FXML private TableColumn<Book, Integer> yearColumn;
    @FXML private TableColumn<Book, String> categoryColumn;
    @FXML private TableColumn<Book, Integer> quantityColumn;
    @FXML private TableColumn<Book, String> fileColumn;
    
    @FXML private TextField searchField;
    @FXML private TextField isbnField;
    @FXML private TextField titleField;
    @FXML private TextField authorField;
    @FXML private TextField yearField;
    @FXML private TextField quantityField;
    @FXML private ComboBox<Category> categoryComboBox;
    
    @FXML private Button uploadButton;
    @FXML private Button viewFileButton;
    @FXML private Button clearFileButton;
    @FXML private Button addButton;
    @FXML private Button updateButton;
    @FXML private Button deleteButton;
    @FXML private Button clearButton;
    
    @FXML private Label fileNameLabel;
    @FXML private Label statusLabel;
    
    private final BookService bookService;
    private final CategoryService categoryService;
    private final ObservableList<Book> bookList = FXCollections.observableArrayList();
    private final ObservableList<Category> categoryList = FXCollections.observableArrayList();
    private File selectedFile;
    
    @Autowired
    public ManageBookController(BookService bookService, CategoryService categoryService) {
        this.bookService = bookService;
        this.categoryService = categoryService;
    }
    
    @FXML
    public void initialize() {
        setupTableColumns();
        loadCategories();
        setupCategoryComboBox();
        loadBooks();
        setupTableSelection();
        setupSearch();
        
        // Initial button states
        updateButton.setDisable(true);
        deleteButton.setDisable(true);
        viewFileButton.setDisable(true);
        clearFileButton.setDisable(true);
    }
    
    private void setupTableColumns() {
        isbnColumn.setCellValueFactory(new PropertyValueFactory<>("isbn"));
        titleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        authorColumn.setCellValueFactory(new PropertyValueFactory<>("author"));
        yearColumn.setCellValueFactory(new PropertyValueFactory<>("publicationYear"));
        categoryColumn.setCellValueFactory(cellData -> {
            Category category = cellData.getValue().getCategory();
            return new javafx.beans.property.SimpleStringProperty(
                category != null ? category.getName() : "No Category"
            );
        });
        quantityColumn.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        fileColumn.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleStringProperty(
                cellData.getValue().getOriginalFileName() != null ? 
                cellData.getValue().getOriginalFileName() : "No File"
            )
        );
    }
    
    private void loadCategories() {
        try {
            categoryList.setAll(categoryService.getAllCategories());
            // Sort categories by name
            categoryList.sort(Comparator.comparing(Category::getName));
        } catch (Exception e) {
            showStatus("Error loading categories: " + e.getMessage());
        }
    }
    
    private void setupCategoryComboBox() {
        categoryComboBox.setItems(categoryList);
        
        categoryComboBox.setConverter(new javafx.util.StringConverter<Category>() {
            @Override
            public String toString(Category category) {
                return category == null ? "" : category.getName();
            }
            
            @Override
            public Category fromString(String string) {
                return null;
            }
        });
        
        categoryComboBox.setEditable(false);
    }
    
    private void loadBooks() {
        try {
            bookList.setAll(bookService.getAllBooks());
            bookTable.setItems(bookList);
        } catch (Exception e) {
            showStatus("Error loading books: " + e.getMessage());
        }
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
                
                return book.getTitle().toLowerCase().contains(lowerCaseFilter) ||
                       book.getAuthor().toLowerCase().contains(lowerCaseFilter) ||
                       (book.getIsbn() != null && book.getIsbn().toLowerCase().contains(lowerCaseFilter));
            });
        });
        
        javafx.collections.transformation.SortedList<Book> sortedData = new javafx.collections.transformation.SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(bookTable.comparatorProperty());
        bookTable.setItems(sortedData);
    }
    
    private void populateForm(Book book) {
        isbnField.setText(book.getIsbn());
        titleField.setText(book.getTitle());
        authorField.setText(book.getAuthor());
        yearField.setText(book.getPublicationYear() != null ? 
                         book.getPublicationYear().toString() : "");
        quantityField.setText(book.getQuantity() != null ? 
                            book.getQuantity().toString() : "1");
        
        // Set category in ComboBox
        if (book.getCategory() != null) {
            categoryComboBox.setValue(book.getCategory());
        } else {
            categoryComboBox.setValue(null);
        }
        
        // Set file info
        if (book.getOriginalFileName() != null) {
            fileNameLabel.setText(book.getOriginalFileName());
            viewFileButton.setDisable(false);
            clearFileButton.setDisable(false);
        } else {
            fileNameLabel.setText("No file selected");
            viewFileButton.setDisable(true);
            clearFileButton.setDisable(true);
        }
    }
    
    private void clearForm() {
        isbnField.clear();
        titleField.clear();
        authorField.clear();
        yearField.clear();
        quantityField.clear();
        categoryComboBox.setValue(null);
        categoryComboBox.getEditor().clear();
        fileNameLabel.setText("No file selected");
        selectedFile = null;
        viewFileButton.setDisable(true);
        clearFileButton.setDisable(true);
        bookTable.getSelectionModel().clearSelection();
    }
    
    @FXML
    private void handleUploadButton() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select PDF File");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("PDF Files", "*.pdf"),
            new FileChooser.ExtensionFilter("All Files", "*.*")
        );
        
        Stage stage = (Stage) bookTable.getScene().getWindow();
        File file = fileChooser.showOpenDialog(stage);
        
        if (file != null) {
            try {
                selectedFile = file;
                fileNameLabel.setText(file.getName());
                viewFileButton.setDisable(false);
                clearFileButton.setDisable(false);
                showStatus("File selected: " + file.getName());
            } catch (Exception e) {
                showStatus("Error: " + e.getMessage());
            }
        }
    }
    
    @FXML
    private void handleViewFile() {
        // Implementation for viewing file
        // You can open the file with system default program
        if (selectedFile != null) {
            try {
                java.awt.Desktop.getDesktop().open(selectedFile);
            } catch (IOException e) {
                showStatus("Error opening file: " + e.getMessage());
            }
        }
    }
    
    @FXML
    private void handleClearFile() {
        selectedFile = null;
        fileNameLabel.setText("No file selected");
        viewFileButton.setDisable(true);
        clearFileButton.setDisable(true);
    }
    
    @FXML
    private void handleAddBook() {
        if (validateInput()) {
            try {
                Book book = createBookFromForm();
                // Note: You'll need to handle file upload separately in your service
                bookService.saveBook(book, null); // Pass null for file or modify as needed
                loadBooks();
                clearForm();
                showStatus("Book added successfully!");
            } catch (Exception e) {
                showStatus("Error: " + e.getMessage());
            }
        }
    }
    
    @FXML
    private void handleUpdateBook() {
        Book selectedBook = bookTable.getSelectionModel().getSelectedItem();
        if (selectedBook != null && validateInput()) {
            try {
                updateBookFromForm(selectedBook);
                bookService.updateBook(selectedBook.getId(), selectedBook, null);
                loadBooks();
                showStatus("Book updated successfully!");
            } catch (Exception e) {
                showStatus("Error: " + e.getMessage());
            }
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
                    try {
                        bookService.deleteBook(selectedBook.getId());
                        loadBooks();
                        clearForm();
                        showStatus("Book deleted successfully!");
                    } catch (Exception e) {
                        showStatus("Error: " + e.getMessage());
                    }
                }
            });
        }
    }
    
    @FXML
    private void handleClearForm() {
        clearForm();
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
        
        if (!yearField.getText().isEmpty()) {
            try {
                book.setPublicationYear(Integer.parseInt(yearField.getText().trim()));
            } catch (NumberFormatException e) {
                book.setPublicationYear(null);
            }
        }
        
        if (!quantityField.getText().isEmpty()) {
            try {
                book.setQuantity(Integer.parseInt(quantityField.getText().trim()));
            } catch (NumberFormatException e) {
                book.setQuantity(1);
            }
        } else {
            book.setQuantity(1);
        }
        
        // Set category
        book.setCategory(categoryComboBox.getValue());
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
        
        if (categoryComboBox.getValue() == null && 
            (categoryComboBox.getEditor().getText() == null || 
             categoryComboBox.getEditor().getText().trim().isEmpty())) {
            errorMessage.append("Category is required!\n");
        }
        
        if (errorMessage.length() > 0) {
            showStatus(errorMessage.toString());
            return false;
        }
        
        return true;
    }
    
    private void showStatus(String message) {
        statusLabel.setText(message);
        
        if (message.toLowerCase().contains("success")) {
            statusLabel.setStyle("-fx-text-fill: #2e7d32;"); // Green
        } else if (message.toLowerCase().contains("error") || message.toLowerCase().contains("required")) {
            statusLabel.setStyle("-fx-text-fill: #c62828;"); // Red
        } else {
            statusLabel.setStyle("-fx-text-fill: #1565c0;"); // Blue for info
        }
    }
}