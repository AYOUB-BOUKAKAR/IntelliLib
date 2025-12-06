package com.intellilib.controllers;

import com.intellilib.models.Admin;
import com.intellilib.services.AdminService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class AdminController {

    @FXML private TableView<Admin> adminTable;
    @FXML private TableColumn<Admin, Long> idColumn;
    @FXML private TableColumn<Admin, String> usernameColumn;

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;

    private AdminService adminService = new AdminService();
    private ObservableList<Admin> adminList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        idColumn.setCellValueFactory(data -> javafx.beans.property.SimpleLongProperty
                .longProperty(data.getValue().getId()).asObject());
        usernameColumn.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getUsername()));

        refreshTable();
    }

    @FXML
    private void addAdmin() {
        String username = usernameField.getText();
        String password = passwordField.getText();
        if (!username.isEmpty() && !password.isEmpty()) {
            adminService.createAdmin(new Admin(username, password));
            refreshTable();
        }
    }

    @FXML
    private void deleteAdmin() {
        Admin selected = adminTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            adminService.deleteAdmin(selected.getId());
            refreshTable();
        }
    }

    private void refreshTable() {
        adminList.setAll(adminService.getAllAdmins());
        adminTable.setItems(adminList);
    }
}