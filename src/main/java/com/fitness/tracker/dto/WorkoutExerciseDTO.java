package com.fitness.tracker.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class WorkoutExerciseDTO {
    @NotNull(message = "Pick an exercise")
    private Long exerciseId;

    @Size(max = 255, message = "Exercise notes must be at most 255 characters")
    private String notes;

    @NotNull(message = "Add at least one set for each exercise")
    @Size(min = 1, message = "Add at least one set for each exercise")
    @Size(max = 50, message = "At most 50 sets per exercise")
    private List<@Valid @NotNull ExerciseSetDTO> sets;
}
