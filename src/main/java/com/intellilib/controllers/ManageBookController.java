package com.intellilib.controllers;

import com.intellilib.models.Book;
import com.intellilib.models.Category;
import com.intellilib.services.BookService;
import com.intellilib.services.CategoryService;
import com.intellilib.services.FileStorageService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.scene.web.WebView;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Year;
import java.net.URL;

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
    @FXML private Button uploadButton;
    @FXML private Button viewFileButton;
    @FXML private Button clearFileButton;
    
    @FXML private Label statusLabel;
    @FXML private Label fileNameLabel;
    
    private final BookService bookService;
    private final CategoryService categoryService;
    private final FileStorageService fileStorageService;
    private final ObservableList<Book> bookList = FXCollections.observableArrayList();
    
    private File selectedFile;
    private Book currentBook;
    
    @Autowired
    public ManageBookController(BookService bookService, CategoryService categoryService, FileStorageService fileStorageService) {
        this.bookService = bookService;
        this.categoryService = categoryService;
        this.fileStorageService = fileStorageService;
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
        clearFileSelection();
    }
    
    private void setupTableColumns() {
        isbnColumn.setCellValueFactory(new PropertyValueFactory<>("isbn"));
        titleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        authorColumn.setCellValueFactory(new PropertyValueFactory<>("author"));
        yearColumn.setCellValueFactory(new PropertyValueFactory<>("publicationYear"));
        categoryColumn.setCellValueFactory(new PropertyValueFactory<>("category"));
        quantityColumn.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        fileColumn.setCellValueFactory(new PropertyValueFactory<>("originalFileName"));
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
                } else if (book.getCategory() != null && book.getCategory().getName().toLowerCase().contains(lowerCaseFilter)) {
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
    private void handleUploadButton() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select PDF File");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("PDF Files", "*.pdf"),
            new FileChooser.ExtensionFilter("All Files", "*.*")
        );
        
        selectedFile = fileChooser.showOpenDialog(uploadButton.getScene().getWindow());
        if (selectedFile != null) {
            fileNameLabel.setText(selectedFile.getName());
            viewFileButton.setDisable(false);
            clearFileButton.setDisable(false);
            showStatus("File selected: " + selectedFile.getName(), false);
        }
    }
    
    @FXML
    private void handleClearFile() {
        clearFileSelection();
    }
    
    @FXML
    private void handleViewFile() {
        if (currentBook == null) {
            showStatus("No book selected!", true);
            return;
        }
        
        String filePath = currentBook.getFilePath();
        if (filePath == null || filePath.isEmpty()) {
            showStatus("No PDF file associated with this book", true);
            return;
        }
        
        try {
            java.nio.file.Path actualPath = fileStorageService.loadFile(filePath);
            if (actualPath == null) {
                showStatus("File path is invalid: " + filePath, true);
                return;
            }
            
            if (!Files.exists(actualPath)) {
                showStatus("File not found at: " + actualPath.toString(), true);
                return;
            }
            
            // Open PDF in JavaFX window
            openPdfInWindow(actualPath.toFile(), currentBook.getTitle());
            
        } catch (Exception e) {
            showStatus("Cannot open file: " + e.getMessage(), true);
            e.printStackTrace();
        }
    }

    private void openPdfInWindow(File pdfFile, String title) {
        try {
            // Create a new stage (window)
            Stage pdfStage = new Stage();
            pdfStage.setTitle("PDF Viewer - " + title);
            
            // Create WebView
            WebView webView = new WebView();
            
            // Get the PDF.js viewer.html file path
            URL pdfJsViewerUrl = getClass().getResource("/pdfjs/web/viewer.html");
            
            if (pdfJsViewerUrl != null) {
                // Build URL with file parameter
                String viewerUrl = pdfJsViewerUrl.toExternalForm() + "?file=" + 
                                pdfFile.toURI().toString();
                webView.getEngine().load(viewerUrl);
            } else {
                // Fallback: Use browser's built-in PDF viewer (if available)
                webView.getEngine().load(pdfFile.toURI().toString());
            }
            
            // Create scene and show stage
            Scene scene = new Scene(webView, 1024, 768);
            pdfStage.setScene(scene);
            pdfStage.show();
            
            showStatus("Opening PDF in viewer...", false);
            
        } catch (Exception e) {
            showStatus("Error opening PDF viewer: " + e.getMessage(), true);
            e.printStackTrace();
        }
    }
    
    @FXML
    private void handleAddBook() {
        if (validateInput()) {
            try {
                Book book = createBookFromForm();
                
                // Convert JavaFX File to MultipartFile
                org.springframework.web.multipart.MultipartFile multipartFile = null;
                if (selectedFile != null) {
                    multipartFile = createMultipartFile(selectedFile);
                }
                
                bookService.saveBook(book, multipartFile);
                loadBooks();
                clearForm();
                showStatus("Book added successfully!", false);
            } catch (Exception e) {
                showStatus("Error adding book: " + e.getMessage(), true);
            }
        }
    }
    
    @FXML
    private void handleUpdateBook() {
        Book selectedBook = bookTable.getSelectionModel().getSelectedItem();
        if (selectedBook != null && validateInput()) {
            try {
                Book bookDetails = createBookFromForm();
                bookDetails.setId(selectedBook.getId());
                
                // Convert JavaFX File to MultipartFile
                org.springframework.web.multipart.MultipartFile multipartFile = null;
                if (selectedFile != null) {
                    multipartFile = createMultipartFile(selectedFile);
                }
                
                bookService.updateBook(selectedBook.getId(), bookDetails, multipartFile);
                loadBooks();
                clearForm();
                showStatus("Book updated successfully!", false);
            } catch (Exception e) {
                showStatus("Error updating book: " + e.getMessage(), true);
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
            alert.setContentText("Are you sure you want to delete this book? This will also delete the associated PDF file.");
            
            alert.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    try {
                        bookService.deleteBook(selectedBook.getId());
                        loadBooks();
                        clearForm();
                        showStatus("Book deleted successfully!", false);
                    } catch (Exception e) {
                        showStatus("Error deleting book: " + e.getMessage(), true);
                    }
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
        currentBook = book;
        
        isbnField.setText(book.getIsbn());
        titleField.setText(book.getTitle());
        authorField.setText(book.getAuthor());
        yearField.setText(String.valueOf(book.getPublicationYear()));
        
        if (book.getCategory() != null) {
            categoryField.setText(book.getCategory().getName());
        } else {
            categoryField.clear();
        }
        
        quantityField.setText(String.valueOf(book.getQuantity()));
        disableForm(false);
        
        // Handle file display
        if (book.getOriginalFileName() != null && !book.getOriginalFileName().isEmpty()) {
            fileNameLabel.setText(book.getOriginalFileName());
            viewFileButton.setDisable(false);
            clearFileButton.setDisable(false);
            selectedFile = null; // Don't override existing file
        } else {
            clearFileSelection();
        }
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
        clearFileSelection();
        currentBook = null;
        statusLabel.setText("");
    }
    
    private void clearFileSelection() {
        selectedFile = null;
        fileNameLabel.setText("No file selected");
        viewFileButton.setDisable(true);
        clearFileButton.setDisable(true);
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
        
        return book;
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
        
        // File validation is optional - files are optional
        
        if (errorMessage.length() > 0) {
            showStatus(errorMessage.toString(), true);
            return false;
        }
        
        return true;
    }
    
    private void showStatus(String message, boolean isError) {
        statusLabel.setText(message);
        if (isError) {
            statusLabel.setStyle("-fx-text-fill: #c62828;"); // Red color for errors
        } else {
            statusLabel.setStyle("-fx-text-fill: #2e7d32;"); // Green color for success
        }
    }
    
    // Helper method to convert JavaFX File to Spring MultipartFile
    private org.springframework.web.multipart.MultipartFile createMultipartFile(File file) {
        return new org.springframework.web.multipart.MultipartFile() {
            @Override
            public String getName() {
                return "file";
            }
            
            @Override
            public String getOriginalFilename() {
                return file.getName();
            }
            
            @Override
            public String getContentType() {
                return "application/pdf";
            }
            
            @Override
            public boolean isEmpty() {
                return file.length() == 0;
            }
            
            @Override
            public long getSize() {
                return file.length();
            }
            
            @Override
            public byte[] getBytes() throws IOException {
                return Files.readAllBytes(file.toPath());
            }
            
            @Override
            public java.io.InputStream getInputStream() throws IOException {
                return new java.io.FileInputStream(file);
            }
            
            @Override
            public void transferTo(java.io.File dest) throws IOException, IllegalStateException {
                Files.copy(file.toPath(), dest.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
        };
    }
}