package com.fitness.tracker.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Response shapes for workout plans, grouped because they only make sense together. */
public final class PlanDtos {

    private PlanDtos() {
    }

    public record PlanSummary(
            Long id,
            String slug,
            String name,
            String summary,
            String goal,
            String level,
            String equipment,
            int durationWeeks,
            int daysPerWeek,
            int totalSessions,
            boolean active) {
    }

    public record PlanExercise(
            Long exerciseId,
            String name,
            String thumbnailUrl,
            String trackingType,
            int sets,
            String reps,
            int restSec) {
    }

    /** weekNumber is null when the session repeats every week. */
    public record PlanSession(
            Long id,
            Integer weekNumber,
            int dayNumber,
            String title,
            String focus,
            String activity,
            String workoutType,
            int targetMinutes,
            Integer targetDistanceM,
            String instructions,
            List<PlanExercise> exercises) {
    }

    public record PlanDetail(PlanSummary plan, String description, List<PlanSession> sessions) {
    }

    public record SessionLog(
            int sessionNumber,
            int weekNumber,
            int dayNumber,
            String title,
            boolean skipped,
            Long workoutId,
            LocalDateTime loggedAt) {
    }

    /**
     * The plan the user is following. currentWeek, currentDay and nextSession are null once it's finished.
     */
    public record ActivePlan(
            Long enrollmentId,
            PlanSummary plan,
            String status,
            LocalDate startedOn,
            int completedSessions,
            int totalSessions,
            int progressPercent,
            Integer currentWeek,
            Integer currentDay,
            PlanSession nextSession,
            List<SessionLog> recentSessions) {
    }
}
