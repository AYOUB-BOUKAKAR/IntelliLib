package com.intellilib.controllers;

import com.intellilib.models.Book;
import com.intellilib.services.BookService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class BookController {

    @FXML private TableView<Book> bookTable;
    @FXML private TableColumn<Book, Long> idColumn;
    @FXML private TableColumn<Book, String> titleColumn;
    @FXML private TableColumn<Book, String> authorColumn;

    @FXML private TextField titleField, authorField, genreField, yearField, coverUrlField;
    @FXML private TextArea descriptionArea;

    private BookService bookService = new BookService();
    private ObservableList<Book> bookList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        idColumn.setCellValueFactory(data -> javafx.beans.property.SimpleLongProperty.longProperty(data.getValue().getId()).asObject());
        titleColumn.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getTitle()));
        authorColumn.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getAuthor()));

        refreshTable();
    }

    @FXML
    private void addBook() {
        try {
            String title = titleField.getText();
            String author = authorField.getText();
            String genre = genreField.getText();
            int year = Integer.parseInt(yearField.getText());
            String cover = coverUrlField.getText();
            String desc = descriptionArea.getText();

            bookService.createBook(new Book(title, author, genre, desc, year, cover));
            refreshTable();
        } catch (NumberFormatException e) {
            showAlert("Erreur", "Année invalide");
        }
    }

    @FXML
    private void deleteBook() {
        Book selected = bookTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            bookService.deleteBook(selected.getId());
            refreshTable();
        }
    }

    private void refreshTable() {
        bookList.setAll(bookService.getAllBooks());
        bookTable.setItems(bookList);
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.showAndWait();
    }
}