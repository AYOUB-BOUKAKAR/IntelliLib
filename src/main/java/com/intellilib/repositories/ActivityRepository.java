package com.intellilib.repositories;

import com.intellilib.models.Activity;
import com.intellilib.models.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ActivityRepository extends JpaRepository<Activity, Long> {
    
    List<Activity> findByUserOrderByTimestampDesc(User user, Pageable pageable);
    
    List<Activity> findAllByOrderByTimestampDesc(Pageable pageable);
    
    @Query("SELECT a FROM Activity a WHERE a.user.id = :userId ORDER BY a.timestamp DESC")
    List<Activity> findRecentByUserId(@Param("userId") Long userId, Pageable pageable);
    
    @Query("SELECT a FROM Activity a WHERE a.action = :action ORDER BY a.timestamp DESC")
    List<Activity> findByAction(@Param("action") String action, Pageable pageable);
    
    @Modifying
    @Query("DELETE FROM Activity a WHERE a.timestamp < :olderThan")
    @Transactional
    int deleteByTimestampBefore(@Param("olderThan") LocalDateTime olderThan);
}
