package com.fitness.tracker.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class RunDTO {
    @NotBlank(message = "Activity type is required")
    private String type;

    // The runner's local calendar date, so a 23:30 run doesn't land on tomorrow.
    @NotNull(message = "Date is required")
    private LocalDate date;

    @NotNull(message = "Start time is required")
    @Positive(message = "Start time is invalid")
    private Long startedAt;

    @Size(max = 255, message = "Notes must be at most 255 characters")
    private String notes;

    @Size(max = 10, message = "You can add at most 10 tags")
    private List<@NotBlank(message = "Tags can't be empty") @Size(max = 20, message = "Tags must be at most 20 characters") String> tags;

    @NotNull(message = "No GPS data was recorded")
    @Size(min = 2, message = "Record at least a few seconds of GPS before saving")
    @Size(max = 43200, message = "A recording can hold at most 12 hours of GPS points")
    private List<@Valid @NotNull RunPointDTO> points;
}
