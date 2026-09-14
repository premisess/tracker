package com.fitness.tracker.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

@Data
public class ProgressRequest {
    @NotNull(message = "Progress is required")
    @PositiveOrZero(message = "Progress can't be negative")
    private Double progress;
}
