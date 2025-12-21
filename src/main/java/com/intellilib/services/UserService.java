package com.intellilib.services;

import com.intellilib.models.Member;
import com.intellilib.models.User;
import com.intellilib.repositories.MemberRepository;
import com.intellilib.repositories.UserRepository;
import com.intellilib.session.SessionManager;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {
    
    private final UserRepository userRepository;
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final SessionManager sessionManager;
    
    public User registerUser(User user) {
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new RuntimeException("Username already exists");
        }
        
        // Encrypt password before saving
        String encryptedPassword = passwordEncoder.encode(user.getPassword());
        user.setPassword(encryptedPassword);
        
        // Set timestamps
        user.setCreatedAt(LocalDateTime.now());
        user.setActive(true);

        if (user.getRole() == User.UserRole.MEMBER && user.getMember() != null) {
            Member member = user.getMember();
            member.linkUserAccount(user);
            // Save member first if it's a new member
            if (member.getId() == null) {
                memberRepository.save(member);
            }
        }
        
        return userRepository.save(user);
    }
    
    public boolean usernameExists(String username) {
        return userRepository.existsByUsername(username);
    }
    
    public boolean emailExists(String email) {
        return userRepository.existsByEmail(email);
    }
    
    public User login(String username, String password) {
        Optional<User> userOpt = userRepository.findByUsername(username);
        
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            
            // Verify password
            if (passwordEncoder.matches(password, user.getPassword())) {
                if (!user.isActive()) {
                    throw new RuntimeException("Account is deactivated");
                }
                
                // Update last login
                user.setLastLogin(LocalDateTime.now());
                User updatedUser = userRepository.save(user);
                
                // Set user in session
                sessionManager.login(updatedUser);
                
                return updatedUser;
            }
        }
        
        return null;
    }
    
    public User getCurrentUser() {
        return sessionManager.getCurrentUser();
    }
    
    public void logout() {
        sessionManager.logout();
    }
    
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
    
    // Optional: Get user by ID
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    public long countActiveMembers() {
        return userRepository.countByActiveTrue();
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public User addUser(User user) {
        return userRepository.save(user);
    }

    public User updateUser(User user) {
        User existingUser = userRepository.findById(user.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Handle member linking for MEMBER role
        if (user.getRole() == User.UserRole.MEMBER && user.getMember() != null) {
            Member member = user.getMember();
            member.linkUserAccount(user);
            // Update or create member
            if (member.getId() == null) {
                memberRepository.save(member);
            } else {
                memberRepository.save(member);
            }
        } else if (existingUser.getMember() != null && user.getRole() != User.UserRole.MEMBER) {
            // If changing from MEMBER to another role, remove the link
            existingUser.getMember().setUserAccount(null);
            existingUser.setMember(null);
        }

        // Update other fields
        existingUser.setUsername(user.getUsername());
        existingUser.setEmail(user.getEmail());
        existingUser.setRole(user.getRole());
        existingUser.setActive(user.isActive());

        // Update password if provided
        if (user.getPassword() != null && !user.getPassword().isEmpty()) {
            String encryptedPassword = passwordEncoder.encode(user.getPassword());
            existingUser.setPassword(encryptedPassword);
        }

        return userRepository.save(existingUser);
    }

    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }

    public double getActiveMembersChangeFromLastMonth() {
        LocalDateTime now = LocalDateTime.now();

        // Get current month start
        LocalDate today = LocalDate.now();
        LocalDate currentMonthStart = today.withDayOfMonth(1);
        LocalDateTime monthStart = currentMonthStart.atStartOfDay();

        // Get previous month start
        LocalDate lastMonthStart = currentMonthStart.minusMonths(1);
        LocalDateTime lastMonthStartDateTime = lastMonthStart.atStartOfDay();
        LocalDateTime lastMonthEndDateTime = monthStart.minusSeconds(1);

        // Count active members added in current month
        long currentMonthActiveMembers = userRepository.countActiveMembersOnlyBetween(monthStart, now);

        // Count active members added in previous month
        long lastMonthActiveMembers = userRepository.countActiveMembersOnlyBetween(
                lastMonthStartDateTime,
                lastMonthEndDateTime
        );

        // Calculate percentage change
        return calculatePercentageChange(currentMonthActiveMembers, lastMonthActiveMembers);
    }

    public long getActiveMembersAddedThisMonth() {
        LocalDate today = LocalDate.now();
        LocalDate monthStart = today.withDayOfMonth(1);
        LocalDateTime monthStartDateTime = monthStart.atStartOfDay();

        return userRepository.countActiveMembersOnlyBetween(monthStartDateTime, LocalDateTime.now());
    }

    private double calculatePercentageChange(double current, double previous) {
        if (previous == 0) {
            return current > 0 ? 100.0 : 0.0;
        }
        return ((current - previous) / previous) * 100;
    }

    public Optional<User> findByMemberId(Long memberId) {
        return userRepository.findByMemberId(memberId);
    }
}