package com.fitness.tracker.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/** A record the just-saved workout beat, e.g. a heavier top set than ever before. */
@Data
@AllArgsConstructor
public class PersonalRecordHit {
    private Long exerciseId;
    private String exerciseName;
    // E1RM, HEAVIEST, MOST_REPS or LONGEST
    private String type;
    private double value;
    private Double previous;
    private String description;
}
