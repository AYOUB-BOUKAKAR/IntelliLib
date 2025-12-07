package com.intellilib.models;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "borrows")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Borrow {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;
    
    @ManyToOne
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;
    
    @Column(name = "borrow_date", nullable = false)
    private LocalDate borrowDate = LocalDate.now();
    
    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;
    
    @Column(name = "return_date")
    private LocalDate returnDate;
    
    private boolean returned = false;
    
    private Double fineAmount = 0.0;
    
    // Custom constructor
    public Borrow(Book book, Member member, LocalDate dueDate) {
        this.book = book;
        this.member = member;
        this.borrowDate = LocalDate.now();
        this.dueDate = dueDate;
        this.returned = false;
        this.fineAmount = 0.0;
    }
}