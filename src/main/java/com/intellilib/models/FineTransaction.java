package com.intellilib.models;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "fine_transactions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FineTransaction {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;
    
    @ManyToOne
    @JoinColumn(name = "borrow_id")
    private Borrow borrow;
    
    @Column(nullable = false)
    private Double amount;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentMethod paymentMethod;
    
    @Column(name = "transaction_date", nullable = false)
    private LocalDateTime transactionDate = LocalDateTime.now();
    
    @Column(name = "payment_reference")
    private String paymentReference;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionStatus status = TransactionStatus.COMPLETED;
    
    @Column(columnDefinition = "TEXT")
    private String notes;
    
    @ManyToOne
    @JoinColumn(name = "processed_by")
    private User processedBy;
    
    @Column(name = "receipt_number")
    private String receiptNumber;
    
    public enum PaymentMethod {
        CASH, CREDIT_CARD, DEBIT_CARD, BANK_TRANSFER, ONLINE, WAIVED, OTHER
    }
    
    public enum TransactionStatus {
        PENDING, COMPLETED, FAILED, REFUNDED, CANCELLED
    }
    
    // Generate receipt number
    @PrePersist
    public void generateReceiptNumber() {
        if (this.receiptNumber == null) {
            this.receiptNumber = "FINE-" + 
                LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd")) + 
                "-" + String.format("%06d", this.id != null ? this.id : 999999);
        }
    }
}