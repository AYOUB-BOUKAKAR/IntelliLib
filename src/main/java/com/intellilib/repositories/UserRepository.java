package com.intellilib.repositories;

import com.intellilib.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    // FREE: save(), findById(), findAll(), deleteById(), etc.
    
    // Find by username
    Optional<User> findByUsername(String username);
    
    // Find by email
    Optional<User> findByEmail(String email);
    
    // Find by role
    List<User> findByRole(User.UserRole role);
    
    // Check if username exists
    boolean existsByUsername(String username);
    
    // Check if email exists
    boolean existsByEmail(String email);
    
    // Find active users
    List<User> findByActiveTrue();

    // Find inactive users
    List<User> countByActiveFalse();

    long countByActiveTrue();
}