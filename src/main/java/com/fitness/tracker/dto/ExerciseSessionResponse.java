package com.fitness.tracker.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/** How one exercise went in one workout; a list of these draws the progression chart. */
@Data
@AllArgsConstructor
public class ExerciseSessionResponse {
    private Long workoutId;
    private LocalDate date;
    private List<ExerciseSetResponse> sets;
    private Double bestE1rmKg;
    private Double heaviestKg;
    private Integer maxReps;
    private Integer longestSec;
    private double volumeKg;
}
