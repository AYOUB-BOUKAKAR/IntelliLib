package com.intellilib.controllers;

import com.intellilib.models.Member;
import com.intellilib.services.MemberService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class MemberController {

    @FXML private TableView<Member> memberTable;
    @FXML private TableColumn<Member, Long> idColumn;
    @FXML private TableColumn<Member, String> nameColumn;
    @FXML private TableColumn<Member, String> emailColumn;

    @FXML private TextField nameField, emailField, phoneField;

    private MemberService memberService = new MemberService();
    private ObservableList<Member> memberList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        idColumn.setCellValueFactory(data -> javafx.beans.property.SimpleLongProperty.longProperty(data.getValue().getId()).asObject());
        nameColumn.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getName()));
        emailColumn.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getEmail()));

        refreshTable();
    }

    @FXML
    private void addMember() {
        String name = nameField.getText();
        String email = emailField.getText();
        String phone = phoneField.getText();
        memberService.createMember(new Member(name, email, phone));
        refreshTable();
    }

    @FXML
    private void deleteMember() {
        Member selected = memberTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            memberService.deleteMember(selected.getId());
            refreshTable();
        }
    }

    private void refreshTable() {
        memberList.setAll(memberService.getAllMembers());
        memberTable.setItems(memberList);
    }
}
