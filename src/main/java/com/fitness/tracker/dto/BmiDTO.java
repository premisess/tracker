package com.fitness.tracker.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDate;

@Data
public class BmiDTO {
    @NotNull(message = "Weight is required")
    @DecimalMin(value = "20", message = "Weight must be between 20 and 500 kg")
    @DecimalMax(value = "500", message = "Weight must be between 20 and 500 kg")
    private Double weight;

    @NotNull(message = "Height is required")
    @DecimalMin(value = "50", message = "Height must be between 50 and 272 cm")
    @DecimalMax(value = "272", message = "Height must be between 50 and 272 cm")
    private Double height;

    // Optional: defaults to today.
    private LocalDate date;
}
