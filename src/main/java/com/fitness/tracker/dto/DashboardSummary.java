package com.fitness.tracker.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Everything the dashboard screen needs, in one call — totals, streaks and the
 * "journey guide" flags that tell a new user which step they still have to do.
 */
@Data
@AllArgsConstructor
public class DashboardSummary {
    private String name;
    private boolean profileComplete;
    private long goalCount;
    private long activeGoalCount;
    private long workoutCount;
    private int currentStreak;
    private Double latestBmi;
    private String latestBmiCategory;
    private String nextStep;
    private int totalCaloriesBurned;
    private int waterTodayMl;
    private boolean hasWaterLog;
    private int longestStreak;
    private boolean emailVerified;
    private long badgesEarned;
    private int caloriesEatenToday;
    private int calorieTarget;
    // The plan being followed and its next session; null when not following one.
    private String activePlanName;
    private String activePlanSlug;
    private String nextPlanSession;
    private Integer planProgressPercent;
}
