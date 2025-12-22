package com.intellilib.services;

import com.intellilib.models.*;
import com.intellilib.repositories.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.Builder;
import lombok.Data;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class FineService {
    
    private final BorrowRepository borrowRepository;
    private final MemberRepository memberRepository;
    private final FineTransactionRepository fineTransactionRepository;
    private final SystemSettingsRepository systemSettingsRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    
    // Default values (can be overridden by system settings)
    private static final double DEFAULT_FINE_PER_DAY = 2.0;
    private static final int DEFAULT_MAX_OVERDUE_DAYS = 30;
    private static final double DEFAULT_CREDIT_LIMIT = 50.0;
    
    /**
     * Scheduled job to calculate fines daily at 2 AM
     */
    @Scheduled(cron = "0 0 2 * * ?") // Runs at 2 AM every day
    @Transactional
    public void calculateDailyFines() {
        log.info("Starting daily fine calculation...");
        
        // Get active overdue borrows
        List<Borrow> overdueBorrows = borrowRepository.findOverdueBorrows(LocalDate.now());
        
        for (Borrow borrow : overdueBorrows) {
            try {
                updateFineForBorrow(borrow);
            } catch (Exception e) {
                log.error("Error calculating fine for borrow ID {}: {}", borrow.getId(), e.getMessage());
            }
        }
        
        // Check for bans
        checkAndApplyBans();
        
        log.info("Daily fine calculation completed. Processed {} borrows.", overdueBorrows.size());
    }
    
    /**
     * Update fine for a single borrow
     */
    @Transactional
    public void updateFineForBorrow(Borrow borrow) {
        if (borrow.isReturned() || borrow.getIsFineExempt()) {
            return;
        }
        
        // Calculate days overdue
        int daysOverdue = borrow.calculateDaysOverdue();
        
        if (daysOverdue <= 0) {
            return;
        }
        
        // Update borrow record
        borrow.setDaysOverdue(daysOverdue);
        
        // Calculate new fine
        Double newFine = daysOverdue * borrow.getFinePerDay();
        
        // Only update if fine has changed
        if (!newFine.equals(borrow.getFineAmount())) {
            double fineDifference = newFine - borrow.getFineAmount();
            
            // Update borrow
            borrow.setFineAmount(newFine);
            borrow.setFineUpdatedDate(LocalDate.now());
            borrow.setLastFineCalculationDate(LocalDate.now());
            
            if (borrow.getFineStatus() == Borrow.FineStatus.NONE) {
                borrow.setFineStatus(Borrow.FineStatus.PENDING);
            }
            
            // Update member's current fines due
            Member member = borrow.getMember();
            member.setCurrentFinesDue(member.getCurrentFinesDue() + fineDifference);
            
            // Update overdue books count
            if (daysOverdue > 0 && borrow.getDaysOverdue() == 0) {
                member.setOverdueBooksCount(member.getOverdueBooksCount() + 1);
            }
            
            borrowRepository.save(borrow);
            memberRepository.save(member);
            
            // Send notification if fine exceeds threshold
            if (member.getCurrentFinesDue() > getCreditLimit()) {
                notificationService.sendFineWarningNotification(member, borrow);
            }
            
            log.info("Updated fine for borrow ID {}: {} days overdue, fine: ${}", 
                    borrow.getId(), daysOverdue, newFine);
        }
    }
    
    /**
     * Check and apply bans for members exceeding max overdue days
     */
    @Transactional
    public void checkAndApplyBans() {
        int maxOverdueDays = getMaxOverdueDays();
        
        List<Borrow> severelyOverdue = borrowRepository.findBorrowsOverdueByDays(maxOverdueDays);
        
        for (Borrow borrow : severelyOverdue) {
            Member member = borrow.getMember();
            
            if (!member.getIsBanned()) {
                // Ban the member
                member.setIsBanned(true);
                member.setBanReason("Excessive overdue: Book '" + borrow.getBook().getTitle() + 
                                   "' overdue by " + borrow.getDaysOverdue() + " days");
                member.setBanStartDate(LocalDate.now());
                member.setBanEndDate(LocalDate.now().plusDays(30)); // 30 day ban
                member.setTotalBanCount(member.getTotalBanCount() + 1);
                
                memberRepository.save(member);
                
                // Send ban notification
                notificationService.sendBanNotification(member, borrow);
                
                log.warn("Member {} banned for excessive overdue on borrow ID {}", 
                        member.getId(), borrow.getId());
            }
        }
    }

    @Transactional
    public FineTransaction processFinePayment(Long borrowId, Double amount,
                                              FineTransaction.PaymentMethod paymentMethod,
                                              String paymentReference, String notes, Long userId) {

        Borrow borrow = borrowRepository.findById(borrowId)
                .orElseThrow(() -> new RuntimeException("Borrow not found"));

        if (borrow.getFineStatus() == Borrow.FineStatus.PAID) {
            throw new RuntimeException("Fine already paid");
        }

        if (borrow.getFineStatus() == Borrow.FineStatus.WAIVED) {
            throw new RuntimeException("Fine has been waived");
        }

        if (amount < borrow.getFineAmount()) {
            throw new RuntimeException("Payment amount less than fine amount");
        }

        // Create transaction
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        FineTransaction transaction = FineTransaction.builder()
                .borrow(borrow)
                .member(borrow.getMember())
                .amount(amount)
                .paymentMethod(paymentMethod)
                .transactionDate(LocalDateTime.now()) // Explicitly set transaction date
                .paymentReference(paymentReference != null ? paymentReference : "MANUAL-" + System.currentTimeMillis())
                .notes(notes)
                .processedBy(user)
                .status(FineTransaction.TransactionStatus.COMPLETED)
                .build();

        // Update borrow
        borrow.setFineStatus(Borrow.FineStatus.PAID);
        borrow.setFineAmount(0.0); // Reset fine after payment

        // Update member
        Member member = borrow.getMember();
        double oldFinesDue = member.getCurrentFinesDue();
        member.setCurrentFinesDue(Math.max(0, oldFinesDue - amount));
        member.setTotalFinesPaid(member.getTotalFinesPaid() + amount);

        // If this was the last overdue book, decrement count
        if (borrow.isOverdue()) {
            member.setOverdueBooksCount(Math.max(0, member.getOverdueBooksCount() - 1));
        }

        // Save all
        FineTransaction savedTransaction = fineTransactionRepository.save(transaction);
        borrowRepository.save(borrow);
        memberRepository.save(member);

        // Generate receipt (will be done by @PrePersist)
        if (notificationService != null) {
            notificationService.sendPaymentReceipt(member, savedTransaction);
        }

        log.info("Fine payment processed: Borrow ID={}, Amount=${}, Member={}",
                borrowId, amount, member.getFullName());

        return savedTransaction;
    }

    /**
     * Waive fine (admin only)
     */
    @Transactional
    public void waiveFine(Long borrowId, String reason, Long userId) {
        Borrow borrow = borrowRepository.findById(borrowId)
                .orElseThrow(() -> new RuntimeException("Borrow not found"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Create waived transaction
        FineTransaction transaction = FineTransaction.builder()
                .borrow(borrow)
                .member(borrow.getMember())
                .amount(borrow.getFineAmount())
                .paymentMethod(FineTransaction.PaymentMethod.WAIVED)
                .transactionDate(LocalDateTime.now()) // Explicitly set transaction date
                .notes("Fine waived: " + reason)
                .processedBy(user)
                .status(FineTransaction.TransactionStatus.COMPLETED)
                .build();

        // Update borrow
        borrow.setFineStatus(Borrow.FineStatus.WAIVED);

        // Update member
        Member member = borrow.getMember();
        member.setCurrentFinesDue(member.getCurrentFinesDue() - borrow.getFineAmount());

        // Reset borrow fine
        double waivedAmount = borrow.getFineAmount();
        borrow.setFineAmount(0.0);

        fineTransactionRepository.save(transaction);
        borrowRepository.save(borrow);
        memberRepository.save(member);

        log.info("Fine waived: Borrow ID={}, Amount=${}, Member={}, Reason={}",
                borrowId, waivedAmount, member.getFullName(), reason);
    }
    
    /**
     * Check and lift expired bans
     */
    @Scheduled(cron = "0 0 3 * * ?") // Runs at 3 AM daily
    @Transactional
    public void checkExpiredBans() {
        List<Member> bannedMembers = memberRepository.findBannedMembers();
        
        for (Member member : bannedMembers) {
            if (member.isBanExpired()) {
                member.setIsBanned(false);
                member.setBanReason(null);
                member.setBanStartDate(null);
                member.setBanEndDate(null);
                
                memberRepository.save(member);
                
                notificationService.sendBanLiftedNotification(member);
                log.info("Ban lifted for member {}", member.getId());
            }
        }
    }
    
    /**
     * Get fine summary for member
     */
    public FineSummary getFineSummary(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("Member not found"));
        
        List<Borrow> pendingFines = borrowRepository.findPendingFinesByMember(memberId);
        
        double totalPending = pendingFines.stream()
                .mapToDouble(Borrow::getFineAmount)
                .sum();
        
        return FineSummary.builder()
                .memberId(memberId)
                .memberName(member.getFullName())
                .currentFinesDue(member.getCurrentFinesDue())
                .totalFinesPaid(member.getTotalFinesPaid())
                .overdueBooksCount(member.getOverdueBooksCount())
                .isBanned(member.getIsBanned())
                .banEndDate(member.getBanEndDate())
                .pendingBorrows(pendingFines)
                .totalPendingFines(totalPending)
                .build();
    }
    
    // Helper methods to get system settings
    private double getFinePerDay() {
        return systemSettingsRepository.findByKey("FINE_PER_DAY")
                .map(SystemSettings::getDoubleValue)
                .orElse(DEFAULT_FINE_PER_DAY);
    }
    
    private int getMaxOverdueDays() {
        return systemSettingsRepository.findByKey("MAX_OVERDUE_DAYS")
                .map(SystemSettings::getIntValue)
                .orElse(DEFAULT_MAX_OVERDUE_DAYS);
    }
    
    private double getCreditLimit() {
        return systemSettingsRepository.findByKey("CREDIT_LIMIT")
                .map(SystemSettings::getDoubleValue)
                .orElse(DEFAULT_CREDIT_LIMIT);
    }
    
    // DTO for fine summary
    @Data
    @Builder
    public static class FineSummary {
        private Long memberId;
        private String memberName;
        private Double currentFinesDue;
        private Double totalFinesPaid;
        private Integer overdueBooksCount;
        private Boolean isBanned;
        private LocalDate banEndDate;
        private List<Borrow> pendingBorrows;
        private Double totalPendingFines;
    }
}