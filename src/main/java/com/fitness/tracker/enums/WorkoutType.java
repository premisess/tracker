package com.fitness.tracker.enums;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.util.Arrays;
import java.util.Optional;

/**
 * Fixed catalog of workout types the frontend dropdown must send.
 * MET (Metabolic Equivalent of Task) values are standard averages from the
 * Compendium of Physical Activities, used to auto-calculate calories burned:
 * calories = MET * weightKg * durationHours.
 *
 * Serialized as an object (not a bare string) so GET /api/workouts/types gives
 * the frontend both the enum name and a human-readable label to display.
 */
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum WorkoutType {
    RUNNING("Running", 9.8),
    WALKING("Walking", 3.8),
    CYCLING("Cycling", 7.5),
    SWIMMING("Swimming", 8.0),
    WEIGHTLIFTING("Weightlifting", 6.0),
    YOGA("Yoga", 2.5),
    HIIT("HIIT", 8.0),
    PILATES("Pilates", 3.0),
    JUMP_ROPE("Jump Rope", 11.0),
    DANCING("Dancing", 5.0),
    BOXING("Boxing", 9.0),
    HIKING("Hiking", 6.0),
    ROWING("Rowing", 7.0),
    ELLIPTICAL("Elliptical", 5.0),
    STAIR_CLIMBING("Stair Climbing", 8.8),
    OTHER("Other", 5.0);

    private final String label;
    private final double met;

    WorkoutType(String label, double met) {
        this.label = label;
        this.met = met;
    }

    public String getLabel() {
        return label;
    }

    public double getMet() {
        return met;
    }

    public static Optional<WorkoutType> fromLabel(String label) {
        if (label == null) {
            return Optional.empty();
        }
        String normalized = label.trim();
        return Arrays.stream(values())
                .filter(t -> t.label.equalsIgnoreCase(normalized) || t.name().equalsIgnoreCase(normalized))
                .findFirst();
    }
}
