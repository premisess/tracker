package com.fitness.tracker.dto;

import com.fitness.tracker.entity.Goal;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.time.LocalDate;

@Data
public class GoalDTO {
    @NotBlank(message = "Goal title is required")
    @Size(max = 255, message = "Title must be at most 255 characters")
    private String title;

    @NotNull(message = "Choose a goal type")
    private Goal.GoalType goalType;

    @NotNull(message = "Target value is required")
    @Positive(message = "Target value must be greater than 0")
    private Double targetValue;

    @PositiveOrZero(message = "Progress can't be negative")
    private Double currentProgress;

    @NotBlank(message = "Unit is required")
    @Size(max = 30, message = "Unit must be at most 30 characters")
    private String unit;

    @NotNull(message = "Deadline is required")
    private LocalDate deadline;

    private Boolean autoTrack;
}
