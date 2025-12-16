package com.intellilib.controllers;

import com.intellilib.models.User;
import com.intellilib.services.UserService;
import com.intellilib.util.ActivityLogger;
import com.intellilib.util.FXMLLoaderUtil;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.animation.PauseTransition;
import javafx.util.Duration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import java.util.regex.Pattern;

@Controller
public class RegisterController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private TextField emailField;
    @FXML private ComboBox<User.UserRole> roleComboBox;
    @FXML private Button registerButton;
    @FXML private Button backButton;
    @FXML private Label errorLabel;
    @FXML private Label successLabel;

    @Autowired
    private UserService userService;
    @Autowired
    private ActivityLogger activityLogger;

    
    private static final Pattern EMAIL_PATTERN = 
        Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");
    
    public RegisterController() {
        // No-arg constructor for FXML compatibility
    }
    
    @FXML
    private void initialize() {
        // Populate role combo box
        roleComboBox.getItems().setAll(User.UserRole.values());
        roleComboBox.setValue(User.UserRole.MEMBER);
        
        // Clear messages when user starts typing
        usernameField.textProperty().addListener((obs, oldVal, newVal) -> clearMessages());
        emailField.textProperty().addListener((obs, oldVal, newVal) -> clearMessages());
        passwordField.textProperty().addListener((obs, oldVal, newVal) -> clearMessages());
        confirmPasswordField.textProperty().addListener((obs, oldVal, newVal) -> clearMessages());
    }
    
    @FXML
    private void register() {
        clearMessages();
        
        String username = usernameField.getText().trim();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();
        String email = emailField.getText().trim();
        User.UserRole role = roleComboBox.getValue();
        
        if (!validateForm(username, password, confirmPassword, email)) {
            return;
        }
        
        try {
            if (userService.usernameExists(username)) {
                errorLabel.setText("Ce nom d'utilisateur est déjà pris");
                return;
            }
            
            if (!email.isEmpty() && userService.emailExists(email)) {
                errorLabel.setText("Cet email est déjà utilisé");
                return;
            }
            
            User newUser = new User(username, password, email, role);
            User savedUser = userService.registerUser(newUser);
            activityLogger.logRegister(savedUser);
            
            if (savedUser != null) {
                successLabel.setText("Compte créé avec succès! Redirection...");
                clearForm();
                
                PauseTransition pause = new PauseTransition(Duration.seconds(2));
                pause.setOnFinished(e -> goToLogin());
                pause.play();
            } else {
                errorLabel.setText("Erreur lors de la création du compte");
            }
        } catch (Exception e) {
            errorLabel.setText("Erreur: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private boolean validateForm(String username, String password, String confirmPassword, String email) {
        if (username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            errorLabel.setText("Veuillez remplir tous les champs obligatoires");
            return false;
        }
        
        if (username.length() < 3 || username.length() > 20) {
            errorLabel.setText("Le nom d'utilisateur doit contenir entre 3 et 20 caractères");
            return false;
        }
        
        if (!username.matches("^[a-zA-Z0-9_]+$")) {
            errorLabel.setText("Le nom d'utilisateur ne peut contenir que des lettres, chiffres et underscores");
            return false;
        }
        
        if (password.length() < 6) {
            errorLabel.setText("Le mot de passe doit contenir au moins 6 caractères");
            return false;
        }
        
        if (!password.equals(confirmPassword)) {
            errorLabel.setText("Les mots de passe ne correspondent pas");
            return false;
        }
        
        if (!email.isEmpty() && !EMAIL_PATTERN.matcher(email).matches()) {
            errorLabel.setText("Veuillez entrer un email valide");
            return false;
        }
        
        return true;
    }
    
    @FXML
    private void goToLogin() {
        try {
            Stage stage = (Stage) backButton.getScene().getWindow();
            stage.setScene(FXMLLoaderUtil.loadScene("/views/login.fxml"));
            stage.setTitle("Connexion");
        } catch (Exception e) {
            showError("Erreur", "Impossible d'ouvrir la fenêtre de connexion");
            e.printStackTrace();
        }
    }
    
    private void clearForm() {
        usernameField.clear();
        passwordField.clear();
        confirmPasswordField.clear();
        emailField.clear();
        roleComboBox.setValue(User.UserRole.MEMBER);
    }
    
    private void clearMessages() {
        errorLabel.setText("");
        successLabel.setText("");
    }
    
    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}