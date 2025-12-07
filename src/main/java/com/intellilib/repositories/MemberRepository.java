package com.intellilib.repositories;

import com.intellilib.models.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface MemberRepository extends JpaRepository<Member, Long> {
    // FREE: save(), findById(), findAll(), deleteById(), etc.
    
    // Find member by email
    Optional<Member> findByEmail(String email);
    
    // Find member by phone
    Optional<Member> findByPhone(String phone);
    
    // Find members by name (partial match)
    List<Member> findByFullNameContainingIgnoreCase(String name);
    
    // Find active members
    List<Member> findByActiveTrue();
    
    // Find inactive members
    List<Member> findByActiveFalse();
    
    // Check if email exists
    boolean existsByEmail(String email);
    
    // Check if phone exists
    boolean existsByPhone(String phone);
}