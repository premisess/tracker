package com.fitness.tracker.service;

import com.fitness.tracker.dto.NutritionTargets;
import com.fitness.tracker.entity.Goal;

import java.util.Locale;

/**
 * Daily targets from the Mifflin-St Jeor resting energy estimate, a light-to-moderate activity factor,
 * and an adjustment for the user's main goal. Protein is set per kg of body weight, fat at 27% of
 * calories, and carbs fill the rest. These are estimates for guidance, not medical advice.
 */
public final class NutritionTargetCalculator {

    // Everyday life plus a few workouts a week. Logged workouts are shown separately, not added here.
    static final double ACTIVITY_FACTOR = 1.45;
    private static final double FAT_SHARE = 0.27;
    private static final int MIN_CALORIES = 1200;

    private NutritionTargetCalculator() {
    }

    /** heightCm is centimetres. Any missing body measurement gives the general 2,000 kcal guideline. */
    public static NutritionTargets calculate(Double weightKg, Double heightCm, Integer age, String gender,
                                             Goal.GoalType goal) {
        if (weightKg == null || heightCm == null || age == null || weightKg <= 0 || heightCm <= 0 || age <= 0) {
            return new NutritionTargets(2000, 100, 250, 65, false,
                    "General guideline. Add your age, gender, weight and height to your profile for personal targets.");
        }

        double bmr = 10 * weightKg + 6.25 * heightCm - 5 * age + sexOffset(gender);
        // A deficit never takes intake below resting needs, or below 1,200 kcal.
        double calories = Math.max(bmr * ACTIVITY_FACTOR + calorieAdjustment(goal), Math.max(MIN_CALORIES, bmr));
        int kcal = (int) (Math.round(calories / 10.0) * 10);

        int protein = (int) Math.round(weightKg * proteinPerKg(goal));
        int fat = (int) Math.round(kcal * FAT_SHARE / 9);
        int carbs = (int) Math.max(0, Math.round((kcal - protein * 4 - fat * 9) / 4.0));

        String basis = "Based on your profile" + (goal != null ? " and your " + label(goal) + " goal" : "");
        return new NutritionTargets(kcal, protein, carbs, fat, true, basis);
    }

    private static double sexOffset(String gender) {
        String g = gender == null ? "" : gender.trim().toLowerCase(Locale.ROOT);
        if (g.startsWith("m")) return 5;
        if (g.startsWith("f")) return -161;
        // Midpoint when gender isn't male or female.
        return -78;
    }

    private static double calorieAdjustment(Goal.GoalType goal) {
        if (goal == null) return 0;
        return switch (goal) {
            case LOSE_WEIGHT -> -500;
            case GAIN_WEIGHT -> 350;
            case BUILD_STRENGTH -> 200;
            case RUN_MORE -> 150;
            case STAY_ACTIVE -> 0;
        };
    }

    private static double proteinPerKg(Goal.GoalType goal) {
        if (goal == null) return 1.4;
        return switch (goal) {
            case LOSE_WEIGHT, BUILD_STRENGTH -> 2.0;
            case GAIN_WEIGHT -> 1.8;
            case RUN_MORE -> 1.6;
            case STAY_ACTIVE -> 1.4;
        };
    }

    private static String label(Goal.GoalType goal) {
        return switch (goal) {
            case LOSE_WEIGHT -> "Lose Weight";
            case GAIN_WEIGHT -> "Gain Weight";
            case BUILD_STRENGTH -> "Build Strength";
            case RUN_MORE -> "Run More";
            case STAY_ACTIVE -> "Stay Active";
        };
    }
}
