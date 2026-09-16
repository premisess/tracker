package com.fitness.tracker.service;

import com.fitness.tracker.dto.NutritionTargets;
import com.fitness.tracker.entity.Goal;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NutritionTargetCalculatorTest {

    @Test
    void maleWithNoGoalGetsMaintenanceTargets() {
        // BMR = 800 + 1125 - 150 + 5 = 1780; x1.45 = 2581 -> 2580 kcal.
        NutritionTargets t = NutritionTargetCalculator.calculate(80.0, 180.0, 30, "Male", null);

        assertTrue(t.personalized());
        assertEquals(2580, t.calories());
        assertEquals(112, t.proteinG());      // 1.4 g/kg
        assertEquals(77, t.fatG());           // 27% of calories
        assertEquals(360, t.carbsG());        // (2580 - 448 - 693) / 4
    }

    @Test
    void weightLossGoalTakes500OffAndRaisesProtein() {
        // Female: BMR = 650 + 1031.25 - 175 - 161 = 1345.25; x1.45 - 500 = 1450.6 -> 1450 kcal.
        NutritionTargets t = NutritionTargetCalculator.calculate(65.0, 165.0, 35, "female", Goal.GoalType.LOSE_WEIGHT);

        assertEquals(1450, t.calories());
        assertEquals(130, t.proteinG());      // 2.0 g/kg
        assertTrue(t.basis().contains("Lose Weight"));
    }

    @Test
    void deficitNeverDropsBelowTheSafetyFloor() {
        // BMR = 450 + 937.5 - 300 - 161 = 926.5; x1.45 - 500 = 843, so the 1200 kcal floor applies.
        NutritionTargets t = NutritionTargetCalculator.calculate(45.0, 150.0, 60, "Female", Goal.GoalType.LOSE_WEIGHT);

        assertEquals(1200, t.calories());
    }

    @Test
    void gainGoalAddsCalories() {
        NutritionTargets maintain = NutritionTargetCalculator.calculate(70.0, 175.0, 25, "Male", null);
        NutritionTargets gain = NutritionTargetCalculator.calculate(70.0, 175.0, 25, "Male", Goal.GoalType.GAIN_WEIGHT);

        assertEquals(350, gain.calories() - maintain.calories(), 10);
    }

    @Test
    void incompleteProfileFallsBackToGeneralGuideline() {
        NutritionTargets t = NutritionTargetCalculator.calculate(70.0, null, 30, "Male", Goal.GoalType.RUN_MORE);

        assertFalse(t.personalized());
        assertEquals(2000, t.calories());
    }
}
