package com.intellilib.models;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "members")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Member {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "full_name", nullable = false)
    private String fullName;
    
    @Column(unique = true)
    private String email;
    
    @Column(unique = true)
    private String phone;
    
    private String address;
    
    @Column(name = "membership_date")
    private LocalDate membershipDate = LocalDate.now();
    
    @Column(name = "membership_expiry")
    private LocalDate membershipExpiry;
    
    private boolean active = true;
    
    // Fine and ban management fields
    @Column(name = "total_fines_paid")
    private Double totalFinesPaid = 0.0;
    
    @Column(name = "current_fines_due")
    private Double currentFinesDue = 0.0;
    
    @Column(name = "overdue_books_count")
    private Integer overdueBooksCount = 0;
    
    @Column(name = "is_banned")
    private Boolean isBanned = false;
    
    @Column(name = "ban_reason")
    private String banReason;
    
    @Column(name = "ban_start_date")
    private LocalDate banStartDate;
    
    @Column(name = "ban_end_date")
    private LocalDate banEndDate;
    
    @Column(name = "total_ban_count")
    private Integer totalBanCount = 0;
    
    @Column(name = "warning_count")
    private Integer warningCount = 0;
    
    @Column(name = "max_allowed_overdue_days")
    private Integer maxAllowedOverdueDays = 30;
    
    @Column(name = "credit_limit")
    private Double creditLimit = 50.0; // Maximum fines before additional restrictions
    
    // Custom constructor
    public Member(String fullName, String email, String phone) {
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.membershipDate = LocalDate.now();
        this.active = true;
        this.isBanned = false;
        this.currentFinesDue = 0.0;
        this.totalFinesPaid = 0.0;
        this.overdueBooksCount = 0;
        this.warningCount = 0;
        this.totalBanCount = 0;
    }
    
    // Helper method to check if member can borrow
    public boolean canBorrow() {
        return active && !isBanned && 
               currentFinesDue <= creditLimit &&
               overdueBooksCount < 3; // Max 3 overdue books
    }
    
    // Check if ban is expired
    public boolean isBanExpired() {
        if (!isBanned || banEndDate == null) {
            return true;
        }
        return LocalDate.now().isAfter(banEndDate);
    }

    public String toText () {
        return fullName + " - " + email;
    }
}