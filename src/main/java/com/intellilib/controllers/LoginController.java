package com.intellilib.controllers;

import com.intellilib.models.User;
import com.intellilib.services.UserService;
import com.intellilib.session.SessionManager;
import com.intellilib.util.ActivityLogger;
import com.intellilib.util.FXMLLoaderUtil;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

@Controller
public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Button loginButton;
    @FXML private Label errorLabel;

    @Autowired
    private UserService userService;
    @Autowired
    private SessionManager sessionManager;
    @Autowired
    private ActivityLogger activityLogger;

    public LoginController() {
        // No-arg constructor for FXML compatibility
    }

    @FXML
    private void initialize() {
        // Initialization code if needed
    }

    @FXML
    private void login() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            errorLabel.setText("Veuillez remplir tous les champs");
            return;
        }

        try {
            User user = userService.login(username, password);
            if (user != null) {
                if (!user.isActive()) {
                    errorLabel.setText("Ce compte est désactivé");
                    return;
                }
                
                errorLabel.setText("");
                sessionManager.login(user);
                activityLogger.logLogin(user);
                openDashboard(user);
            } else {
                errorLabel.setText("Nom d'utilisateur ou mot de passe incorrect");
            }
        } catch (Exception e) {
            errorLabel.setText("Erreur: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void openDashboard(User user) {
        try {
            String fxmlPath;
            String title;
            
            // Determine which dashboard to load based on user role
            switch (user.getRole()) {
                case ADMIN:
                    fxmlPath = "/views/admin-dashboard.fxml";
                    title = "Tableau de Bord Admin - IntelliLib";
                    break;
                case LIBRARIAN:
                    fxmlPath = "/views/librarian-dashboard.fxml";
                    title = "Tableau de Bord Bibliothécaire - IntelliLib";
                    break;
                case MEMBER:
                default:
                    fxmlPath = "/views/member-dashboard.fxml";
                    title = "Mon Tableau de Bord - IntelliLib";
                    break;
            }
            
            Stage stage = FXMLLoaderUtil.loadStage(fxmlPath, title, true);
            stage.show();
            
            // Close login window
            Stage loginStage = (Stage) loginButton.getScene().getWindow();
            loginStage.close();
            
        } catch (Exception e) {
            showError("Erreur", "Impossible d'ouvrir le tableau de bord");
            e.printStackTrace();
        }
    }
    
    @FXML
    private void backToMain() {
        try {
            Stage stage = (Stage) loginButton.getScene().getWindow();
            stage.setScene(FXMLLoaderUtil.loadScene("/views/main.fxml"));
            stage.setTitle("IntelliLib - Welcome");
        } catch (Exception e) {
            showError("Erreur", "Impossible de revenir à l'accueil");
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