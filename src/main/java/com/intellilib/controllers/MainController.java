package com.intellilib.controllers;

import com.intellilib.util.FXMLLoaderUtil;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.stage.Stage;
import org.springframework.stereotype.Controller;

import java.io.IOException;

@Controller
public class MainController {
    
    @FXML private Button loginButton;
    @FXML private Button registerButton;
    
    public MainController() {
        // No-arg constructor for FXML compatibility
    }
    
    @FXML
    public void initialize() {
        // Initialization code if needed
    }
    
    @FXML
    public void showLogin() {
        try {
            Stage stage = (Stage) loginButton.getScene().getWindow();
            stage.setScene(FXMLLoaderUtil.loadScene("/views/login.fxml"));
            stage.setTitle("Connexion");
        } catch (IOException e) {
            e.printStackTrace();
            showError("Erreur", "Impossible de charger la page de connexion: " + e.getMessage());
        }
    }

    @FXML
    public void showRegister() {
        try {
            Stage stage = (Stage) registerButton.getScene().getWindow();
            stage.setScene(FXMLLoaderUtil.loadScene("/views/register.fxml"));
            stage.setTitle("Inscription");
        } catch (IOException e) {
            e.printStackTrace();
            showError("Erreur", "Impossible de charger la page d'inscription: " + e.getMessage());
        }
    }
    
    private void showError(String title, String message) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}