package com.intellilib.controllers;

import com.intellilib.models.Category;
import com.intellilib.services.CategoryService;

import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;

import java.util.Optional;

@Controller
@RequiredArgsConstructor
public class CategoryController {

    @FXML private TableView<Category> categoryTable;
    @FXML private TableColumn<Category, Long> idColumn;
    @FXML private TableColumn<Category, String> nameColumn;
    @FXML private TableColumn<Category, String> descriptionColumn;

    @FXML private TextField nameField;
    @FXML private TextArea descriptionArea;

    private final CategoryService categoryService;
    private ObservableList<Category> categoryList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        idColumn.setCellValueFactory(data -> new SimpleLongProperty(data.getValue().getId()).asObject());
        nameColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getName()));
        descriptionColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getDescription()));

        refreshTable();
    }

    @FXML
    private void addCategory() {
        try {
            String name = nameField.getText().trim();
            String description = descriptionArea.getText().trim();
            
            if (name.isEmpty()) {
                showAlert("Erreur", "Le nom de la catégorie est obligatoire");
                return;
            }
            
            Category category = new Category(name);
            category.setDescription(description);
            
            categoryService.createCategory(category);
            clearFields();
            refreshTable();
            
        } catch (Exception e) {
            showAlert("Erreur", "Impossible d'ajouter la catégorie: " + e.getMessage());
        }
    }

    @FXML
    private void deleteCategory() {
        Category selected = categoryTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Confirmation");
            confirm.setContentText("Voulez-vous vraiment supprimer cette catégorie ?");
            Optional<ButtonType> result = confirm.showAndWait();
            
            if (result.isPresent() && result.get() == ButtonType.OK) {
                try {
                    categoryService.deleteCategory(selected.getId());
                    refreshTable();
                } catch (Exception e) {
                    showAlert("Erreur", "Impossible de supprimer la catégorie: " + e.getMessage());
                }
            }
        } else {
            showAlert("Erreur", "Veuillez sélectionner une catégorie à supprimer");
        }
    }
    
    @FXML
    private void editCategory() {
        Category selected = categoryTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            nameField.setText(selected.getName());
            descriptionArea.setText(selected.getDescription());
        }
    }

    private void refreshTable() {
        categoryList.setAll(categoryService.getAllCategories());
        categoryTable.setItems(categoryList);
    }
    
    private void clearFields() {
        nameField.clear();
        descriptionArea.clear();
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.showAndWait();
    }
}