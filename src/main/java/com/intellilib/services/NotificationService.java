package com.intellilib.services;

import com.intellilib.models.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import com.intellilib.repositories.SystemSettingsRepository;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {
    
    private final JavaMailSender mailSender;
    private final SystemSettingsRepository systemSettingsRepository;
    
    @Async
    public void sendFineWarningNotification(Member member, Borrow borrow) {
        try {
            String subject = "Library Fine Warning";
            String message = String.format(
                "Dear %s,\n\n" +
                "You have accumulated fines of $%.2f for overdue books.\n" +
                "Book: %s\n" +
                "Due Date: %s\n" +
                "Days Overdue: %d\n" +
                "Current Fine: $%.2f\n\n" +
                "Please return the book and pay your fines to avoid restrictions.\n\n" +
                "Thank you,\nLibrary Management System",
                member.getFullName(),
                member.getCurrentFinesDue(),
                borrow.getBook().getTitle(),
                borrow.getDueDate(),
                borrow.getDaysOverdue(),
                borrow.getFineAmount()
            );
            
            sendEmail(member.getEmail(), subject, message);
            log.info("Fine warning sent to {}", member.getEmail());
        } catch (Exception e) {
            log.error("Failed to send fine warning: {}", e.getMessage());
        }
    }
    
    @Async
    public void sendBanNotification(Member member, Borrow borrow) {
        try {
            String subject = "Library Membership Suspended";
            String message = String.format(
                "Dear %s,\n\n" +
                "Your library membership has been suspended due to excessive overdue items.\n" +
                "Reason: %s\n" +
                "Book: %s (Overdue by %d days)\n" +
                "Ban Period: %s to %s\n\n" +
                "Please contact the library to resolve this issue.\n\n" +
                "Thank you,\nLibrary Management System",
                member.getFullName(),
                member.getBanReason(),
                borrow.getBook().getTitle(),
                borrow.getDaysOverdue(),
                member.getBanStartDate(),
                member.getBanEndDate()
            );
            
            sendEmail(member.getEmail(), subject, message);
            log.info("Ban notification sent to {}", member.getEmail());
        } catch (Exception e) {
            log.error("Failed to send ban notification: {}", e.getMessage());
        }
    }
    
    @Async
    public void sendPaymentReceipt(Member member, FineTransaction transaction) {
        try {
            String subject = "Fine Payment Receipt";
            String message = String.format(
                "Dear %s,\n\n" +
                "Payment Receipt\n" +
                "Receipt #: %s\n" +
                "Date: %s\n" +
                "Amount: $%.2f\n" +
                "Payment Method: %s\n" +
                "Reference: %s\n\n" +
                "Thank you for your payment.\n\n" +
                "Library Management System",
                member.getFullName(),
                transaction.getReceiptNumber(),
                transaction.getTransactionDate(),
                transaction.getAmount(),
                transaction.getPaymentMethod(),
                transaction.getPaymentReference()
            );
            
            sendEmail(member.getEmail(), subject, message);
            log.info("Payment receipt sent to {}", member.getEmail());
        } catch (Exception e) {
            log.error("Failed to send payment receipt: {}", e.getMessage());
        }
    }
    
    @Async
    public void sendBanLiftedNotification(Member member) {
        try {
            String subject = "Library Membership Restored";
            String message = String.format(
                "Dear %s,\n\n" +
                "Your library membership has been restored.\n" +
                "You can now borrow books again.\n\n" +
                "Thank you,\nLibrary Management System",
                member.getFullName()
            );
            
            sendEmail(member.getEmail(), subject, message);
            log.info("Ban lifted notification sent to {}", member.getEmail());
        } catch (Exception e) {
            log.error("Failed to send ban lifted notification: {}", e.getMessage());
        }
    }
    
    private void sendEmail(String to, String subject, String text) {
        if (to == null || to.isEmpty()) {
            return;
        }
        
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);
        message.setFrom(getFromEmail());
        
        mailSender.send(message);
    }
    
    private String getFromEmail() {
        return systemSettingsRepository.findByKey("EMAIL_FROM")
                .map(SystemSettings::getValue)
                .orElse("noreply@library.com");
    }
}