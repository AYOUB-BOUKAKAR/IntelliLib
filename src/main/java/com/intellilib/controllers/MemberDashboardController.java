// package com.intellilib.controllers;

// import com.intellilib.models.User;
// import com.intellilib.services.UserService;
// import com.intellilib.services.BookService;
// import com.intellilib.services.BorrowService;
// import javafx.fxml.FXML;
// import javafx.scene.control.Label;
// import javafx.scene.control.TableColumn;
// import javafx.scene.control.TableView;
// import javafx.scene.control.cell.PropertyValueFactory;
// import org.springframework.stereotype.Controller;

// @Controller
// public class MemberDashboardController extends BaseDashboardController {
    
//     @FXML private Label welcomeLabel;
//     @FXML private Label activeBorrowingsLabel;
//     @FXML private Label overdueBooksLabel;
//     @FXML private Label finesLabel;
    
//     @FXML private TableView<?> recentBooksTable;
//     @FXML private TableColumn<?, ?> bookTitleColumn;
//     @FXML private TableColumn<?, ?> borrowDateColumn;
//     @FXML private TableColumn<?, ?> dueDateColumn;
//     @FXML private TableColumn<?, ?> statusColumn;
    
//     private final BookService bookService;
//     private final BorrowService BorrowService;
    
//     public MemberDashboardController(UserService userService, BookService bookService, BorrowService BorrowService) {
//         super(userService);
//         this.bookService = bookService;
//         this.BorrowService = BorrowService;
//     }
    
//     @Override
//     protected void loadDashboardData() {
//         if (currentUser == null) return;
        
//         // Set welcome message
//         welcomeLabel.setText("Bienvenue, " + currentUser.getUsername() + "!");
        
//         // Load member statistics
//         loadMemberStatistics();
        
//         // Load recent borrowings
//         loadRecentBorrowings();
//     }
    
//     private void loadMemberStatistics() {
//         try {
//             // Get active borrowings count
//             // long activeBorrowings = BorrowService.countActiveBorrowingsForMember(currentUser.getId());
//             activeBorrowingsLabel.setText(String.valueOf(activeBorrowings));
            
//             // Get overdue books count
//             // long overdueBooks = BorrowService.countOverdueBooksForMember(currentUser.getId());
//             overdueBooksLabel.setText(String.valueOf(overdueBooks));
            
//             // Calculate fines
//             // double fines = BorrowService.calculateFinesForMember(currentUser.getId());
//             finesLabel.setText(String.format("%.2f €", fines));
            
//         } catch (Exception e) {
//             showError("Erreur", "Impossible de charger les statistiques");
//             e.printStackTrace();
//         }
//     }
    
//     private void loadRecentBorrowings() {
//         try {
//             // Initialize table columns
//             bookTitleColumn.setCellValueFactory(new PropertyValueFactory<>("bookTitle"));
//             borrowDateColumn.setCellValueFactory(new PropertyValueFactory<>("borrowDate"));
//             dueDateColumn.setCellValueFactory(new PropertyValueFactory<>("dueDate"));
//             statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
            
//             // Load data
//             var recentBorrowings = BorrowService.getRecentBorrowingsForMember(currentUser.getId(), 10);
//             recentBooksTable.getItems().setAll(recentBorrowings);
            
//         } catch (Exception e) {
//             showError("Erreur", "Impossible de charger les emprunts récents");
//             e.printStackTrace();
//         }
//     }
    
//     @FXML
//     private void browseBooks() {
//         try {
//             Stage stage = FXMLLoaderUtil.loadStage("/views/browse-books.fxml", "Parcourir les Livres", true);
//             stage.show();
//         } catch (Exception e) {
//             showError("Erreur", "Impossible d'ouvrir le catalogue");
//             e.printStackTrace();
//         }
//     }
    
//     @FXML
//     private void viewMyBorrowings() {
//         try {
//             Stage stage = FXMLLoaderUtil.loadStage("/views/my-borrowings.fxml", "Mes Emprunts", true);
//             stage.show();
//         } catch (Exception e) {
//             showError("Erreur", "Impossible d'ouvrir mes emprunts");
//             e.printStackTrace();
//         }
//     }
    
//     @FXML
//     private void viewMyFines() {
//         try {
//             Stage stage = FXMLLoaderUtil.loadStage("/views/my-fines.fxml", "Mes Amendes", true);
//             stage.show();
//         } catch (Exception e) {
//             showError("Erreur", "Impossible d'ouvrir mes amendes");
//             e.printStackTrace();
//         }
//     }
    
//     @FXML
//     private void updateProfile() {
//         try {
//             Stage stage = FXMLLoaderUtil.loadStage("/views/update-profile.fxml", "Modifier Mon Profil", true);
//             stage.show();
//         } catch (Exception e) {
//             showError("Erreur", "Impossible d'ouvrir la modification du profil");
//             e.printStackTrace();
//         }
//     }
    
//     @FXML
//     private void logout() {
//         try {
//             // Clear current user
//             userService.logout();
            
//             // Go back to login
//             Stage stage = FXMLLoaderUtil.loadStage("/views/login.fxml", "Connexion - IntelliLib", false);
//             stage.show();
            
//             // Close dashboard
//             Stage dashboardStage = (Stage) welcomeLabel.getScene().getWindow();
//             dashboardStage.close();
            
//         } catch (Exception e) {
//             showError("Erreur", "Impossible de se déconnecter");
//             e.printStackTrace();
//         }
//     }
    
//     private void showError(String title, String message) {
//         Alert alert = new Alert(Alert.AlertType.ERROR);
//         alert.setTitle(title);
//         alert.setHeaderText(null);
//         alert.setContentText(message);
//         alert.showAndWait();
//     }
// }