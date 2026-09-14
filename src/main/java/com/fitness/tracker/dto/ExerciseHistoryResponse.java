package com.fitness.tracker.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class ExerciseHistoryResponse {
    private ExerciseResponse exercise;
    // Null until the exercise has been logged at least once.
    private ExerciseRecordSummary records;
    // Oldest first.
    private List<ExerciseSessionResponse> sessions;
}
