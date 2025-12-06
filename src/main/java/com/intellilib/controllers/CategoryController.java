package com.intellilib.controllers;

import com.intellilib.models.Category;
import com.intellilib.services.CategoryService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class CategoryController {

    @FXML private TableView<Category> categoryTable;
    @FXML private TableColumn<Category, Long> idColumn;
    @FXML private TableColumn<Category, String> nameColumn;

    @FXML private TextField nameField;

    private CategoryService categoryService = new CategoryService();
    private ObservableList<Category> categoryList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        idColumn.setCellValueFactory(data -> javafx.beans.property.SimpleLongProperty.longProperty(data.getValue().getId()).asObject());
        nameColumn.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getName()));

        refreshTable();
    }

    @FXML
    private void addCategory() {
        String name = nameField.getText();
        categoryService.createCategory(new Category(name));
        refreshTable();
    }

    @FXML
    private void deleteCategory() {
        Category selected = categoryTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            categoryService.deleteCategory(selected.getId());
            refreshTable();
        }
    }

    private void refreshTable() {
        categoryList.setAll(categoryService.getAllCategories());
        categoryTable.setItems(categoryList);
    }
}
