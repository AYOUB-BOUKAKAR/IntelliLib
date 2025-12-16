package com.intellilib.controllers.admin;

import com.intellilib.models.Category;
import com.intellilib.services.CategoryService;
import com.intellilib.session.SessionManager;
import com.intellilib.util.ActivityLogger;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

@Controller
public class ManageCategoryController {

    @FXML private TableView<Category> categoryTable;
    @FXML private TableColumn<Category, String> nameColumn;
    @FXML private TableColumn<Category, String> descriptionColumn;
    
    @FXML private TextField nameField;
    @FXML private TextArea descriptionField;
    @FXML private TextField searchField;
    
    @FXML private Button addButton;
    @FXML private Button updateButton;
    @FXML private Button deleteButton;
    @FXML private Button clearButton;
    
    @FXML private Label statusLabel;
    
    private final CategoryService categoryService;
    private final SessionManager sessionManager;
    private final ActivityLogger activityLogger;
    private final ObservableList<Category> categoryList = FXCollections.observableArrayList();
    
    @Autowired
    public ManageCategoryController(CategoryService categoryService, SessionManager sessionManager, ActivityLogger activityLogger) {
        this.categoryService = categoryService;
        this.sessionManager = sessionManager;
        this.activityLogger = activityLogger;
    }
    
    @FXML
    public void initialize() {
        setupTableColumns();
        loadCategories();
        setupTableSelection();
        setupSearch();
        disableForm(false);
        updateButton.setDisable(true);
        deleteButton.setDisable(true);
    }
    
    private void setupTableColumns() {
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
    }
    
    private void loadCategories() {
        categoryList.setAll(categoryService.getAllCategories());
        categoryTable.setItems(categoryList);
    }
    
    private void setupTableSelection() {
        categoryTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
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
        FilteredList<Category> filteredData = new FilteredList<>(categoryList, p -> true);
        
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(category -> {
                if (newValue == null || newValue.isEmpty()) {
                    return true;
                }
                
                String lowerCaseFilter = newValue.toLowerCase();
                return category.getName().toLowerCase().contains(lowerCaseFilter) ||
                       (category.getDescription() != null && 
                        category.getDescription().toLowerCase().contains(lowerCaseFilter));
            });
        });
        
        SortedList<Category> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(categoryTable.comparatorProperty());
        categoryTable.setItems(sortedData);
    }
    
    @FXML
    private void handleAddCategory() {
        if (validateInput()) {
            try {
                Category category = createCategoryFromForm();
                categoryService.createCategory(category);
                activityLogger.logCategoryAdd(sessionManager.getCurrentUser(), category.getName());
                loadCategories();
                clearForm();
                showStatus("Category added successfully!");
            } catch (Exception e) {
                showStatus("Error: " + e.getMessage());
            }
        }
    }
    
    @FXML
    private void handleUpdateCategory() {
        Category selectedCategory = categoryTable.getSelectionModel().getSelectedItem();
        if (selectedCategory != null && validateInput()) {
            try {
                updateCategoryFromForm(selectedCategory);
                categoryService.updateCategory(selectedCategory.getId(), selectedCategory);
                activityLogger.logCategoryUpdate(sessionManager.getCurrentUser(), selectedCategory.getName());
                loadCategories();
                showStatus("Category updated successfully!");
            } catch (Exception e) {
                showStatus("Error: " + e.getMessage());
            }
        }
    }
    
    @FXML
    private void handleDeleteCategory() {
        Category selectedCategory = categoryTable.getSelectionModel().getSelectedItem();
        if (selectedCategory != null) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Delete Category");
            alert.setHeaderText("Delete " + selectedCategory.getName());
            alert.setContentText("Are you sure you want to delete this category?");
            
            alert.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    try {
                        categoryService.deleteCategory(selectedCategory.getId());
                        activityLogger.logCategoryDelete(sessionManager.getCurrentUser(), selectedCategory.getName());
                        loadCategories();
                        clearForm();
                        showStatus("Category deleted successfully!");
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
        categoryTable.getSelectionModel().clearSelection();
    }
    
    private void populateForm(Category category) {
        nameField.setText(category.getName());
        descriptionField.setText(category.getDescription());
        disableForm(false);
    }
    
    private void clearForm() {
        nameField.clear();
        descriptionField.clear();
        disableForm(false);
        addButton.setDisable(false);
        updateButton.setDisable(true);
        deleteButton.setDisable(true);
    }
    
    private void disableForm(boolean disable) {
        nameField.setDisable(disable);
        descriptionField.setDisable(disable);
    }
    
    private Category createCategoryFromForm() {
        Category category = new Category();
        updateCategoryFromForm(category);
        return category;
    }
    
    private void updateCategoryFromForm(Category category) {
        category.setName(nameField.getText().trim());
        category.setDescription(descriptionField.getText().trim());
    }
    
    private boolean validateInput() {
        StringBuilder errorMessage = new StringBuilder();
        
        if (nameField.getText() == null || nameField.getText().trim().isEmpty()) {
            errorMessage.append("Category name is required!\n");
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