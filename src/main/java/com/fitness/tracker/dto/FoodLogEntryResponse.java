package com.fitness.tracker.dto;

import java.time.LocalDate;

/** A diary entry. Nutrition values are totals for the logged servings. */
public record FoodLogEntryResponse(
        Long id,
        LocalDate date,
        String meal,
        Long foodId,
        String name,
        String servingLabel,
        double servings,
        double calories,
        double proteinG,
        double carbsG,
        double fatG) {
}
