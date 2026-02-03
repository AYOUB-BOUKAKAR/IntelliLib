package com.intellilib.controllers.admin;

import com.intellilib.models.Member;
import com.intellilib.models.User;
import com.intellilib.services.MemberService;
import com.intellilib.services.UserService;
import com.intellilib.session.SessionManager;
import com.intellilib.util.ActivityLogger;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import java.time.LocalDate;
import java.util.Optional;

@Controller
public class ManageMemberController {

    @FXML private TableView<Member> memberTable;
    @FXML private TableColumn<Member, String> nameColumn;
    @FXML private TableColumn<Member, String> emailColumn;
    @FXML private TableColumn<Member, String> phoneColumn;
    @FXML private TableColumn<Member, String> addressColumn;
    @FXML private TableColumn<Member, String> membershipDateColumn;
    @FXML private TableColumn<Member, String> membershipExpiryColumn;
    @FXML private TableColumn<Member, String> statusColumn;
    @FXML private TableColumn<Member, String> hasAccountColumn;
    
    @FXML private TextField nameField;
    @FXML private TextField emailField;
    @FXML private TextField phoneField;
    @FXML private TextField addressField;
    @FXML private DatePicker membershipDateField;
    @FXML private DatePicker expiryDateField;
    @FXML private CheckBox activeCheckBox;
    @FXML private TextField searchField;
    
    @FXML private Button addButton;
    @FXML private Button updateButton;
    @FXML private Button deleteButton;
    @FXML private Button clearButton;
    @FXML private Button createAccountButton;
    
    @FXML private Label statusLabel;
    
    private final MemberService memberService;
    private final UserService userService;
    private final ActivityLogger activityLogger;
    private final SessionManager sessionManager;
    private final ObservableList<Member> memberList = FXCollections.observableArrayList();
    
    @Autowired
    public ManageMemberController(MemberService memberService, UserService userService, ActivityLogger activityLogger, SessionManager sessionManager) {
        this.memberService = memberService;
        this.userService = userService;
        this.activityLogger = activityLogger;
        this.sessionManager = sessionManager;
    }
    
    @FXML
    public void initialize() {
        setupTableColumns();
        loadMembers();
        setupTableSelection();
        setupSearch();
        disableForm(false);
        updateButton.setDisable(true);
        deleteButton.setDisable(true);
        
        // Set default membership date to today
        membershipDateField.setValue(LocalDate.now());
    }
    
