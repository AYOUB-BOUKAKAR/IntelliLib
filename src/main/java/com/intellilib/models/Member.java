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
    
    // Custom constructor
    public Member(String fullName, String email, String phone) {
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.membershipDate = LocalDate.now();
        this.active = true;
    }
}