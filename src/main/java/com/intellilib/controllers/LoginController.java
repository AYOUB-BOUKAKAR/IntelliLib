package com.intellilib.controllers;

import com.intellilib.models.User;
import com.intellilib.services.UserService;
import com.intellilib.util.FXMLLoaderUtil;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;

import java.util.Optional;

@Controller  // Changed from public class to @Controller
@RequiredArgsConstructor  // Spring will inject services via constructor
public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Button loginButton;
    @FXML private Label errorLabel;

    // Spring will inject these automatically
    private final UserService userService;
    
    // Remove manual service creation: private final AuthService authService = new AuthService();

    @FXML
    private void login() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            errorLabel.setText("Veuillez remplir tous les champs");
            return;
        }

        try {
            Optional<User> userOpt = userService.login(username, password);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                if (!user.isActive()) {
                    errorLabel.setText("Ce compte est désactivé");
                    return;
                }
                
                errorLabel.setText("");
                openDashboard();
                // Close login window
                ((Stage) loginButton.getScene().getWindow()).close();
            } else {
                errorLabel.setText("Nom d'utilisateur ou mot de passe incorrect");
            }
        } catch (Exception e) {
            errorLabel.setText("Une erreur est survenue lors de la connexion");
            e.printStackTrace();
        }
    }

    private void openDashboard() {
        try {
            // Use the Spring-aware FXMLLoader
            Stage stage = FXMLLoaderUtil.loadStage("/fxml/Dashboard.fxml", "Tableau de Bord - IntelliLib", true);
            stage.show();
            
            // Close login window after successful login
            Stage loginStage = (Stage) loginButton.getScene().getWindow();
            loginStage.close();
        } catch (Exception e) {
            showError("Erreur", "Impossible d'ouvrir le tableau de bord");
            e.printStackTrace();
        }
    }
    
    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}