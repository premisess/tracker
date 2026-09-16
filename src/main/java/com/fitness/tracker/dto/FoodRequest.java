package com.fitness.tracker.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** A food the user describes themselves, with nutrition for one serving. */
@Data
public class FoodRequest {
    @NotBlank(message = "Food name is required")
    @Size(max = 120, message = "Food name must be at most 120 characters")
    private String name;

    @NotBlank(message = "Describe the serving, e.g. \"1 cup\" or \"100 g\"")
    @Size(max = 60, message = "Serving description must be at most 60 characters")
    private String servingLabel;

    @DecimalMin(value = "0", message = "Serving weight can't be negative")
    @DecimalMax(value = "5000", message = "Serving weight must be at most 5000 g")
    private Double servingGrams;

    @NotNull(message = "Calories are required")
    @DecimalMin(value = "0", message = "Calories can't be negative")
    @DecimalMax(value = "5000", message = "Calories must be at most 5000 per serving")
    private Double calories;

    @NotNull(message = "Protein is required (enter 0 if none)")
    @DecimalMin(value = "0", message = "Protein can't be negative")
    @DecimalMax(value = "500", message = "Protein must be at most 500 g per serving")
    private Double proteinG;

    @NotNull(message = "Carbs are required (enter 0 if none)")
    @DecimalMin(value = "0", message = "Carbs can't be negative")
    @DecimalMax(value = "1000", message = "Carbs must be at most 1000 g per serving")
    private Double carbsG;

    @NotNull(message = "Fat is required (enter 0 if none)")
    @DecimalMin(value = "0", message = "Fat can't be negative")
    @DecimalMax(value = "500", message = "Fat must be at most 500 g per serving")
    private Double fatG;

    @DecimalMin(value = "0", message = "Fiber can't be negative")
    @DecimalMax(value = "200", message = "Fiber must be at most 200 g per serving")
    private Double fiberG;
}
