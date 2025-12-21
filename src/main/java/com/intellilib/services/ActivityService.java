package com.intellilib.services;

import com.intellilib.models.Activity;
import com.intellilib.models.User;
import com.intellilib.repositories.ActivityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class ActivityService {

    private final ActivityRepository activityRepository;
    private final UserService userService;

    /**
     * Log a new activity
     */
    public void logActivity(String username, String action, String description, String ipAddress) {
        Optional<User> userOpt = userService.findByUsername(username);
        if (userOpt.isEmpty()) {
            throw new RuntimeException("User not found: " + username);
        }
        
        Activity activity = Activity.builder()
                .user(userOpt.get())
                .action(action)
                .description(description)
                .ipAddress(ipAddress)
                .build();
                
        activityRepository.save(activity);
    }

    /**
     * Simplified version without IP address
     */
    public void logActivity(User user, String action, String description) {
        logActivity(user, action, description, null);
    }

    /**
     * Log activity using User object
     */
    public void logActivity(User user, String action, String description, String ipAddress) {
        Activity activity = Activity.builder()
                .user(user)
                .action(action)
                .description(description)
                .ipAddress(ipAddress)
                .build();
        activityRepository.save(activity);
    }

    /**
     * Get recent activities for the admin dashboard
     */
    public List<Activity> getRecentActivities(int limit) {
        return activityRepository.findAllByOrderByTimestampDesc(PageRequest.of(0, limit));
    }

    /**
     * Get activities for chart - last 7 days
     */
    public Map<String, Object> getActivityChartData(int days) {
        Map<String, Object> chartData = new HashMap<>();

        // Use proper date range (start of first day to end of last day)
        LocalDateTime endDate = LocalDateTime.now()
                .withHour(23).withMinute(59).withSecond(59).withNano(999999999);
        LocalDateTime startDate = endDate.minusDays(days - 1)
                .withHour(0).withMinute(0).withSecond(0).withNano(0);

        // Debug logging
        System.out.println("DEBUG: Query range - Start: " + startDate + ", End: " + endDate);

        // Get daily activity counts
        List<Object[]> dailyCounts = activityRepository.countActivitiesByDay(startDate, endDate);

        // Debug the results
        System.out.println("DEBUG: Daily counts from DB: " + dailyCounts);

        List<String> labels = new ArrayList<>();
        List<Long> data = new ArrayList<>();

        // Generate labels for all days
        for (int i = 0; i < days; i++) {
            LocalDate date = LocalDate.now().minusDays(days - 1 - i);
            labels.add(date.toString());
            data.add(0L); // Initialize with 0
        }

        // Fill in actual data with null checks
        if (dailyCounts != null) {
            for (Object[] count : dailyCounts) {
                // Add null checks to prevent NPE
                if (count != null && count.length >= 2 && count[0] != null && count[1] != null) {
                    // Convert java.sql.Date to LocalDate properly
                    LocalDate date;
                    if (count[0] instanceof java.sql.Date) {
                        date = ((java.sql.Date) count[0]).toLocalDate();
                    } else if (count[0] instanceof LocalDate) {
                        date = (LocalDate) count[0];
                    } else {
                        // Handle other date types or skip this entry
                        System.err.println("Unexpected date type: " + count[0].getClass());
                        continue;
                    }
                    
                    Long activityCount = (Long) count[1];

                    String dateString = date.toString();
                    int index = labels.indexOf(dateString);
                    if (index != -1) {
                        data.set(index, activityCount);
                    }
                }
            }
        }

        // Get activity by type with null checks
        List<Object[]> byType = activityRepository.countActivitiesByType(startDate, endDate);
        List<String> types = new ArrayList<>();
        List<Long> typeCounts = new ArrayList<>();

        if (byType != null) {
            for (Object[] typeCount : byType) {
                // Add null checks for type data
                if (typeCount != null && typeCount.length >= 2 && typeCount[0] != null && typeCount[1] != null) {
                    types.add((String) typeCount[0]);
                    typeCounts.add((Long) typeCount[1]);
                }
            }
        }

        // Get top users with activity counts (add this missing feature)
        List<Object[]> topUserData = activityRepository.findTopActiveUsers(startDate, endDate, PageRequest.of(0, 5));
        List<String> topUsers = new ArrayList<>();
        List<Long> userActivityCounts = new ArrayList<>();

        if (topUserData != null) {
            for (Object[] userData : topUserData) {
                if (userData != null && userData.length >= 2 && userData[0] != null && userData[1] != null) {
                    topUsers.add((String) userData[0]);
                    userActivityCounts.add((Long) userData[1]);
                }
            }
        }

        // Calculate total activities and average with null safety
        Long totalActivities = activityRepository.countByTimestampBetween(startDate, endDate);
        if (totalActivities == null) {
            totalActivities = 0L;
        }

        long averageDaily = days > 0 ? totalActivities / days : 0;

        chartData.put("labels", labels);
        chartData.put("dailyActivity", data);
        chartData.put("activityTypes", types);
        chartData.put("typeCounts", typeCounts);
        chartData.put("topUsers", topUsers);
        chartData.put("userActivityCounts", userActivityCounts);
        chartData.put("totalActivities", totalActivities);
        chartData.put("averageDaily", averageDaily);

        return chartData;
    }

    /**
     * Get activities by action type
     */
    public List<Activity> getActivitiesByAction(String action, int limit) {
        return activityRepository.findByAction(action, PageRequest.of(0, limit));
    }

    /**
     * Get recent registered users (last 30 days)
     */
    public List<User> getRecentRegisteredUsers(int limit) {
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        return activityRepository.findRecentRegisteredUsers(thirtyDaysAgo, PageRequest.of(0, limit));
    }

    /**
     * Clean up old activities
     */
    public int cleanupOldActivities(LocalDateTime olderThan) {
        return activityRepository.deleteByTimestampBefore(olderThan);
    }
}