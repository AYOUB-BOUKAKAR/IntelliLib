package com.intellilib.services;

import com.intellilib.models.User;
import com.intellilib.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.Optional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {
    
    private final UserRepository userRepository;
    
    public User registerUser(String username, String password, String email, User.UserRole role) {
        if (userRepository.existsByUsername(username)) {
            throw new RuntimeException("Username already exists");
        }
        
        User user = new User(username, password, email, role);
        return userRepository.save(user);
    }
    
    public Optional<User> login(String username, String password) {
        Optional<User> user = userRepository.findByUsername(username);
        if (user.isPresent() && user.get().getPassword().equals(password)) {
            User loggedInUser = user.get();
            loggedInUser.setLastLogin(java.time.LocalDateTime.now());
            userRepository.save(loggedInUser);
            return user;
        }
        return Optional.empty();
    }
    
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
    
    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }
}