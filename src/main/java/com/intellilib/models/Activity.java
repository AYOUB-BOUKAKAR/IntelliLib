package com.intellilib.models;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "activities")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Activity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(foreignKeyDefinition = "FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE"))
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private User user;

    @Column(nullable = false)
    private String action;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Long timestamp = System.currentTimeMillis();  // CHANGED: Use Long instead of LocalDateTime

    @Column(name = "ip_address")
    private String ipAddress;

    public Activity(User user, String action, String description, String ipAddress) {
        this.user = user;
        this.action = action;
        this.description = description;
        this.ipAddress = ipAddress;
        this.timestamp = System.currentTimeMillis();  // Set timestamp on creation
    }
}