    private void setupTableColumns() {
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        emailColumn.setCellValueFactory(new PropertyValueFactory<>("email"));
        phoneColumn.setCellValueFactory(new PropertyValueFactory<>("phone"));
        addressColumn.setCellValueFactory(new PropertyValueFactory<>("address"));
        membershipDateColumn.setCellValueFactory(new PropertyValueFactory<>("membershipDate"));
        membershipExpiryColumn.setCellValueFactory(new PropertyValueFactory<>("membershipExpiry"));
        statusColumn.setCellValueFactory(cellData -> 
            cellData.getValue().isActive() ? 
                new javafx.beans.property.SimpleStringProperty("Active") : 
                new javafx.beans.property.SimpleStringProperty("Inactive")
        );
        hasAccountColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(
                        cellData.getValue().hasUserAccount() ? "Yes" : "No"
                )
        );
    }
    
    private void loadMembers() {
        memberList.setAll(memberService.getAllMembers());
        memberTable.setItems(memberList);
    }
    
    private void setupTableSelection() {
        memberTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
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
        FilteredList<Member> filteredData = new FilteredList<>(memberList, p -> true);
        
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(member -> {
                if (newValue == null || newValue.isEmpty()) {
                    return true;
                }
                
                String lowerCaseFilter = newValue.toLowerCase();
                
                return member.getFullName().toLowerCase().contains(lowerCaseFilter) ||
                       member.getEmail().toLowerCase().contains(lowerCaseFilter) ||
                       member.getPhone().contains(lowerCaseFilter);
            });
        });
        
        SortedList<Member> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(memberTable.comparatorProperty());
        memberTable.setItems(sortedData);
    }

    @FXML
    private void handleCreateUserAccount() {
        Member selectedMember = memberTable.getSelectionModel().getSelectedItem();
        if (selectedMember == null) {
            showStatus("Please select a member first!");
            return;
        }

        // Check if member already has user account
        if (selectedMember.hasUserAccount()) {
            showStatus("Member already has a user account!");
            return;
        }

        // Create the custom dialog
        Dialog<UserCredentials> dialog = new Dialog<>();
        dialog.setTitle("Create User Account");
        dialog.setHeaderText("Enter user account details for " + selectedMember.getFullName());

        // Set the button types
        ButtonType createButtonType = new ButtonType("Create", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(createButtonType, ButtonType.CANCEL);

        // Create the username and password fields
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField emailField = new TextField(selectedMember.getEmail());
        emailField.setPromptText("Email");
        TextField usernameField = new TextField(selectedMember.getEmail().substring(0, selectedMember.getEmail().indexOf('@')));
        usernameField.setPromptText("Username");
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Password");

        grid.add(new Label("Email:"), 0, 0);
        grid.add(emailField, 1, 0);
        grid.add(new Label("Username:"), 0, 1);
        grid.add(usernameField, 1, 1);
        grid.add(new Label("Password:"), 0, 2);
        grid.add(passwordField, 1, 2);

        dialog.getDialogPane().setContent(grid);

        // Request focus on the username field by default
        Platform.runLater(usernameField::requestFocus);

        // Convert the result to a username-password-pair when the create button is clicked
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == createButtonType) {
                return new UserCredentials(
                        emailField.getText().trim(),
                        usernameField.getText().trim(),
                        passwordField.getText()
                );
            }
            return null;
        });

        // Show the dialog and wait for user input
        Optional<UserCredentials> result = dialog.showAndWait();

        result.ifPresent(credentials -> {
            try {
                // Validate input
                if (credentials.email().isEmpty() || credentials.username().isEmpty() || credentials.password().isEmpty()) {
                    showStatus("All fields are required!");
                    return;
                }

                // Use the new service method
                userService.createUserAccountForMember(
                        credentials.username(),
                        credentials.password(),
                        credentials.email(),
                        selectedMember.getId()
                );

                showStatus("User account created successfully for " + selectedMember.getFullName());
                loadMembers(); // Refresh the table
            } catch (Exception e) {
                showStatus("Error creating user account: " + e.getMessage());
            }
        });
    }

    // Helper record to store user credentials
    private record UserCredentials(String email, String username, String password) {}
    
    @FXML
    private void handleAddMember() {
        if (validateInput()) {
            try {
                Member member = createMemberFromForm();
                memberService.createMember(member);
                activityLogger.logMemberAdd(sessionManager.getCurrentUser(), member.getFullName());
                loadMembers();
                clearForm();
                showStatus("Member added successfully!");
            } catch (Exception e) {
                showStatus("Error: " + e.getMessage());
            }
        }
    }
    
    @FXML
    private void handleUpdateMember() {
        Member selectedMember = memberTable.getSelectionModel().getSelectedItem();
        if (selectedMember != null && validateInput()) {
            try {
                updateMemberFromForm(selectedMember);
                memberService.updateMember(selectedMember.getId(), selectedMember);
                activityLogger.logMemberUpdate(sessionManager.getCurrentUser(), selectedMember.getFullName());
                loadMembers();
                showStatus("Member updated successfully!");
            } catch (Exception e) {
                showStatus("Error: " + e.getMessage());
            }
        }
    }
    
    @FXML
    private void handleDeleteMember() {
        Member selectedMember = memberTable.getSelectionModel().getSelectedItem();
        if (selectedMember != null) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Delete Member");
            alert.setHeaderText("Delete " + selectedMember.getFullName());
            alert.setContentText("Are you sure you want to delete this member?");
            
            alert.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    try {
                        memberService.deleteMember(selectedMember.getId());
                        activityLogger.logMemberDelete(sessionManager.getCurrentUser(), selectedMember.getFullName());
                        loadMembers();
                        clearForm();
                        showStatus("Member deleted successfully!");
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
        memberTable.getSelectionModel().clearSelection();
    }
    
    private void populateForm(Member member) {
        nameField.setText(member.getFullName());
        emailField.setText(member.getEmail());
        phoneField.setText(member.getPhone());
        addressField.setText(member.getAddress());
        membershipDateField.setValue(member.getMembershipDate());
        expiryDateField.setValue(member.getMembershipExpiry());
        activeCheckBox.setSelected(member.isActive());
        disableForm(false);
    }
    
    private void clearForm() {
        nameField.clear();
        emailField.clear();
        phoneField.clear();
        addressField.clear();
        membershipDateField.setValue(LocalDate.now());
        expiryDateField.setValue(null);
        activeCheckBox.setSelected(true);
        disableForm(false);
        addButton.setDisable(false);
        updateButton.setDisable(true);
        deleteButton.setDisable(true);
    }
    
    private void disableForm(boolean disable) {
        nameField.setDisable(disable);
        emailField.setDisable(disable);
        phoneField.setDisable(disable);
        addressField.setDisable(disable);
        membershipDateField.setDisable(disable);
        expiryDateField.setDisable(disable);
        activeCheckBox.setDisable(disable);
    }
    
    private Member createMemberFromForm() {
        Member member = new Member();
        updateMemberFromForm(member);
        return member;
    }
    
    private void updateMemberFromForm(Member member) {
        member.setFullName(nameField.getText() != null ? nameField.getText().trim() : "");
        member.setEmail(emailField.getText() != null ? emailField.getText().trim() : "");
        member.setPhone(phoneField.getText() != null ? phoneField.getText().trim() : null);
        member.setAddress(addressField.getText() != null ? addressField.getText().trim() : null);
        member.setMembershipDate(membershipDateField.getValue());
        member.setMembershipExpiry(expiryDateField.getValue());
        member.setActive(activeCheckBox.isSelected());
    }
    
    private boolean validateInput() {
        StringBuilder errorMessage = new StringBuilder();
        
        if (nameField.getText() == null || nameField.getText().trim().isEmpty()) {
            errorMessage.append("Name is required!\n");
        }
        
        if (emailField.getText() == null || emailField.getText().trim().isEmpty()) {
            errorMessage.append("Email is required!\n");
        } else if (!emailField.getText().matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            errorMessage.append("Please enter a valid email address!\n");
        }
        
        if (phoneField.getText() == null || phoneField.getText().trim().isEmpty()) {
            errorMessage.append("Phone number is required!\n");
        }
        
        if (membershipDateField.getValue() == null) {
            errorMessage.append("Membership date is required!\n");
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
