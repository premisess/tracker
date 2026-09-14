package com.fitness.tracker.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class WorkoutExerciseResponse {
    private Long id;
    private Long exerciseId;
    private String exerciseName;
    private String thumbnailUrl;
    private String trackingType;
    private Integer position;
    private String notes;
    private List<ExerciseSetResponse> sets;
    // Sum of weight x reps over working sets; 0 for bodyweight or timed exercises.
    private double volumeKg;
}
