// package com.intellilib.controllers;

// import com.intellilib.models.User;
// import com.intellilib.services.AdminService;
// import com.intellilib.util.FXMLLoaderUtil;
// import javafx.collections.FXCollections;
// import javafx.collections.ObservableList;
// import javafx.fxml.FXML;
// import javafx.scene.control.*;
// import javafx.scene.control.cell.PropertyValueFactory;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.stereotype.Controller;

// import java.util.Optional;

// @Controller
// public class UserController {

//     @Autowired
//     private AdminService adminService;

//     // Table controls
//     @FXML private TableView<User> userTable;
//     @FXML private TableColumn<User, Long> idColumn;
//     @FXML private TableColumn<User, String> usernameColumn;
//     @FXML private TableColumn<User, String> emailColumn;
//     @FXML private TableColumn<User, String> roleColumn;
//     @FXML private TableColumn<User, Boolean> activeColumn;

//     // Form controls
//     @FXML private TextField usernameField;
//     @FXML private PasswordField passwordField;
//     @FXML private TextField emailField;
//     @FXML private ComboBox<User.UserRole> roleCombo;
//     @FXML private CheckBox activeCheckbox;

//     @FXML private TextField searchField;
    
//     private ObservableList<User> userList = FXCollections.observableArrayList();
//     private ObservableList<User.UserRole> roleList = FXCollections.observableArrayList();

//     public UserController() {
//         // No-arg constructor for FXML compatibility
//     }

//     @FXML
//     public void initialize() {
//         setupTableColumns();
//         setupComboBoxes();
//         loadUsers();
//     }

//     private void setupTableColumns() {
//         idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
//         usernameColumn.setCellValueFactory(new PropertyValueFactory<>("username"));
//         emailColumn.setCellValueFactory(new PropertyValueFactory<>("email"));
//         roleColumn.setCellValueFactory(new PropertyValueFactory<>("role"));
//         activeColumn.setCellValueFactory(new PropertyValueFactory<>("active"));
        
//         // Make active column show checkboxes
//         activeColumn.setCellFactory(col -> new TableCell<User, Boolean>() {
//             private final CheckBox checkBox = new CheckBox();
            
//             @Override
//             protected void updateItem(Boolean active, boolean empty) {
//                 super.updateItem(active, empty);
//                 if (empty || active == null) {
//                     setGraphic(null);
//                 } else {
//                     checkBox.setSelected(active);
//                     setGraphic(checkBox);
//                 }
//             }
//         });
//     }

//     private void setupComboBoxes() {
//         roleList.setAll(User.UserRole.values());
//         roleCombo.setItems(roleList);
//         roleCombo.getSelectionModel().selectFirst();
//     }

//     @FXML
//     private void loadUsers() {
//         try {
//             userList.setAll(adminService.getAllUsers());
//             userTable.setItems(userList);
//         } catch (Exception e) {
//             showAlert("Erreur", "Impossible de charger les utilisateurs: " + e.getMessage());
//         }
//     }

//     @FXML
//     private void searchUsers() {
//         String keyword = searchField.getText().trim().toLowerCase();
//         if (keyword.isEmpty()) {
//             loadUsers();
//             return;
//         }
        
//         try {
//             var filtered = userList.stream()
//                 .filter(user -> user.getUsername().toLowerCase().contains(keyword) ||
//                                user.getEmail().toLowerCase().contains(keyword))
//                 .toList();
//             userTable.setItems(FXCollections.observableArrayList(filtered));
//         } catch (Exception e) {
//             showAlert("Erreur", "Impossible de rechercher les utilisateurs: " + e.getMessage());
//         }
//     }

//     @FXML
//     private void createUser() {
//         try {
//             String username = usernameField.getText().trim();
//             String password = passwordField.getText();
//             String email = emailField.getText().trim();
//             User.UserRole role = roleCombo.getValue();
//             boolean active = activeCheckbox.isSelected();

//             if (username.isEmpty() || password.isEmpty() || email.isEmpty() || role == null) {
//                 showAlert("Erreur", "Tous les champs sont obligatoires");
//                 return;
//             }

//             User user;
//             switch (role) {
//                 case ADMIN:
//                     user = adminService.createAdmin(username, password, email);
//                     break;
//                 case LIBRARIAN:
//                     user = adminService.createLibrarian(username, password, email);
//                     break;
//                 case MEMBER:
//                     user = adminService.createMemberUser(username, password, email);
//                     break;
//                 default:
//                     throw new IllegalArgumentException("Rôle invalide");
//             }
            
