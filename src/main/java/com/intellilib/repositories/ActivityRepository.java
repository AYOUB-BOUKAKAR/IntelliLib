package com.intellilib.repositories;

import com.intellilib.models.Activity;
import com.intellilib.models.User;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ActivityRepository extends JpaRepository<Activity, Long> {

    List<Activity> findByUserOrderByTimestampDesc(User user, Pageable pageable);

    // CHANGED: Added JOIN FETCH to load User eagerly
    @Query("SELECT a FROM Activity a JOIN FETCH a.user u ORDER BY a.timestamp DESC")
    List<Activity> findAllByOrderByTimestampDesc(Pageable pageable);

    @Query("SELECT a FROM Activity a JOIN FETCH a.user u WHERE a.user.id = :userId ORDER BY a.timestamp DESC")
    List<Activity> findRecentByUserId(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT a FROM Activity a JOIN FETCH a.user u WHERE a.action = :action ORDER BY a.timestamp DESC")
    List<Activity> findByAction(@Param("action") String action, Pageable pageable);

    @Modifying
    @Query("DELETE FROM Activity a WHERE a.timestamp < :olderThan")
    @Transactional
    int deleteByTimestampBefore(@Param("olderThan") LocalDateTime olderThan);

    // New queries for chart data
    @Query("SELECT CAST(a.timestamp AS date) as activityDate, COUNT(a) as count " +
            "FROM Activity a " +
            "WHERE a.timestamp >= :startDate AND a.timestamp < :endDate " +
            "GROUP BY CAST(a.timestamp AS date) " +
            "ORDER BY CAST(a.timestamp AS date)")
    List<Object[]> countActivitiesByDay(@Param("startDate") LocalDateTime startDate,
                                        @Param("endDate") LocalDateTime endDate);

    @Query("SELECT a.action, COUNT(a) as count " +
            "FROM Activity a " +
            "WHERE a.timestamp >= :startDate AND a.timestamp < :endDate " +
            "GROUP BY a.action " +
            "ORDER BY count DESC")
    List<Object[]> countActivitiesByType(@Param("startDate") LocalDateTime startDate,
                                         @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COUNT(a) FROM Activity a WHERE a.timestamp >= :startDate AND a.timestamp < :endDate")
    long countByTimestampBetween(@Param("startDate") LocalDateTime startDate,
                                 @Param("endDate") LocalDateTime endDate);

    // Get recent registered users from activities
    @Query("SELECT DISTINCT a.user FROM Activity a " +
            "WHERE a.action = 'USER_REGISTERED' AND a.timestamp >= :since " +
            "ORDER BY a.timestamp DESC")
    List<User> findRecentRegisteredUsers(@Param("since") LocalDateTime since, Pageable pageable);

    @Query("SELECT u.username, COUNT(a) as activityCount FROM Activity a JOIN a.user u " +
            "WHERE a.timestamp >= :startDate AND a.timestamp < :endDate " +
            "GROUP BY u.username ORDER BY activityCount DESC")
    List<Object[]> findTopActiveUsers(@Param("startDate") LocalDateTime startDate,
                                      @Param("endDate") LocalDateTime endDate,
                                      Pageable pageable);

    // CHANGED: Removed JOIN FETCH as it's now eager by default
    @Query("SELECT a FROM Activity a ORDER BY a.timestamp DESC")
    List<Activity> findRecentActivitiesWithUser(Pageable pageable);

    default List<Activity> findRecentActivities(int limit) {
        return findRecentActivitiesWithUser(PageRequest.of(0, limit));
    }
}