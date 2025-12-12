package com.intellilib.services;

import com.intellilib.models.Activity;
import com.intellilib.models.User;
import com.intellilib.repositories.ActivityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class ActivityService {

    private final ActivityRepository activityRepository;
    private final UserService userService;

    /**
     * Log a new activity
     * @param username The username of the user performing the action
     * @param action The type of action (e.g., 'LOGIN', 'LOGOUT', 'BOOK_BORROWED')
     * @param description Detailed description of the activity
     * @param ipAddress The IP address of the user (optional)
     */
    public void logActivity(String username, String action, String description, String ipAddress) {
        // Find user by username
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
    public void logActivity(String username, String action, String description) {
        logActivity(username, action, description, null);
    }

    /**
     * Get recent activities for the admin dashboard
     * @param limit Maximum number of activities to return
     * @return List of recent activities
     */
    public List<Activity> getRecentActivities(int limit) {
        return activityRepository.findAllByOrderByTimestampDesc(PageRequest.of(0, limit));
    }

    /**
     * Get recent activities for a specific user
     * @param username The username of the user
     * @param limit Maximum number of activities to return
     * @return List of user's recent activities
     */
    public List<Activity> getUserActivities(String username, int limit) {
        Optional<User> userOpt = userService.findByUsername(username);
        if (userOpt.isEmpty()) {
            throw new RuntimeException("User not found: " + username);
        }
        return activityRepository.findRecentByUserId(userOpt.get().getId(), PageRequest.of(0, limit));
    }

    /**
     * Get activities by action type
     * @param action The action type to filter by
     * @param limit Maximum number of activities to return
     * @return List of activities matching the action type
     */
    public List<Activity> getActivitiesByAction(String action, int limit) {
        return activityRepository.findByAction(action, PageRequest.of(0, limit));
    }

    /**
     * Clean up old activities
     * @param olderThan Delete activities older than this date
     * @return Number of activities deleted
     */
    public int cleanupOldActivities(LocalDateTime olderThan) {
        return activityRepository.deleteByTimestampBefore(olderThan);
    }
}
