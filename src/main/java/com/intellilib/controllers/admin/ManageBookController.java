package com.intellilib.controllers.admin;

import com.intellilib.models.Book;
import com.intellilib.models.Category;
import com.intellilib.models.User;
import com.intellilib.services.BookService;
import com.intellilib.services.CategoryService;
import com.intellilib.services.FileStorageService;
import com.intellilib.session.SessionManager;
import com.intellilib.util.ActivityLogger;
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
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

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
    private final FileStorageService fileStorageService;
    private final ActivityLogger activityLogger;
    private final SessionManager sessionManager;
    private final ObservableList<Book> bookList = FXCollections.observableArrayList();
    private final ObservableList<Category> categoryList = FXCollections.observableArrayList();

    // Keep track of the selected file for upload
    private File selectedFile;
    // Keep track of the stored file path from the selected book
    private String storedFilePath;

    @Autowired
    public ManageBookController(BookService bookService,
                                CategoryService categoryService,
                                FileStorageService fileStorageService,
                                ActivityLogger activityLogger,
                                SessionManager sessionManager) {
        this.bookService = bookService;
        this.categoryService = categoryService;
        this.fileStorageService = fileStorageService;
        this.activityLogger = activityLogger;
        this.sessionManager = sessionManager;
    }

    @FXML
    public void initialize() {
        setupTableColumns();
        loadCategories();
        setupCategoryComboBox();
        loadBooks();
        setupTableSelection();
        setupSearch();

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

        if (book.getCategory() != null) {
            categoryComboBox.setValue(book.getCategory());
        } else {
            categoryComboBox.setValue(null);
        }

        // Handle existing file from database
        if (book.getFilePath() != null && !book.getFilePath().isEmpty()) {
            storedFilePath = book.getFilePath();

            if (book.getOriginalFileName() != null) {
                fileNameLabel.setText(book.getOriginalFileName());
            } else {
                String fileName = book.getFilePath().substring(book.getFilePath().lastIndexOf("/") + 1);
                fileNameLabel.setText(fileName);
            }

            viewFileButton.setDisable(false);
            clearFileButton.setDisable(false);
        } else {
            fileNameLabel.setText("No file selected");
            viewFileButton.setDisable(true);
            clearFileButton.setDisable(true);
            storedFilePath = null;
        }

        // Clear selectedFile when populating from existing book
        selectedFile = null;
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
        storedFilePath = null;
        viewFileButton.setDisable(true);
        clearFileButton.setDisable(true);
        bookTable.getSelectionModel().clearSelection();
    }

    @FXML
    private void handleUploadButton() {
        try {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Select PDF File");
            fileChooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("PDF Files", "*.pdf"),
                    new FileChooser.ExtensionFilter("All Files", "*.*")
            );

            File initialDirectory = new File("uploads/books");
            if (initialDirectory.exists() && initialDirectory.isDirectory()) {
                fileChooser.setInitialDirectory(initialDirectory);
            }

            Stage stage = (Stage) bookTable.getScene().getWindow();
            File file = fileChooser.showOpenDialog(stage);

            if (file != null) {
                if (!validateSelectedFile(file)) {
                    return;
                }

                // Just keep reference to the file, don't store it yet
                selectedFile = file;
                fileNameLabel.setText(file.getName());
                viewFileButton.setDisable(false);
                clearFileButton.setDisable(false);

                // Clear any existing stored path since we have a new file
                storedFilePath = null;

                showStatus("File selected: " + file.getName());
            }

        } catch (Exception e) {
            showStatus("Error selecting file: " + e.getMessage());
            resetFileSelection();
        }
    }

    @FXML
    private void handleViewFile() {
        try {
            if (storedFilePath != null && !storedFilePath.isEmpty()) {
                // View existing file from database
                Path filePath = fileStorageService.loadFile(storedFilePath);
                File fileToOpen = filePath.toFile();
                openFileWithProcessBuilder(fileToOpen);
            } else if (selectedFile != null) {
                // View newly selected file (not yet saved)
                openFileWithProcessBuilder(selectedFile);
            } else {
                showStatus("No file to open");
            }
        } catch (Exception e) {
            showStatus("Error opening file: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void openFileWithProcessBuilder(File file) throws IOException {
        if (!file.exists()) {
            showStatus("File not found: " + file.getPath());
            return;
        }

        String os = System.getProperty("os.name").toLowerCase();
        ProcessBuilder processBuilder;

        if (os.contains("win")) {
            processBuilder = new ProcessBuilder("cmd", "/c", "start", "",
                    file.getAbsolutePath().replace("/", "\\"));
        } else if (os.contains("mac")) {
            processBuilder = new ProcessBuilder("open", file.getAbsolutePath());
        } else if (os.contains("nix") || os.contains("nux") || os.contains("aix")) {
            processBuilder = new ProcessBuilder("xdg-open", file.getAbsolutePath());
        } else {
            throw new IOException("Unsupported operating system for file opening");
        }

        try {
            Process process = processBuilder.start();

            new Thread(() -> {
                try {
                    boolean exited = process.waitFor(5, TimeUnit.SECONDS);
                    if (exited && process.exitValue() != 0) {
                        showStatus("Failed to open file. Make sure you have a PDF viewer installed.");
                    } else {
                        showStatus("File opened successfully");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();

        } catch (Exception e) {
            showStatus("Error opening file: " + e.getMessage());
        }
    }

    @FXML
    private void handleClearFile() {
        try {
            // If clearing a stored file (from database), delete it
            if (storedFilePath != null && !storedFilePath.isEmpty()) {
                fileStorageService.deleteFile(storedFilePath);
            }
        } catch (IOException e) {
            showStatus("Warning: Could not delete file: " + e.getMessage());
        }

        resetFileSelection();
        showStatus("File cleared");
    }

    @FXML
    private void handleAddBook() {
        if (validateInput()) {
            try {
                Book book = createBookFromForm();

                // Pass the selected file (if any) to the service
                Book savedBook = bookService.saveBookWithFile(book, selectedFile);

                User user = sessionManager.getCurrentUser();
                activityLogger.logBookAdd(user, savedBook.getTitle());

                loadBooks();
                clearForm();
                showStatus("Book added successfully!");

            } catch (Exception e) {
                showStatus("Error: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    @FXML
    private void handleUpdateBook() {
        Book selectedBook = bookTable.getSelectionModel().getSelectedItem();
        if (selectedBook != null && validateInput()) {
            try {
                Long bookId = selectedBook.getId();
                Book updatedBook = createBookFromForm();
                updatedBook.setId(bookId);

                // Pass the selected file (if any) to the service
                Book savedBook = bookService.updateBookWithFile(bookId, updatedBook, selectedFile);

                User user = sessionManager.getCurrentUser();
                activityLogger.logBookUpdate(user, savedBook.getTitle());

                loadBooks();
                clearForm();
                showStatus("Book updated successfully!");

            } catch (Exception e) {
                showStatus("Error updating book: " + e.getMessage());
                e.printStackTrace();
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
                        if (selectedBook.getFilePath() != null) {
                            fileStorageService.deleteFile(selectedBook.getFilePath());
                        }

                        bookService.deleteBook(selectedBook.getId());
                        User user = sessionManager.getCurrentUser();
                        activityLogger.logBookDelete(user, selectedBook.getTitle());
                        loadBooks();
                        clearForm();
                        showStatus("Book deleted successfully!");
                    } catch (Exception e) {
                        showStatus("Error: " + e.getMessage());
                        e.printStackTrace();
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

        book.setCategory(categoryComboBox.getValue());
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

        if (categoryComboBox.getValue() == null) {
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
            statusLabel.setStyle("-fx-text-fill: #2e7d32;");
        } else if (message.toLowerCase().contains("error") || message.toLowerCase().contains("required")) {
            statusLabel.setStyle("-fx-text-fill: #c62828;");
        } else {
            statusLabel.setStyle("-fx-text-fill: #1565c0;");
        }
    }

    private boolean validateSelectedFile(File file) {
        if (file == null) {
            return false;
        }

        long maxSize = 50 * 1024 * 1024; // 50MB
        if (file.length() > maxSize) {
            showStatus("File too large. Maximum size is 50MB");
            return false;
        }

        String fileName = file.getName().toLowerCase();
        if (!fileName.endsWith(".pdf")) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Confirm File Type");
            alert.setHeaderText("The selected file is not a PDF");
            alert.setContentText("Do you want to continue anyway?");

            Optional<ButtonType> result = alert.showAndWait();
            return result.isPresent() && result.get() == ButtonType.OK;
        }

        return true;
    }

    private void resetFileSelection() {
        selectedFile = null;
        storedFilePath = null;
        fileNameLabel.setText("No file selected");
        viewFileButton.setDisable(true);
        clearFileButton.setDisable(true);
    }
}