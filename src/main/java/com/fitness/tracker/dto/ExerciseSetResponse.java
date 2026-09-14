package com.fitness.tracker.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ExerciseSetResponse {
    private Integer setNumber;
    private Integer reps;
    private Double weightKg;
    private Integer durationSec;
    private Double rpe;
    private boolean warmup;
}
