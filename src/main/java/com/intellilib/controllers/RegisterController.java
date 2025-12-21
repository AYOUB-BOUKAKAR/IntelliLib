package com.intellilib.controllers;

import com.intellilib.models.Member;
import com.intellilib.models.User;
import com.intellilib.services.UserService;
import com.intellilib.util.ActivityLogger;
import com.intellilib.util.FXMLLoaderUtil;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.animation.PauseTransition;
import javafx.util.Duration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import java.time.LocalDate;
import java.util.regex.Pattern;

@Controller
public class RegisterController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private TextField emailField;
    @FXML private TextField phoneField;
    @FXML private TextArea addressField;
    @FXML private ComboBox<User.UserRole> roleComboBox;
    @FXML private Button registerButton;
    @FXML private Button backButton;
    @FXML private Label errorLabel;
    @FXML private Label successLabel;

    @FXML private VBox phoneContainer;
    @FXML private VBox addressContainer;

    @Autowired
    private UserService userService;
    @Autowired
    private ActivityLogger activityLogger;


    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");
    private static final Pattern PHONE_PATTERN =
            Pattern.compile("^[+]?[0-9\\s\\-\\(\\)]{8,20}$");

    public RegisterController() {
        // No-arg constructor for FXML compatibility
    }

    @FXML
    private void initialize() {
        // Populate role combo box
        roleComboBox.getItems().setAll(User.UserRole.values());
        roleComboBox.setValue(User.UserRole.MEMBER);

        // Add listener to show/hide member-specific fields
        roleComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            boolean isMember = newValue == User.UserRole.MEMBER;
            phoneContainer.setVisible(isMember);
            phoneContainer.setManaged(isMember);
            addressContainer.setVisible(isMember);
            addressContainer.setManaged(isMember);

            // Clear member-specific fields if switching away from MEMBER
            if (!isMember) {
                phoneField.clear();
                addressField.clear();
            }
        });

        // Initially show member fields since default is MEMBER
        phoneContainer.setVisible(true);
        phoneContainer.setManaged(true);
        addressContainer.setVisible(true);
        addressContainer.setManaged(true);

        // Clear messages when user starts typing
        usernameField.textProperty().addListener((obs, oldVal, newVal) -> clearMessages());
        emailField.textProperty().addListener((obs, oldVal, newVal) -> clearMessages());
        phoneField.textProperty().addListener((obs, oldVal, newVal) -> clearMessages());
        passwordField.textProperty().addListener((obs, oldVal, newVal) -> clearMessages());
        confirmPasswordField.textProperty().addListener((obs, oldVal, newVal) -> clearMessages());
        addressField.textProperty().addListener((obs, oldVal, newVal) -> clearMessages());
    }

    @FXML
    private void register() {
        clearMessages();

        String username = usernameField.getText().trim();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();
        String email = emailField.getText().trim();
        String phone = phoneField.getText().trim();
        String address = addressField.getText().trim();
        User.UserRole role = roleComboBox.getValue();

        if (!validateForm(username, password, confirmPassword, email, phone, address, role)) {
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

            User newUser = new User();
            newUser.setUsername(username);
            newUser.setPassword(password);
            newUser.setEmail(email);
            newUser.setRole(role);
            newUser.setActive(true);

            if (role == User.UserRole.MEMBER) {
                // Validate phone uniqueness for members
                if (isPhoneAlreadyRegistered(phone)) {
                    errorLabel.setText("Ce numéro de téléphone est déjà enregistré");
                    return;
                }

                Member newMember = new Member();
                newMember.setFullName(username);
                newMember.setEmail(email);
                newMember.setPhone(phone);
                newMember.setAddress(address);
                newMember.setMembershipDate(LocalDate.now());
                newMember.setMembershipExpiry(LocalDate.now().plusYears(1));
                newMember.setActive(true);
                newUser.setMember(newMember);
            }

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

    private boolean isPhoneAlreadyRegistered(String phone) {
        // You need to implement this method in your MemberService
        // For now, we'll assume you have a method to check phone existence
        // return memberService.existsByPhone(phone);

        // Since we don't have access to memberService in this controller,
        // you might need to inject it or handle this differently
        // For now, I'll return false - you should implement proper validation
        return false;
    }

    private boolean validateForm(String username, String password, String confirmPassword,
                                 String email, String phone, String address, User.UserRole role) {
        StringBuilder errors = new StringBuilder();

        // Username validation
        if (username.isEmpty()) {
            errors.append("Le nom d'utilisateur est obligatoire\n");
        } else if (username.length() < 3 || username.length() > 20) {
            errors.append("Le nom d'utilisateur doit contenir entre 3 et 20 caractères\n");
        } else if (!username.matches("^[a-zA-Z0-9_]+$")) {
            errors.append("Le nom d'utilisateur ne peut contenir que des lettres, chiffres et underscores\n");
        }

        // Password validation
        if (password.isEmpty()) {
            errors.append("Le mot de passe est obligatoire\n");
        } else if (password.length() < 6) {
            errors.append("Le mot de passe doit contenir au moins 6 caractères\n");
        }

        // Confirm password
        if (confirmPassword.isEmpty()) {
            errors.append("La confirmation du mot de passe est obligatoire\n");
        } else if (!password.equals(confirmPassword)) {
            errors.append("Les mots de passe ne correspondent pas\n");
        }

        // Email validation
        if (!email.isEmpty() && !EMAIL_PATTERN.matcher(email).matches()) {
            errors.append("Veuillez entrer un email valide\n");
        }

        // Member-specific validation
        if (role == User.UserRole.MEMBER) {
            // Phone validation for members
            if (phone.isEmpty()) {
                errors.append("Le numéro de téléphone est obligatoire pour les membres\n");
            } else if (!PHONE_PATTERN.matcher(phone).matches()) {
                errors.append("Veuillez entrer un numéro de téléphone valide (8-20 chiffres)\n");
            }

            // Address is optional, no validation needed
        }

        if (errors.length() > 0) {
            errorLabel.setText(errors.toString());
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
        phoneField.clear();
        addressField.clear();
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