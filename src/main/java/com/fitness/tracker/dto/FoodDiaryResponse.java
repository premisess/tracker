package com.fitness.tracker.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * One day of the food diary. {@code meals} is keyed BREAKFAST, LUNCH, DINNER, SNACK (always all four, in that order).
 * {@code caloriesBurned} comes from the workouts logged that day.
 */
public record FoodDiaryResponse(
        LocalDate date,
        NutritionTargets targets,
        MacroTotals totals,
        int caloriesBurned,
        int waterMl,
        Map<String, List<FoodLogEntryResponse>> meals) {
}
