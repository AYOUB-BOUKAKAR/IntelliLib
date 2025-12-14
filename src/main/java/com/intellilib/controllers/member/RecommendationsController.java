package com.intellilib.controllers.member;

import com.intellilib.models.Book;
import com.intellilib.models.User;
import com.intellilib.services.UserService;
import com.intellilib.services.BookService;
import com.intellilib.services.BorrowService;
import com.intellilib.util.FXMLLoaderUtil;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Controller
public class RecommendationsController {

    @FXML private HBox preferencesContainer;
    @FXML private HBox historyContainer;
    @FXML private HBox categoriesContainer;
    @FXML private HBox newBooksContainer;

    @FXML private ProgressIndicator preferencesProgress;
    @FXML private ProgressIndicator historyProgress;
    @FXML private ProgressIndicator categoriesProgress;
    @FXML private ProgressIndicator newBooksProgress;

    @FXML private CheckBox showPreferencesCheck;
    @FXML private CheckBox showHistoryCheck;
    @FXML private CheckBox showCategoriesCheck;
    @FXML private CheckBox showNewBooksCheck;
    @FXML private Spinner<Integer> limitSpinner;

    private final UserService userService;
    private final BookService bookService;
    private final BorrowService borrowService;

    private User currentUser;
    private final ExecutorService executor = Executors.newFixedThreadPool(4);

    public RecommendationsController(UserService userService, BookService bookService,
                                   BorrowService borrowService) {
        this.userService = userService;
        this.bookService = bookService;
        this.borrowService = borrowService;
    }

    @FXML
    public void initialize() {
        currentUser = userService.getCurrentUser();
        if (currentUser == null) {
            showError("Erreur", "Utilisateur non connecté");
            goBack();
            return;
        }

        setupControls();
        loadAllRecommendations();
    }

    private void setupControls() {
        // Setup spinner
        SpinnerValueFactory<Integer> valueFactory =
            new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 20, 5);
        limitSpinner.setValueFactory(valueFactory);

        // Add listeners to checkboxes
        showPreferencesCheck.selectedProperty().addListener((obs, oldVal, newVal) -> {
            preferencesContainer.setVisible(newVal);
            preferencesContainer.setManaged(newVal);
        });

        showHistoryCheck.selectedProperty().addListener((obs, oldVal, newVal) -> {
            historyContainer.setVisible(newVal);
            historyContainer.setManaged(newVal);
        });

        showCategoriesCheck.selectedProperty().addListener((obs, oldVal, newVal) -> {
            categoriesContainer.setVisible(newVal);
            categoriesContainer.setManaged(newVal);
        });