//             if (user != null) {
//                 if (!active) {
//                     adminService.deactivateUser(user.getId());
//                 }
//                 showAlert("Succès", "Utilisateur créé avec succès!");
//                 clearForm();
//                 loadUsers();
//             }
//         } catch (Exception e) {
//             showAlert("Erreur", "Impossible de créer l'utilisateur: " + e.getMessage());
//         }
//     }

//     @FXML
//     private void updateUser() {
//         User selected = userTable.getSelectionModel().getSelectedItem();
//         if (selected == null) {
//             showAlert("Erreur", "Veuillez sélectionner un utilisateur à modifier");
//             return;
//         }

//         try {
//             String username = usernameField.getText().trim();
//             String password = passwordField.getText();
//             String email = emailField.getText().trim();
//             User.UserRole role = roleCombo.getValue();
//             boolean active = activeCheckbox.isSelected();

//             if (username.isEmpty() || email.isEmpty() || role == null) {
//                 showAlert("Erreur", "Tous les champs sont obligatoires (sauf mot de passe)");
//                 return;
//             }

//             // Update user info
//             selected.setUsername(username);
//             selected.setEmail(email);
//             selected.setRole(role);
            
//             // Only update password if provided
//             if (!password.isEmpty()) {
//                 selected.setPassword(password);
//             }
            
//             // Update active status
//             if (selected.isActive() != active) {
//                 if (active) {
//                     adminService.activateUser(selected.getId());
//                 } else {
//                     adminService.deactivateUser(selected.getId());
//                 }
//             }
            
//             User updated = adminService.saveUser(selected);
//             if (updated != null) {
//                 showAlert("Succès", "Utilisateur modifié avec succès!");
//                 clearForm();
//                 loadUsers();
//             }
//         } catch (Exception e) {
//             showAlert("Erreur", "Impossible de modifier l'utilisateur: " + e.getMessage());
//         }
//     }

//     @FXML
//     private void deleteUser() {
//         User selected = userTable.getSelectionModel().getSelectedItem();
//         if (selected == null) {
//             showAlert("Erreur", "Veuillez sélectionner un utilisateur à supprimer");
//             return;
//         }

//         // Don't allow self-deletion or deleting last admin
//         try {
//             Optional<User> currentUser = adminService.getCurrentUser();
//             if (currentUser.isPresent() && currentUser.get().getId().equals(selected.getId())) {
//                 showAlert("Erreur", "Vous ne pouvez pas supprimer votre propre compte");
//                 return;
//             }

//             Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
//             confirm.setTitle("Confirmation");
//             confirm.setHeaderText("Supprimer l'utilisateur");
//             confirm.setContentText("Êtes-vous sûr de vouloir supprimer l'utilisateur '" + 
//                                  selected.getUsername() + "' ?");
            
//             Optional<ButtonType> result = confirm.showAndWait();
//             if (result.isPresent() && result.get() == ButtonType.OK) {
//                 adminService.deleteUser(selected.getId());
//                 showAlert("Succès", "Utilisateur supprimé avec succès!");
//                 loadUsers();
//             }
//         } catch (Exception e) {
//             showAlert("Erreur", "Impossible de supprimer l'utilisateur: " + e.getMessage());
//         }
//     }

//     @FXML
//     private void toggleUserStatus() {
//         User selected = userTable.getSelectionModel().getSelectedItem();
//         if (selected == null) {
//             showAlert("Erreur", "Veuillez sélectionner un utilisateur");
//             return;
//         }

//         try {
//             if (selected.isActive()) {
//                 adminService.deactivateUser(selected.getId());
//                 showAlert("Succès", "Utilisateur désactivé avec succès!");
//             } else {
//                 adminService.activateUser(selected.getId());
//                 showAlert("Succès", "Utilisateur activé avec succès!");
//             }
//             loadUsers();
//         } catch (Exception e) {
//             showAlert("Erreur", "Impossible de modifier le statut: " + e.getMessage());
//         }
//     }

//     @FXML
//     private void loadUserForEdit() {
//         User selected = userTable.getSelectionModel().getSelectedItem();
//         if (selected != null) {
//             usernameField.setText(selected.getUsername());
//             emailField.setText(selected.getEmail());
//             roleCombo.setValue(selected.getRole());
//             activeCheckbox.setSelected(selected.isActive());
//             passwordField.clear(); // Clear password field for security
//         }
//     }

