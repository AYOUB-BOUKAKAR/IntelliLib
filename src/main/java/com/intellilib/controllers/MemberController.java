package com.intellilib.controllers;

import com.intellilib.models.Member;
import com.intellilib.services.MemberService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.SimpleBooleanProperty;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;

import java.util.Optional;

@Controller
@RequiredArgsConstructor
public class MemberController {

    @FXML private TableView<Member> memberTable;
    @FXML private TableColumn<Member, Long> idColumn;
    @FXML private TableColumn<Member, String> nameColumn;
    @FXML private TableColumn<Member, String> emailColumn;
    @FXML private TableColumn<Member, Boolean> activeColumn;

    @FXML private TextField nameField, emailField, phoneField, addressField;

    private final MemberService memberService;
    private ObservableList<Member> memberList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        idColumn.setCellValueFactory(data -> new SimpleLongProperty(data.getValue().getId()).asObject());
        nameColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getFullName()));
        emailColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getEmail()));
        activeColumn.setCellValueFactory(data -> new SimpleBooleanProperty(data.getValue().isActive()));

        refreshTable();
    }

    @FXML
    private void addMember() {
        try {
            String name = nameField.getText().trim();
            String email = emailField.getText().trim();
            String phone = phoneField.getText().trim();
            String address = addressField.getText().trim();
            
            if (name.isEmpty() || email.isEmpty()) {
                showAlert("Erreur", "Le nom et l'email sont obligatoires");
                return;
            }
            
            Member member = new Member(name, email, phone);
            member.setAddress(address);
            
            memberService.createMember(member);
            clearFields();
            refreshTable();
            
        } catch (Exception e) {
            showAlert("Erreur", "Impossible d'ajouter le membre: " + e.getMessage());
        }
    }

    @FXML
    private void deleteMember() {
        Member selected = memberTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Confirmation");
            confirm.setContentText("Voulez-vous vraiment supprimer ce membre ?");
            Optional<ButtonType> result = confirm.showAndWait();
            
            if (result.isPresent() && result.get() == ButtonType.OK) {
                try {
                    memberService.deleteMember(selected.getId());
                    refreshTable();
                } catch (Exception e) {
                    showAlert("Erreur", "Impossible de supprimer le membre: " + e.getMessage());
                }
            }
        } else {
            showAlert("Erreur", "Veuillez sélectionner un membre à supprimer");
        }
    }
    
    @FXML
    private void toggleMemberStatus() {
        Member selected = memberTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            try {
                if (selected.isActive()) {
                    memberService.deactivateMember(selected.getId());
                } else {
                    memberService.activateMember(selected.getId());
                }
                refreshTable();
            } catch (Exception e) {
                showAlert("Erreur", "Impossible de modifier le statut: " + e.getMessage());
            }
        }
    }

    private void refreshTable() {
        memberList.setAll(memberService.getAllMembers());
        memberTable.setItems(memberList);
    }
    
    private void clearFields() {
        nameField.clear();
        emailField.clear();
        phoneField.clear();
        addressField.clear();
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.showAndWait();
    }
}