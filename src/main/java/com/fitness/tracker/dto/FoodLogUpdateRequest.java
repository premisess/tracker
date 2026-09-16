package com.fitness.tracker.dto;

import com.fitness.tracker.entity.FoodLogEntry;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import lombok.Data;

/** Changes the servings or meal of a diary entry. Fields left null stay as they are. */
@Data
public class FoodLogUpdateRequest {
    @DecimalMin(value = "0.1", message = "Servings must be at least 0.1")
    @DecimalMax(value = "50", message = "Servings must be at most 50")
    private Double servings;

    private FoodLogEntry.Meal meal;
}
