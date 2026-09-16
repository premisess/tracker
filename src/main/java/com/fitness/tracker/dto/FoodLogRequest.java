package com.fitness.tracker.dto;

import com.fitness.tracker.entity.FoodLogEntry;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

/** Logs a food: either one from the catalog ({@code foodId}) or a one-off {@code customFood}. */
@Data
public class FoodLogRequest {
    @NotNull(message = "Date is required")
    private LocalDate date;

    @NotNull(message = "Choose a meal")
    private FoodLogEntry.Meal meal;

    private Long foodId;

    @Valid
    private FoodRequest customFood;

    // Also keep a custom food in "My foods" for next time.
    private Boolean saveCustomFood;

    @NotNull(message = "Servings are required")
    @DecimalMin(value = "0.1", message = "Servings must be at least 0.1")
    @DecimalMax(value = "50", message = "Servings must be at most 50")
    private Double servings;

    @AssertTrue(message = "Pick a food from the list or enter your own")
    public boolean isFoodChosen() {
        return (foodId != null) != (customFood != null);
    }
}