//     @FXML
//     private void clearForm() {
//         usernameField.clear();
//         passwordField.clear();
//         emailField.clear();
//         roleCombo.getSelectionModel().selectFirst();
//         activeCheckbox.setSelected(true);
//         userTable.getSelectionModel().clearSelection();
//     }

//     @FXML
//     private void resetPassword() {
//         User selected = userTable.getSelectionModel().getSelectedItem();
//         if (selected == null) {
//             showAlert("Erreur", "Veuillez sélectionner un utilisateur");
//             return;
//         }

//         TextInputDialog dialog = new TextInputDialog();
//         dialog.setTitle("Réinitialiser le mot de passe");
//         dialog.setHeaderText("Réinitialisation du mot de passe pour " + selected.getUsername());
//         dialog.setContentText("Nouveau mot de passe:");

//         Optional<String> result = dialog.showAndWait();
//         result.ifPresent(newPassword -> {
//             try {
//                 adminService.changePassword(selected.getId(), newPassword);
//                 showAlert("Succès", "Mot de passe modifié avec succès!");
//             } catch (Exception e) {
//                 showAlert("Erreur", "Impossible de modifier le mot de passe: " + e.getMessage());
//             }
//         });
//     }

//     @FXML
//     private void exportUsers() {
//         try {
//             // Simple export to console - you can enhance this to export to CSV/Excel
//             System.out.println("=== LISTE DES UTILISATEURS ===");
//             System.out.printf("%-5s %-20s %-30s %-15s %s%n", 
//                 "ID", "Username", "Email", "Rôle", "Actif");
//             System.out.println("------------------------------------------------------------------");
            
//             for (User user : adminService.getAllUsers()) {
//                 System.out.printf("%-5d %-20s %-30s %-15s %s%n",
//                     user.getId(),
//                     user.getUsername(),
//                     user.getEmail(),
//                     user.getRole(),
//                     user.isActive() ? "Oui" : "Non");
//             }
            
//             showAlert("Export", "Liste des utilisateurs exportée dans la console");
//         } catch (Exception e) {
//             showAlert("Erreur", "Impossible d'exporter les utilisateurs: " + e.getMessage());
//         }
//     }

//     private void showAlert(String title, String message) {
//         Alert alert = new Alert(Alert.AlertType.INFORMATION);
//         alert.setTitle(title);
//         alert.setHeaderText(null);
//         alert.setContentText(message);
//         alert.showAndWait();
//     }

//     @FXML
//     private void showError(String title, String message) {
//         Alert alert = new Alert(Alert.AlertType.ERROR);
//         alert.setTitle(title);
//         alert.setHeaderText(null);
//         alert.setContentText(message);
//         alert.showAndWait();
//     }

//     // Statistics methods
//     @FXML
//     private void showUserStats() {
//         try {
//             long totalUsers = adminService.getAllUsers().size();
//             long activeUsers = adminService.getAllUsers().stream()
//                     .filter(User::isActive)
//                     .count();
//             long admins = adminService.getAllUsers().stream()
//                     .filter(user -> user.getRole() == User.UserRole.ADMIN)
//                     .count();
//             long librarians = adminService.getAllUsers().stream()
//                     .filter(user -> user.getRole() == User.UserRole.LIBRARIAN)
//                     .count();
//             long members = adminService.getAllUsers().stream()
//                     .filter(user -> user.getRole() == User.UserRole.MEMBER)
//                     .count();

//             String stats = String.format(
//                 "Statistiques des utilisateurs:\n\n" +
//                 "Total: %d\n" +
//                 "Actifs: %d\n" +
//                 "Inactifs: %d\n\n" +
//                 "Par rôle:\n" +
//                 "  Administrateurs: %d\n" +
//                 "  Bibliothécaires: %d\n" +
//                 "  Membres: %d",
//                 totalUsers, activeUsers, totalUsers - activeUsers,
//                 admins, librarians, members
//             );

//             Alert alert = new Alert(Alert.AlertType.INFORMATION);
//             alert.setTitle("Statistiques");
//             alert.setHeaderText("Répartition des utilisateurs");
//             alert.setContentText(stats);
//             alert.showAndWait();
//         } catch (Exception e) {
//             showError("Erreur", "Impossible de charger les statistiques: " + e.getMessage());
//         }
//     }
// }