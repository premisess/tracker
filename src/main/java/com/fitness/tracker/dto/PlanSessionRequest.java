package com.fitness.tracker.dto;

import jakarta.validation.constraints.AssertTrue;
import lombok.Data;

/** Marks the next plan session as done by a logged workout, or skipped. */
@Data
public class PlanSessionRequest {
    private Long workoutId;

    private boolean skip;

    @AssertTrue(message = "Choose the workout that completed this session, or skip it")
    public boolean isWorkoutOrSkip() {
        return skip || workoutId != null;
    }
}
