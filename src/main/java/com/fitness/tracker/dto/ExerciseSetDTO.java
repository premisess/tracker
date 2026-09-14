package com.fitness.tracker.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class ExerciseSetDTO {
    @Min(value = 0, message = "Reps can't be negative")
    @Max(value = 1000, message = "Reps must be at most 1000")
    private Integer reps;

    @DecimalMin(value = "0", message = "Weight can't be negative")
    @DecimalMax(value = "1000", message = "Weight must be at most 1000 kg")
    private Double weightKg;

    @Min(value = 0, message = "Duration can't be negative")
    @Max(value = 86400, message = "A set can't last more than 24 hours")
    private Integer durationSec;

    @DecimalMin(value = "1", message = "RPE is between 1 and 10")
    @DecimalMax(value = "10", message = "RPE is between 1 and 10")
    private Double rpe;

    private Boolean warmup;

    @AssertTrue(message = "Each set needs reps or a duration")
    public boolean isEffortRecorded() {
        return (reps != null && reps > 0) || (durationSec != null && durationSec > 0);
    }
}
