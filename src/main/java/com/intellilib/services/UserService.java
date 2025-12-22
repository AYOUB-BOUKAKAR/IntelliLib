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

    public User createUserAccountForMember(String username, String password, String email, Long memberId) {
        // Validate username and email
        if (userRepository.existsByUsername(username)) {
            throw new RuntimeException("Username already exists");
        }
        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("Email already exists");
        }

        // Get the managed member entity
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("Member not found with id: " + memberId));

        // Check if member already has an account
        if (member.hasUserAccount()) {
            throw new RuntimeException("Member already has a user account");
        }

        // Create new user
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setEmail(email);
        user.setRole(User.UserRole.MEMBER);
        user.setActive(true);
        user.setCreatedAt(LocalDateTime.now());
        user.setMember(member);

        // Save the user (cascade will handle saving the member link)
        User savedUser = userRepository.save(user);

        // Update the member's reference
        member.setUserAccount(savedUser);
        memberRepository.save(member);

        return savedUser;
    }

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

        // Save the user (cascade will handle the member if it's new)
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

        // Check if role is changing
        boolean roleChanged = existingUser.getRole() != user.getRole();

        // Handle member linking/unlinking based on role
        if (user.getRole() == User.UserRole.MEMBER) {
            // User is being updated to MEMBER role
            if (user.getMember() != null && user.getMember().getId() != null) {
                // User should be linked to an existing member
                Member member = memberRepository.findById(user.getMember().getId())
                        .orElseThrow(() -> new RuntimeException("Member not found"));

                // Check if this member is already linked to a different user
                if (member.hasUserAccount() && !member.getUserAccount().getId().equals(user.getId())) {
                    throw new RuntimeException("Member is already linked to another user account");
                }

                // Link the user to the member
                existingUser.setMember(member);
                member.setUserAccount(existingUser);
                memberRepository.save(member);
            } else if (roleChanged && existingUser.getMember() == null) {
                // Role changed to MEMBER but no member provided - can't link
                throw new RuntimeException("Cannot assign MEMBER role without linking to a member");
            }
            // If user.getMember() is null but existingUser already has a member, keep it
        } else if (existingUser.getMember() != null) {
            // User is changing from MEMBER to another role - remove the link
            Member member = existingUser.getMember();
            member.setUserAccount(null);
            existingUser.setMember(null);
            memberRepository.save(member);
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