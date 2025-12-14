package com.intellilib.controllers.member;

import com.intellilib.models.User;
import com.intellilib.services.UserService;
import com.intellilib.services.CategoryService;
import com.intellilib.util.FXMLLoaderUtil;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;

import java.time.format.DateTimeFormatter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Controller
public class UpdateProfileController {

    @FXML private TextField usernameField;
    @FXML private TextField emailField;
    @FXML private TextField fullNameField;
    @FXML private TextField phoneField;
    @FXML private TextArea addressField;

    @FXML private PasswordField currentPasswordField;
    @FXML private PasswordField newPasswordField;
    @FXML private PasswordField confirmPasswordField;

    @FXML private CheckBox emailNotificationsCheck;
    @FXML private ComboBox<String> languageCombo;
    @FXML private ListView<String> preferredCategoriesList;

    @FXML private Label registrationDateLabel;
    @FXML private Label lastLoginLabel;
    @FXML private Label accountStatusLabel;
    @FXML private Label statusLabel;

    @FXML private VBox statusContainer;
    @FXML private ProgressIndicator progressIndicator;

    private final UserService userService;
    private final CategoryService categoryService;
    private final PasswordEncoder passwordEncoder;

    private User currentUser;
    private ObservableList<String> categories = FXCollections.observableArrayList();
    private ObservableList<String> languages = FXCollections.observableArrayList("Français", "Anglais", "Arabe");

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public UpdateProfileController(UserService userService, CategoryService categoryService,
                                 PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.categoryService = categoryService;
        this.passwordEncoder = passwordEncoder;
    }

    @FXML
    public void initialize() {
        currentUser = userService.getCurrentUser();
        if (currentUser == null) {
            showError("Erreur", "Utilisateur non connecté");
            goBack();
            return;
        }

        loadUserData();
        setupForm();
    }

