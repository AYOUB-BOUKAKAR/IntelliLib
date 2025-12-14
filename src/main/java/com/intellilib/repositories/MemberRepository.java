package com.intellilib.repositories;

import com.intellilib.models.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    @Query("SELECT m FROM Member m WHERE m.isBanned = true")
    List<Member> findBannedMembers();
    
    @Query("SELECT m FROM Member m WHERE m.currentFinesDue > :minAmount")
    List<Member> findMembersWithHighFines(@Param("minAmount") Double minAmount);
    
    @Query("SELECT m FROM Member m WHERE m.active = true AND m.isBanned = false")
    List<Member> findActiveMembers();
}