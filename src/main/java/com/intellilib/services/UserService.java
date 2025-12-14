package com.intellilib.services;

import com.intellilib.models.User;
import com.intellilib.repositories.UserRepository;
import com.intellilib.session.SessionManager;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {
    
    private final UserRepository userRepository;
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
        return userRepository.save(user);
    }

    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }

//    public List<UserActivity> getRecentActivity(int limit) {
//        // This might be better in a separate ActivityService
//        Pageable pageable = PageRequest.of(0, limit, Sort.by("lastLogin").descending());
//        return userRepository.findRecentActiveUsers(pageable);
//    }
//
//    // OR if you have a separate Activity entity:
//    public List<Activity> getRecentActivity(int limit) {
//        Pageable pageable = PageRequest.of(0, limit, Sort.by("timestamp").descending());
//        return activityRepository.findAll(pageable).getContent();
//    }
}