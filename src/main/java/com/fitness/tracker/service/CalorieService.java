package com.fitness.tracker.service;

import com.fitness.tracker.enums.WorkoutType;
import org.springframework.stereotype.Service;

@Service
public class CalorieService {

    // Used when the user hasn't filled in their profile weight yet, so a workout can still be logged.
    private static final double DEFAULT_WEIGHT_KG = 70.0;

    public int calculate(WorkoutType type, int durationMinutes, Double weightKg) {
        double weight = (weightKg != null && weightKg > 0) ? weightKg : DEFAULT_WEIGHT_KG;
        double hours = durationMinutes / 60.0;
        return (int) Math.round(type.getMet() * weight * hours);
    }
}
