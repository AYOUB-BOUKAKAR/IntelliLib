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
    
    // Fine management fields
    @Column(name = "fine_per_day")
    private Double finePerDay = 2.0; // Default daily fine rate
    
    @Column(name = "days_overdue")
    private Integer daysOverdue = 0;
    
    @Column(name = "fine_updated_date")
    private LocalDate fineUpdatedDate;
    
    @Column(name = "max_overdue_days")
    private Integer maxOverdueDays = 30;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "fine_status")
    private FineStatus fineStatus = FineStatus.NONE;
    
    @Column(name = "last_fine_calculation_date")
    private LocalDate lastFineCalculationDate;
    
    @Column(name = "is_fine_exempt")
    private Boolean isFineExempt = false;
    
    @Column(name = "fine_exemption_reason")
    private String fineExemptionReason;
    
    public enum FineStatus {
        NONE, PENDING, PAID, WAIVED, CANCELLED
    }
    
    // Custom constructor
    public Borrow(Book book, Member member, LocalDate dueDate) {
        this.book = book;
        this.member = member;
        this.borrowDate = LocalDate.now();
        this.dueDate = dueDate;
        this.returned = false;
        this.fineAmount = 0.0;
        this.daysOverdue = 0;
        this.fineStatus = FineStatus.NONE;
        this.fineUpdatedDate = LocalDate.now();
        this.lastFineCalculationDate = LocalDate.now();
        this.isFineExempt = false;
    }
    
    // Helper method to check if overdue
    public boolean isOverdue() {
        return !returned && LocalDate.now().isAfter(dueDate);
    }
    
    // Calculate days overdue
    public int calculateDaysOverdue() {
        if (returned || LocalDate.now().isBefore(dueDate) || isFineExempt) {
            return 0;
        }
        return Math.max(0, (int) java.time.temporal.ChronoUnit.DAYS.between(dueDate, LocalDate.now()));
    }
    
    // Calculate fine
    public double calculateFine() {
        if (returned || isFineExempt || fineStatus == FineStatus.PAID || fineStatus == FineStatus.WAIVED) {
            return 0.0;
        }
        
        int daysOverdue = calculateDaysOverdue();
        return daysOverdue * finePerDay;
    }
}