    private void loadUserData() {
        // Load user information
        usernameField.setText(currentUser.getUsername());
        emailField.setText(currentUser.getEmail());
        // Note: fullName, phone, address might not exist in User model
        // You might need to extend your User model or create a separate UserProfile entity

        // Load account info
        if (currentUser.getCreatedAt() != null) {
            registrationDateLabel.setText(currentUser.getCreatedAt()
                .format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        }

        if (currentUser.getLastLogin() != null) {
            lastLoginLabel.setText(currentUser.getLastLogin()
                .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
        }

        accountStatusLabel.setText(currentUser.isActive() ? "Actif" : "Inactif");
    }

    private void setupForm() {
        // Setup language combo
        languageCombo.setItems(languages);
        languageCombo.setValue("Français");

        // Load categories for preferences
        executor.submit(() -> {
            try {
                var allCategories = categoryService.getAllCategories();
                Platform.runLater(() -> {
                    categories.setAll(allCategories.stream()
                        .map(category -> category.getName())
                        .toList());
                    preferredCategoriesList.setItems(categories);
                });
            } catch (Exception e) {
                Platform.runLater(() ->
                    showStatus("Erreur lors du chargement des catégories", true));
            }
        });
    }

    @FXML
    private void saveProfile() {
        if (!validateForm()) {
            return;
        }

        showProgress(true);
        showStatus("Enregistrement en cours...", false);

        executor.submit(() -> {
            try {
                Thread.sleep(1000); // Simulate processing

                Platform.runLater(() -> {
                    // Update user information
                    currentUser.setEmail(emailField.getText().trim());
                    // Update other fields if they exist in your User model

                    // Check if password should be changed
                    String currentPassword = currentPasswordField.getText();
                    String newPassword = newPasswordField.getText();

                    if (!currentPassword.isEmpty() && !newPassword.isEmpty()) {
                        // Verify current password
                        if (!passwordEncoder.matches(currentPassword, currentUser.getPassword())) {
                            showStatus("Mot de passe actuel incorrect", true);
                            showProgress(false);
                            return;
                        }

                        // Update password
                        String encryptedPassword = passwordEncoder.encode(newPassword);
                        currentUser.setPassword(encryptedPassword);
                    }

                    // Save user (you'll need to implement updateUser in UserService)
                    // userService.updateUser(currentUser);

                    showProgress(false);
                    showSuccess("Succès", "Profil mis à jour avec succès !");

                    // Clear password fields
                    currentPasswordField.clear();
                    newPasswordField.clear();
                    confirmPasswordField.clear();
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    showProgress(false);
                    showStatus("Erreur: " + e.getMessage(), true);
                });
            }
        });
    }

    private boolean validateForm() {
        StringBuilder errors = new StringBuilder();

        // Email validation
        String email = emailField.getText().trim();
        if (email.isEmpty()) {
            errors.append("L'email est obligatoire\n");
        } else if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            errors.append("Email invalide\n");
        }

        // Password validation if changing password
        String newPassword = newPasswordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        if (!newPassword.isEmpty()) {
            if (currentPasswordField.getText().isEmpty()) {
                errors.append("Le mot de passe actuel est requis pour changer le mot de passe\n");
            }

            if (newPassword.length() < 6) {
                errors.append("Le nouveau mot de passe doit contenir au moins 6 caractères\n");
            }

            if (!newPassword.equals(confirmPassword)) {
                errors.append("Les mots de passe ne correspondent pas\n");
            }
        }

        if (errors.length() > 0) {
            showStatus(errors.toString(), true);
            return false;
        }

        return true;
    }

    @FXML
    private void resetForm() {
        loadUserData();
        currentPasswordField.clear();
        newPasswordField.clear();
        confirmPasswordField.clear();
        emailNotificationsCheck.setSelected(false);
        languageCombo.setValue("Français");
        preferredCategoriesList.getSelectionModel().clearSelection();
        showStatus("Formulaire réinitialisé", false);
    }

    @FXML
    private void deleteAccount() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Supprimer le compte");
        confirm.setHeaderText("Suppression définitive du compte");
        confirm.setContentText("Êtes-vous sûr de vouloir supprimer votre compte ?\n" +
                             "Cette action est irréversible et supprimera toutes vos données.");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                Alert passwordDialog = new Alert(Alert.AlertType.CONFIRMATION);
                passwordDialog.setTitle("Confirmation finale");
                passwordDialog.setHeaderText("Entrez votre mot de passe pour confirmer");

                PasswordField passwordField = new PasswordField();
                passwordField.setPromptText("Mot de passe");

                passwordDialog.getDialogPane().setContent(passwordField);

                passwordDialog.showAndWait().ifPresent(response2 -> {
                    if (response2 == ButtonType.OK) {
                        String password = passwordField.getText();

                        // Verify password
                        if (passwordEncoder.matches(password, currentUser.getPassword())) {
                            // Delete account (you'll need to implement deleteUser in UserService)
                            // userService.deleteUser(currentUser.getId());
                            showSuccess("Compte supprimé", "Votre compte a été supprimé avec succès.");
                            logout();
                        } else {
                            showError("Erreur", "Mot de passe incorrect");
                        }
                    }
                });
            }
        });
    }

    private void logout() {
        try {
            userService.logout();
            Stage stage = (Stage) usernameField.getScene().getWindow();
            stage.setScene(FXMLLoaderUtil.loadScene("/views/login.fxml"));
            stage.setTitle("Connexion");
        } catch (Exception e) {
            showError("Erreur", "Impossible de se déconnecter");
        }
    }

    @FXML
    private void goBack() {
        try {
            Stage stage = (Stage) usernameField.getScene().getWindow();
            stage.setScene(FXMLLoaderUtil.loadScene("/views/member-dashboard.fxml"));
            stage.setTitle("Tableau de Bord Membre");
        } catch (Exception e) {
            showError("Erreur", "Impossible de retourner au tableau de bord");
        }
    }

    private void showProgress(boolean show) {
        progressIndicator.setVisible(show);
        statusContainer.setDisable(show);
    }

    private void showStatus(String message, boolean isError) {
        statusLabel.setText(message);
        if (isError) {
            statusLabel.getStyleClass().removeAll("success");
            statusLabel.getStyleClass().add("error");
        } else {
            statusLabel.getStyleClass().removeAll("error");
            statusLabel.getStyleClass().add("success");
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