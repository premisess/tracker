package com.fitness.tracker.dto;

import lombok.Data;

import java.time.LocalDate;

/** A user's all-time bests for one exercise. Fields that don't apply to the exercise stay null. */
@Data
public class ExerciseRecordSummary {
    private Long exerciseId;
    private String exerciseName;
    private String category;
    private String thumbnailUrl;
    private String trackingType;

    // Estimated one-rep max (Epley), from the set that produced it.
    private Double bestE1rmKg;
    private Double bestE1rmWeightKg;
    private Integer bestE1rmReps;
    private LocalDate bestE1rmDate;

    private Double heaviestKg;
    private Integer heaviestReps;
    private LocalDate heaviestDate;

    private Integer maxReps;
    private LocalDate maxRepsDate;

    private Integer longestSec;
    private LocalDate longestDate;

    private int totalSets;
    private double totalVolumeKg;
    private int sessions;
    private LocalDate lastPerformed;
}
