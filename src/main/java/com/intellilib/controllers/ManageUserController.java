package com.intellilib.controllers;

import com.intellilib.models.User;
import com.intellilib.services.UserService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import java.time.format.DateTimeFormatter;

@Controller
public class ManageUserController {

    @FXML private TableView<User> userTable;
    @FXML private TableColumn<User, Long> idColumn;
    @FXML private TableColumn<User, String> usernameColumn;
    @FXML private TableColumn<User, String> emailColumn;
    @FXML private TableColumn<User, String> roleColumn;
    @FXML private TableColumn<User, Boolean> activeColumn;
    @FXML private TableColumn<User, String> lastLoginColumn;
    @FXML private TableColumn<User, String> createdAtColumn;
    
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private TextField emailField;
    @FXML private ComboBox<User.UserRole> roleComboBox;
    @FXML private CheckBox activeCheckBox;
    @FXML private TextField searchField;
    
    @FXML private Button addButton;
    @FXML private Button updateButton;
    @FXML private Button deleteButton;
    @FXML private Button clearButton;
    @FXML private Button toggleStatusButton;
    
    @FXML private Label statusLabel;
    
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final ObservableList<User> userList = FXCollections.observableArrayList();
    private User currentUser;
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    
    @Autowired
    public ManageUserController(UserService userService, PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
    }
    
    @FXML
    public void initialize() {
        setupTableColumns();
        setupRoleComboBox();
        loadUsers();
        setupTableSelection();
        setupSearch();
        disableForm(false);
        updateButton.setDisable(true);
        deleteButton.setDisable(true);
        toggleStatusButton.setDisable(true);
    }
    
    private void setupTableColumns() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        usernameColumn.setCellValueFactory(new PropertyValueFactory<>("username"));
        emailColumn.setCellValueFactory(new PropertyValueFactory<>("email"));
        roleColumn.setCellValueFactory(new PropertyValueFactory<>("role"));
        activeColumn.setCellValueFactory(new PropertyValueFactory<>("active"));
        
        // Format date columns
        lastLoginColumn.setCellValueFactory(cellData -> {
            var lastLogin = cellData.getValue().getLastLogin();
            return javafx.beans.binding.Bindings.createStringBinding(() -> 
                lastLogin != null ? lastLogin.format(dateFormatter) : "Never"
            );
        });
        
