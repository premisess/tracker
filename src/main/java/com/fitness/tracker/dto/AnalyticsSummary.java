package com.fitness.tracker.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.List;

@Data
@AllArgsConstructor
public class AnalyticsSummary {
    private long totalWorkouts;
    private int totalCaloriesBurned;
    private int totalDurationMinutes;
    private double averageCaloriesPerWorkout;
    private int currentStreak;
    private int longestStreak;
    private List<WorkoutTypeBreakdown> byType;
    private List<DailyPoint> caloriesOverTime;
    private List<DailyPoint> weightTrend;
}