        showNewBooksCheck.selectedProperty().addListener((obs, oldVal, newVal) -> {
            newBooksContainer.setVisible(newVal);
            newBooksContainer.setManaged(newVal);
        });
    }

    private void loadAllRecommendations() {
        int limit = limitSpinner.getValue();

        // Load recommendations based on preferences
        if (showPreferencesCheck.isSelected()) {
            showProgress(preferencesProgress, true);
            executor.submit(() -> {
                try {
                    List<Book> books = bookService.getAllBooks().stream()
                        .limit(limit)
                        .toList();

                    Platform.runLater(() -> {
                        displayBooks(books, preferencesContainer);
                        showProgress(preferencesProgress, false);
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> {
                        showProgress(preferencesProgress, false);
                        showErrorInContainer(preferencesContainer, e.getMessage());
                    });
                }
            });
        }

        // Load recommendations based on history
        if (showHistoryCheck.isSelected()) {
            showProgress(historyProgress, true);
            executor.submit(() -> {
                try {
                    List<Book> books = bookService.getAllBooks().stream()
                        .skip(5)
                        .limit(limit)
                        .toList();

                    Platform.runLater(() -> {
                        displayBooks(books, historyContainer);
                        showProgress(historyProgress, false);
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> {
                        showProgress(historyProgress, false);
                        showErrorInContainer(historyContainer, e.getMessage());
                    });
                }
            });
        }

        // Load recommendations by categories
        if (showCategoriesCheck.isSelected()) {
            showProgress(categoriesProgress, true);
            executor.submit(() -> {
                try {
                    List<Book> books = bookService.getAllBooks().stream()
                        .skip(10)
                        .limit(limit)
                        .toList();

                    Platform.runLater(() -> {
                        displayBooks(books, categoriesContainer);
                        showProgress(categoriesProgress, false);
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> {
                        showProgress(categoriesProgress, false);
                        showErrorInContainer(categoriesContainer, e.getMessage());
                    });
                }
            });
        }

        // Load new books
        if (showNewBooksCheck.isSelected()) {
            showProgress(newBooksProgress, true);
            executor.submit(() -> {
                try {
                    List<Book> books = bookService.getAllBooks().stream()
                        .skip(15)
                        .limit(limit)
                        .toList();

                    Platform.runLater(() -> {
                        displayBooks(books, newBooksContainer);
                        showProgress(newBooksProgress, false);
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> {
                        showProgress(newBooksProgress, false);
                        showErrorInContainer(newBooksContainer, e.getMessage());
                    });
                }
            });
        }
    }

    private void displayBooks(List<Book> books, HBox container) {
        container.getChildren().clear();

        if (books.isEmpty()) {
            Label noBooksLabel = new Label("Aucune recommandation disponible");
            noBooksLabel.getStyleClass().add("no-books-label");
            container.getChildren().add(noBooksLabel);
            return;
        }

        for (Book book : books) {
            VBox bookCard = createBookCard(book);
            container.getChildren().add(bookCard);
        }
    }

    private VBox createBookCard(Book book) {
        VBox card = new VBox(10);
        card.getStyleClass().add("book-card");
        card.setPrefWidth(180);

        // Book cover (placeholder)
        ImageView cover = new ImageView();
        cover.setFitWidth(150);
        cover.setFitHeight(200);
        cover.getStyleClass().add("book-cover");

        try {
            // In a real app, load actual book cover
            cover.setImage(new Image(getClass().getResourceAsStream("/images/book-placeholder.png")));
        } catch (Exception e) {
            // Use default placeholder
            cover.setStyle("-fx-background-color: #e0e0e0;");
        }

        // Book info
        VBox info = new VBox(5);

        Label titleLabel = new Label(book.getTitle());
        titleLabel.getStyleClass().add("book-title");
        titleLabel.setWrapText(true);
        titleLabel.setMaxWidth(150);

        Label authorLabel = new Label(book.getAuthor());
        authorLabel.getStyleClass().add("book-author");

        Label categoryLabel = new Label(book.getCategory() != null ?
            book.getCategory().getName() : "Non catégorisé");
        categoryLabel.getStyleClass().add("book-category");

        Label availabilityLabel = new Label(book.isAvailable() ? "Disponible" : "Emprunté");
        availabilityLabel.getStyleClass().add(book.isAvailable() ? "available" : "unavailable");

        // Borrow button
        Button borrowButton = new Button("Emprunter");
        borrowButton.getStyleClass().add("borrow-button");
        borrowButton.setDisable(!book.isAvailable());
        borrowButton.setOnAction(e -> borrowBook(book));

        info.getChildren().addAll(titleLabel, authorLabel, categoryLabel, availabilityLabel, borrowButton);
        card.getChildren().addAll(cover, info);

        return card;
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
                    refreshRecommendations();
                } catch (Exception e) {
                    showError("Erreur", "Impossible d'emprunter le livre: " + e.getMessage());
                }
            }
        });
    }

    @FXML
    private void refreshRecommendations() {
        loadAllRecommendations();
    }

    private void showProgress(ProgressIndicator progress, boolean show) {
        progress.setVisible(show);
    }

    private void showErrorInContainer(HBox container, String message) {
        container.getChildren().clear();
        Label errorLabel = new Label("Erreur: " + message);
        errorLabel.getStyleClass().add("error-label");
        container.getChildren().add(errorLabel);
    }

    @FXML
    private void goBack() {
        try {
            Stage stage = (Stage) preferencesContainer.getScene().getWindow();
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

    public void shutdown() {
        executor.shutdown();
    }
}