package com.intellilib.controllers.member;

import com.intellilib.models.Book;
import com.intellilib.services.BookService;
import com.intellilib.services.BorrowService;
import com.intellilib.services.CategoryService;
import com.intellilib.util.FXMLLoaderUtil;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.stream.Collectors;

@Controller
public class BrowseBooksController {

    @FXML private TextField searchField;
    @FXML private ComboBox<String> categoryFilter;
    @FXML private ComboBox<String> availabilityFilter;

    @FXML private TableView<Book> booksTable;
    @FXML private TableColumn<Book, String> titleColumn;
    @FXML private TableColumn<Book, String> authorColumn;
    @FXML private TableColumn<Book, String> categoryColumn;
    @FXML private TableColumn<Book, Integer> yearColumn;
    @FXML private TableColumn<Book, Boolean> availableColumn;
    @FXML private TableColumn<Book, String> actionsColumn;

    @FXML private Label pageInfoLabel;
    @FXML private ComboBox<String> pageSizeCombo;

    private final BookService bookService;
    private final BorrowService borrowService;
    private final CategoryService categoryService;

    private ObservableList<Book> allBooks = FXCollections.observableArrayList();
    private ObservableList<Book> filteredBooks = FXCollections.observableArrayList();

    private int currentPage = 0;
    private int pageSize = 10;

    public BrowseBooksController(BookService bookService, BorrowService borrowService,
                                CategoryService categoryService) {
        this.bookService = bookService;
        this.borrowService = borrowService;
        this.categoryService = categoryService;
    }

    @FXML
    public void initialize() {
        // setupTableColumns();
        loadBooks();
        setupFilters();
        setupPagination();
    }

    // private void setupTableColumns() {
    //     titleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
    //     authorColumn.setCellValueFactory(new PropertyValueFactory<>("author"));
    //     categoryColumn.setCellValueFactory(cellData ->
    //         new SimpleStringProperty(cellData.getValue().getCategory() != null ?
    //             cellData.getValue().getCategory().getName() : ""));
    //     yearColumn.setCellValueFactory(new PropertyValueFactory<>("publicationYear"));
    //     availableColumn.setCellValueFactory(new PropertyValueFactory<>("available"));

    //     // Actions column with borrow button
    //     actionsColumn.setCellFactory(col -> new TableCell<>() {
    //         private final Button borrowButton = new Button("Emprunter");

    //         @Override
    //         protected void updateItem(Book book, boolean empty) {
    //             super.updateItem(book, empty);
    //             if (empty || book == null) {
    //                 setGraphic(null);
    //             } else {
    //                 borrowButton.setDisable(!book.isAvailable());
    //                 borrowButton.setOnAction(e -> borrowBook(book));
    //                 setGraphic(borrowButton);
    //             }
    //         }
    //     });
    // }

    private void loadBooks() {
        allBooks.setAll(bookService.getAllBooks());
        applyFilters();
    }

    private void setupFilters() {
        // Category filter
        List<String> categories = categoryService.getAllCategories().stream()
            .map(category -> category.getName())
            .collect(Collectors.toList());
        categoryFilter.getItems().setAll(categories);
        categoryFilter.getItems().add(0, "Toutes");
        categoryFilter.setValue("Toutes");

        // Availability filter
        availabilityFilter.getItems().setAll("Tous", "Disponibles", "Non disponibles");
        availabilityFilter.setValue("Tous");

        // Add listeners
        categoryFilter.setOnAction(e -> applyFilters());
        availabilityFilter.setOnAction(e -> applyFilters());
        searchField.textProperty().addListener((obs, oldVal, newVal) -> applyFilters());
    }

    private void setupPagination() {
        pageSizeCombo.getSelectionModel().select("10");
        pageSizeCombo.setOnAction(e -> {
            pageSize = Integer.parseInt(pageSizeCombo.getValue());
            currentPage = 0;
            updateTable();
        });
        updateTable();
    }

    private void applyFilters() {
        String searchText = searchField.getText().toLowerCase();
        String selectedCategory = categoryFilter.getValue();
        String selectedAvailability = availabilityFilter.getValue();

        filteredBooks.setAll(allBooks.stream()
            .filter(book ->
                (searchText.isEmpty() ||
                 book.getTitle().toLowerCase().contains(searchText) ||
                 book.getAuthor().toLowerCase().contains(searchText)) &&
                (selectedCategory.equals("Toutes") ||
                 (book.getCategory() != null && book.getCategory().getName().equals(selectedCategory))) &&
                (selectedAvailability.equals("Tous") ||
                 (selectedAvailability.equals("Disponibles") && book.isAvailable()) ||
                 (selectedAvailability.equals("Non disponibles") && !book.isAvailable())))
            .collect(Collectors.toList()));

        currentPage = 0;
        updateTable();
    }

    private void updateTable() {
        int startIndex = currentPage * pageSize;
        int endIndex = Math.min(startIndex + pageSize, filteredBooks.size());

        booksTable.getItems().setAll(filteredBooks.subList(startIndex, endIndex));
        pageInfoLabel.setText(String.format("Page %d/%d (%d livres)",
            currentPage + 1,
            (int) Math.ceil((double) filteredBooks.size() / pageSize),
            filteredBooks.size()));
    }

    @FXML
    private void searchBooks() {
        applyFilters();
    }

    @FXML
    private void resetFilters() {
        searchField.clear();
        categoryFilter.setValue("Toutes");
        availabilityFilter.setValue("Tous");
        applyFilters();
    }

    @FXML
    private void previousPage() {
        if (currentPage > 0) {
            currentPage--;
            updateTable();
        }
    }

    @FXML
    private void nextPage() {
        int maxPage = (int) Math.ceil((double) filteredBooks.size() / pageSize) - 1;
        if (currentPage < maxPage) {
            currentPage++;
            updateTable();
        }
    }

    private void borrowBook(Book book) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Emprunter un livre");
        confirm.setHeaderText("Emprunter '" + book.getTitle() + "'");
        confirm.setContentText("Êtes-vous sûr de vouloir emprunter ce livre ?");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    // TODO: Implement borrow logic with member ID
                    showSuccess("Succès", "Livre emprunté avec succès !");
                    loadBooks(); // Refresh the list
                } catch (Exception e) {
                    showError("Erreur", "Impossible d'emprunter le livre: " + e.getMessage());
                }
            }
        });
    }

    @FXML
    private void goBack() {
        try {
            Stage stage = (Stage) searchField.getScene().getWindow();
            stage.setScene(FXMLLoaderUtil.loadScene("/views/member-dashboard.fxml"));
            stage.setTitle("Tableau de Bord Membre");
        } catch (Exception e) {
            showError("Erreur", "Impossible de retourner au tableau de bord");
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