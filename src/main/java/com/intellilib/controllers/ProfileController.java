package com.intellilib.controllers;

import com.intellilib.models.Member;
import com.intellilib.models.User;
import com.intellilib.services.UserService;
import com.intellilib.session.SessionManager;
import com.intellilib.util.AlertUtil;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

@Component
@RequiredArgsConstructor
public class ProfileController implements Initializable {

    private final UserService userService;
    private final SessionManager sessionManager;

    private User currentUser;

    // Header Section
    @FXML private Text pageTitle;
    @FXML private Text pageSubtitle;

    // Profile Info Section
    @FXML private Text usernameLabel;
    @FXML private Text emailLabel;
    @FXML private Text roleLabel;
    @FXML private Text accountStatusLabel;
    @FXML private Text memberIdLabel;
    @FXML private Text memberNameLabel;
    @FXML private Text memberPhoneLabel;
    @FXML private Text memberAddressLabel;
    @FXML private Text memberStatusLabel;
    @FXML private Text membershipDateLabel;
    @FXML private Text expiryDateLabel;
    @FXML private Text lastLoginLabel;
    @FXML private Text createdAtLabel;

    // Member Info Section
    @FXML private VBox memberInfoSection;

    // Success Message
    @FXML private Label successMessage;

    // Refresh Button
    @FXML private Button refreshButton;

    private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadUserProfile();
    }

    private void loadUserProfile() {
        currentUser = userService.getCurrentUser();
        if (currentUser == null) {
            AlertUtil.showError("Session Error", "No user logged in. Please login again.");
            return;
        }

        updateProfileDisplay();
    }

    private void updateProfileDisplay() {
        // User Information
        usernameLabel.setText(currentUser.getUsername());
        emailLabel.setText(currentUser.getEmail() != null ? currentUser.getEmail() : "N/A");
        roleLabel.setText(currentUser.getRole().toString());
        accountStatusLabel.setText(currentUser.isActive() ? "Active" : "Inactive");
        accountStatusLabel.getStyleClass().add(currentUser.isActive() ? "stat-value-success" : "stat-value-error");

        lastLoginLabel.setText(currentUser.getLastLogin() != null ?
                currentUser.getLastLogin().format(formatter) : "Never");
        createdAtLabel.setText(currentUser.getCreatedAt() != null ?
                currentUser.getCreatedAt().format(formatter) : "N/A");

        // Member Information (if exists)
        if (currentUser.getMember() != null) {
            Member member = currentUser.getMember();
            memberIdLabel.setText(member.getId() != null ? member.getId().toString() : "N/A");
            memberNameLabel.setText(member.getFullName() != null ? member.getFullName() : "N/A");
            memberPhoneLabel.setText(member.getPhone() != null ? member.getPhone() : "N/A");
            memberAddressLabel.setText(member.getAddress() != null ? member.getAddress() : "N/A");
            memberStatusLabel.setText(member.isActive() ? "Active" : "Inactive");
            memberStatusLabel.getStyleClass().add(member.isActive() ? "stat-value-success" : "stat-value-error");

            membershipDateLabel.setText(member.getMembershipDate() != null ?
                    member.getMembershipDate().toString() : "N/A");
            expiryDateLabel.setText(member.getMembershipExpiry() != null ?
                    member.getMembershipExpiry().toString() : "No expiry");

            // Show member section
            memberInfoSection.setVisible(true);
            memberInfoSection.setManaged(true);
        } else {
            // Hide member section
            memberInfoSection.setVisible(false);
            memberInfoSection.setManaged(false);
        }
    }

    @FXML
    private void handleRefresh() {
        loadUserProfile();
        clearMessages();
    }

    private void clearMessages() {
        successMessage.setText("");
    }
}