        createdAtColumn.setCellValueFactory(cellData -> {
            var createdAt = cellData.getValue().getCreatedAt();
            return javafx.beans.binding.Bindings.createStringBinding(() -> 
                createdAt != null ? createdAt.format(dateFormatter) : ""
            );
        });
    }
    
    private void setupRoleComboBox() {
        roleComboBox.setItems(FXCollections.observableArrayList(
            User.UserRole.ADMIN,
            User.UserRole.LIBRARIAN,
            User.UserRole.MEMBER
        ));
        roleComboBox.getSelectionModel().selectFirst();
    }
    
    private void loadUsers() {
        userList.setAll(userService.getAllUsers());
        userTable.setItems(userList);
    }
    
    private void setupTableSelection() {
        userTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                populateForm(newSelection);
                addButton.setDisable(true);
                updateButton.setDisable(false);
                deleteButton.setDisable(false);
                toggleStatusButton.setDisable(false);
            } else {
                clearForm();
                addButton.setDisable(false);
                updateButton.setDisable(true);
                deleteButton.setDisable(true);
                toggleStatusButton.setDisable(true);
            }
        });
    }
    
    private void setupSearch() {
        FilteredList<User> filteredData = new FilteredList<>(userList, p -> true);
        
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(user -> {
                if (newValue == null || newValue.isEmpty()) {
                    return true;
                }
                
                String lowerCaseFilter = newValue.toLowerCase();
                
                if (user.getUsername().toLowerCase().contains(lowerCaseFilter)) {
                    return true;
                } else if (user.getEmail().toLowerCase().contains(lowerCaseFilter)) {
                    return true;
                } else if (user.getRole().toString().toLowerCase().contains(lowerCaseFilter)) {
                    return true;
                }
                return false;
            });
        });
        
        SortedList<User> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(userTable.comparatorProperty());
        userTable.setItems(sortedData);
    }
    
    @FXML
    private void handleAddUser() {
        if (validateInput()) {
            try {
                User user = createUserFromForm();
                userService.registerUser(user);
                loadUsers();
                clearForm();
                showStatus("User added successfully!", false);
            } catch (Exception e) {
                showStatus("Error adding user: " + e.getMessage(), true);
            }
        }
    }
    
    @FXML
    private void handleUpdateUser() {
        User selectedUser = userTable.getSelectionModel().getSelectedItem();
        if (selectedUser != null && validateInput()) {
            try {
                User userDetails = createUserFromForm();
                userDetails.setId(selectedUser.getId());
                userDetails.setCreatedAt(selectedUser.getCreatedAt());
                userDetails.setLastLogin(selectedUser.getLastLogin());
                
                // If password field is empty, keep existing password
                if (passwordField.getText() == null || passwordField.getText().trim().isEmpty()) {
                    userDetails.setPassword(selectedUser.getPassword());
                } else {
                    userDetails.setPassword(passwordEncoder.encode(passwordField.getText()));
                }
                userService.updateUser(userDetails);
                loadUsers();
                clearForm();
                showStatus("User updated successfully!", false);
            } catch (Exception e) {
                showStatus("Error updating user: " + e.getMessage(), true);
            }
        }
    }
    
    @FXML
    private void handleDeleteUser() {
        User selectedUser = userTable.getSelectionModel().getSelectedItem();
        if (selectedUser != null) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Delete User");
            alert.setHeaderText("Delete " + selectedUser.getUsername());
            alert.setContentText("Are you sure you want to delete this user? This action cannot be undone.");
            
            alert.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    try {
                        userService.logout();
                        userService.deleteUser(selectedUser.getId());
                        loadUsers();
                        clearForm();
                        showStatus("User deleted successfully!", false);
                    } catch (Exception e) {
                        showStatus("Error deleting user: " + e.getMessage(), true);
                    }
                }
            });
        }
    }
    
    @FXML
    private void handleToggleStatus() {
        User selectedUser = userTable.getSelectionModel().getSelectedItem();
        if (selectedUser != null) {
            try {
                selectedUser.setActive(!selectedUser.isActive());
                userService.updateUser(selectedUser);
                loadUsers();
                showStatus("User status updated!", false);
            } catch (Exception e) {
                showStatus("Error updating user status: " + e.getMessage(), true);
            }
        }
    }
    
    @FXML
    private void handleClearForm() {
        clearForm();
        userTable.getSelectionModel().clearSelection();
    }
    
    private void populateForm(User user) {
        currentUser = user;
        
        usernameField.setText(user.getUsername());
        passwordField.clear(); // Don't show password
        emailField.setText(user.getEmail());
        roleComboBox.getSelectionModel().select(user.getRole());
        activeCheckBox.setSelected(user.isActive());
        disableForm(false);
    }
    
    private void clearForm() {
        usernameField.clear();
        passwordField.clear();
        emailField.clear();
        roleComboBox.getSelectionModel().selectFirst();
        activeCheckBox.setSelected(true);
        disableForm(false);
        addButton.setDisable(false);
        updateButton.setDisable(true);
        deleteButton.setDisable(true);
        toggleStatusButton.setDisable(true);
        statusLabel.setText("");
    }
    
    private void disableForm(boolean disable) {
        usernameField.setDisable(disable);
        passwordField.setDisable(disable);
        emailField.setDisable(disable);
        roleComboBox.setDisable(disable);
        activeCheckBox.setDisable(disable);
    }
    
    private User createUserFromForm() {
        User user = new User();
        user.setUsername(usernameField.getText().trim());
        
        // Only set password if provided (for updates)
        if (passwordField.getText() != null && !passwordField.getText().trim().isEmpty()) {
            user.setPassword(passwordField.getText().trim());
        }
        
        user.setEmail(emailField.getText().trim());
        user.setRole(roleComboBox.getValue());
        user.setActive(activeCheckBox.isSelected());
        
        return user;
    }
    
    private boolean validateInput() {
        StringBuilder errorMessage = new StringBuilder();
        
        if (usernameField.getText() == null || usernameField.getText().trim().isEmpty()) {
            errorMessage.append("Username is required!\n");
        } else if (usernameField.getText().trim().length() < 3) {
            errorMessage.append("Username must be at least 3 characters!\n");
        }
        
        // Only validate password for new users (when adding)
        if (addButton.isDisable() == false && 
            (passwordField.getText() == null || passwordField.getText().trim().isEmpty())) {
            errorMessage.append("Password is required for new users!\n");
        } else if (passwordField.getText() != null && 
                   passwordField.getText().trim().length() < 6) {
            errorMessage.append("Password must be at least 6 characters!\n");
        }
        
        if (emailField.getText() == null || emailField.getText().trim().isEmpty()) {
            errorMessage.append("Email is required!\n");
        } else if (!emailField.getText().trim().matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            errorMessage.append("Please enter a valid email address!\n");
        }
        
        if (roleComboBox.getValue() == null) {
            errorMessage.append("Role is required!\n");
        }
        
        // Check for duplicate username (only when adding new user)
        if (addButton.isDisable() == false && 
            userService.usernameExists(usernameField.getText().trim())) {
            errorMessage.append("Username already exists!\n");
        }
        
        // Check for duplicate email (only when adding new user)
        if (addButton.isDisable() == false && 
            userService.emailExists(emailField.getText().trim())) {
            errorMessage.append("Email already exists!\n");
        }
        
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
}