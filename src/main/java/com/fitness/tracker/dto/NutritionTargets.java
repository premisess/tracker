package com.fitness.tracker.dto;

/**
 * Daily calorie and macro targets. {@code personalized} is false when the profile is incomplete and a
 * general guideline is used instead; {@code basis} explains where the numbers come from.
 */
public record NutritionTargets(
        int calories,
        int proteinG,
        int carbsG,
        int fatG,
        boolean personalized,
        String basis) {
}
