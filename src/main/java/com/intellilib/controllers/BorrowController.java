package com.intellilib.controllers;

import com.intellilib.models.Borrow;
import com.intellilib.models.Book;
import com.intellilib.models.Member;
import com.intellilib.services.BorrowService;
import com.intellilib.services.BookService;
import com.intellilib.services.MemberService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import java.time.LocalDate;

public class BorrowController {

    @FXML private TableView<Borrow> borrowTable;
    @FXML private TableColumn<Borrow, Long> idColumn;
    @FXML private TableColumn<Borrow, String> bookColumn;
    @FXML private TableColumn<Borrow, String> memberColumn;

    @FXML private ComboBox<Book> bookCombo;
    @FXML private ComboBox<Member> memberCombo;
    @FXML private DatePicker borrowDatePicker, returnDatePicker;

    private BorrowService borrowService = new BorrowService();
    private BookService bookService = new BookService();
    private MemberService memberService = new MemberService();
    private ObservableList<Borrow> borrowList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        idColumn.setCellValueFactory(data -> javafx.beans.property.SimpleLongProperty.longProperty(data.getValue().getId()).asObject());
        bookColumn.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getBook().getTitle()));
        memberColumn.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getMember().getName()));

        bookCombo.setItems(FXCollections.observableArrayList(bookService.getAllBooks()));
        memberCombo.setItems(FXCollections.observableArrayList(memberService.getAllMembers()));

        refreshTable();
    }

    @FXML
    private void addBorrow() {
        Book book = bookCombo.getValue();
        Member member = memberCombo.getValue();
        LocalDate borrowDate = borrowDatePicker.getValue();
        LocalDate returnDate = returnDatePicker.getValue();
        if (book != null && member != null && borrowDate != null && returnDate != null) {
            borrowService.createBorrow(new Borrow(book, member, borrowDate, returnDate));
            refreshTable();
        }
    }

    @FXML
    private void deleteBorrow() {
        Borrow selected = borrowTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            borrowService.deleteBorrow(selected.getId());
            refreshTable();
        }
    }

    private void refreshTable() {
        borrowList.setAll(borrowService.getAllBorrows());
        borrowTable.setItems(borrowList);
    }
}
