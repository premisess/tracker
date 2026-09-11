package com.fitness.tracker.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class WorkoutTypeBreakdown {
    private String type;
    private long count;
    private int totalCalories;
    private int totalDuration;
}
