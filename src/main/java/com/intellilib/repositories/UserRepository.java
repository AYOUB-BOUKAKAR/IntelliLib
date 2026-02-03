package com.intellilib.repositories;

import com.intellilib.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Repository
public interface UserRepository extends JpaRepository<User, Long>, CustomUserRepository {
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

    Optional<User> findByMemberId(Long memberId);

    @Query("SELECT u FROM User u WHERE u.member IS NULL AND u.role = 'MEMBER'")
    List<User> findMembersWithoutLinkedMember();

    @Query("SELECT u FROM User u WHERE u.member IS NOT NULL")
    List<User> findUsersWithLinkedMember();

    @Query("SELECT COUNT(u) FROM User u WHERE u.active = true AND u.createdAt < :date")
    long countActiveMembersByDate(@Param("date") LocalDateTime date);

    @Query("SELECT COUNT(u) FROM User u WHERE u.active = true AND u.createdAt >= :startDate AND u.createdAt < :endDate")
    long countActiveMembersBetween(@Param("startDate") LocalDateTime startDate,
                                   @Param("endDate") LocalDateTime endDate);

    // Count active members with role MEMBER (excluding ADMIN and LIBRARIAN)
    @Query("SELECT COUNT(u) FROM User u WHERE u.active = true AND u.role = 'MEMBER'")
    long countActiveMembersOnly();

    @Query("SELECT COUNT(u) FROM User u WHERE u.active = true AND u.role = 'MEMBER' AND u.createdAt >= :startDate AND u.createdAt < :endDate")
    long countActiveMembersOnlyBetween(@Param("startDate") LocalDateTime startDate,
                                       @Param("endDate") LocalDateTime endDate);

    // In UserRepository.java, add this method:
    List<User> findTop10ByOrderByCreatedAtDesc();

}