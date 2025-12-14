package com.intellilib.controllers;

import com.intellilib.models.Member;
import com.intellilib.services.MemberService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import java.time.LocalDate;

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
    
    @FXML private Label statusLabel;
    
    private final MemberService memberService;
    private final ObservableList<Member> memberList = FXCollections.observableArrayList();
    
    @Autowired
    public ManageMemberController(MemberService memberService) {
        this.memberService = memberService;
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
    private void handleAddMember() {
        if (validateInput()) {
            try {
                Member member = createMemberFromForm();
                memberService.createMember(member);
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
        member.setFullName(nameField.getText().trim());
        member.setEmail(emailField.getText().trim());
        member.setPhone(phoneField.getText().trim());
        member.setAddress(addressField.getText().trim());
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
