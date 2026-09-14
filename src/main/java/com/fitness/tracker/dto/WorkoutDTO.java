package com.fitness.tracker.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.time.LocalDate;
import java.util.List;

@Data
public class WorkoutDTO {
    @NotBlank(message = "Workout type is required")
    private String type;

    @NotNull(message = "Duration is required")
    @Min(value = 1, message = "Duration must be at least 1 minute")
    @Max(value = 1440, message = "Duration can't be more than 24 hours")
    private Integer duration;

    @NotNull(message = "Date is required")
    private LocalDate date;

    @Size(max = 255, message = "Notes must be at most 255 characters")
    private String notes;

    // Tags are stored comma-joined in a varchar(255); 10 tags of 20 chars always fit.
    @Size(max = 10, message = "You can add at most 10 tags")
    private List<@NotBlank(message = "Tags can't be empty") @Size(max = 20, message = "Tags must be at most 20 characters") String> tags;

    // Optional. On update, null leaves the logged exercises as they are; an empty list removes them.
    @Size(max = 30, message = "At most 30 exercises per workout")
    private List<@Valid @NotNull WorkoutExerciseDTO> exercises;
}
