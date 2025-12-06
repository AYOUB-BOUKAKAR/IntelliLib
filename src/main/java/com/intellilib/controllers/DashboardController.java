package com.intellilib.controllers;

import com.intellilib.services.*;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class DashboardController {

    @FXML private Label totalAdminsLabel;
    @FXML private Label totalBooksLabel;
    @FXML private Label totalMembersLabel;
    @FXML private Label totalCategoriesLabel;
    @FXML private Label totalBorrowsLabel;

    private AdminService adminService = new AdminService();
    private BookService bookService = new BookService();
    private MemberService memberService = new MemberService();
    private CategoryService categoryService = new CategoryService();
    private BorrowService borrowService = new BorrowService();

    @FXML
    public void initialize() {
        totalAdminsLabel.setText(String.valueOf(adminService.getAllAdmins().size()));
        totalBooksLabel.setText(String.valueOf(bookService.getAllBooks().size()));
        totalMembersLabel.setText(String.valueOf(memberService.getAllMembers().size()));
        totalCategoriesLabel.setText(String.valueOf(categoryService.getAllCategories().size()));
        totalBorrowsLabel.setText(String.valueOf(borrowService.getAllBorrows().size()));
    }
}