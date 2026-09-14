package com.fitness.tracker.service;

import com.fitness.tracker.entity.ExerciseSet;

import java.util.Collection;

/** Shared formulas for strength training. */
public final class StrengthMath {

    // Beyond this many reps the Epley estimate stops being meaningful.
    private static final int MAX_REPS_FOR_ESTIMATE = 12;

    private StrengthMath() {
    }

    /** Epley estimated one-rep max, or null when the set can't produce a sensible estimate. */
    public static Double estimatedOneRepMax(Double weightKg, Integer reps) {
        if (weightKg == null || weightKg <= 0 || reps == null || reps < 1 || reps > MAX_REPS_FOR_ESTIMATE) {
            return null;
        }
        return round1(reps == 1 ? weightKg : weightKg * (1 + reps / 30.0));
    }

    /** Weight x reps over working sets. */
    public static double volume(Collection<ExerciseSet> sets) {
        double total = 0;
        for (ExerciseSet set : sets) {
            if (!set.isWarmup() && set.getWeightKg() != null && set.getReps() != null) {
                total += set.getWeightKg() * set.getReps();
            }
        }
        return round1(total);
    }

    public static double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
