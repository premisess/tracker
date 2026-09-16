package com.fitness.tracker.dto;

import java.time.LocalDate;
import java.util.List;

/** Daily totals for a range of days (oldest first), with the current targets to compare against. */
public record NutritionHistoryResponse(NutritionTargets targets, List<Day> days) {

    public record Day(LocalDate date, double calories, double proteinG, double carbsG, double fatG, int caloriesBurned) {
    }
}
