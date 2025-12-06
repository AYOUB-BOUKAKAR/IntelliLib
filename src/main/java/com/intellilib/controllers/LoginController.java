package com.intellilib.controllers;

import com.intellilib.models.Admin;
import com.intellilib.services.AdminService;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.fxml.FXMLLoader;

public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Button loginButton;
    @FXML private Label errorLabel;

    private AdminService adminService = new AdminService();

    @FXML
    private void login() {
        String username = usernameField.getText();
        String password = passwordField.getText();

        Admin admin = adminService.authenticate(username, password);
        if (admin != null) {
            errorLabel.setText("");
            openDashboard();
            // fermer la fenêtre login
            loginButton.getScene().getWindow().hide();
        } else {
            errorLabel.setText("Nom d'utilisateur ou mot de passe incorrect");
        }
    }

    private void openDashboard() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Dashboard.fxml"));
            Scene scene = new Scene(loader.load());
            Stage stage = new Stage();
            stage.setTitle("Tableau de Bord");
            stage.setScene(scene);
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
