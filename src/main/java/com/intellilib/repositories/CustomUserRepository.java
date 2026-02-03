package com.intellilib.repositories;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CustomUserRepository {
    @Modifying
    @Transactional
    @Query(value = "DELETE FROM activities WHERE user_id = :userId", nativeQuery = true)
    void deleteUserActivities(@Param("userId") Long userId);
    
    @Modifying
    @Transactional
    @Query(value = "UPDATE members SET id = id WHERE id IN (SELECT member_id FROM users WHERE id = :userId)", nativeQuery = true)
    void unlinkMember(@Param("userId") Long userId);
}
