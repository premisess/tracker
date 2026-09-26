package com.fitness.tracker.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class SleepDtos {

    private SleepDtos() {
    }

    /** Logging a night again replaces the earlier entry for that morning. */
    @Data
    public static class SleepRequest {
        @NotNull(message = "Choose when you went to bed")
        private LocalDateTime bedTime;

        @NotNull(message = "Choose when you woke up")
        private LocalDateTime wakeTime;

        @Min(value = 1, message = "Quality is from 1 to 5")
        @Max(value = 5, message = "Quality is from 1 to 5")
        private Integer quality;

        @Size(max = 280, message = "Keep notes under 280 characters")
        private String notes;
    }

    public record SleepEntry(
            Long id,
            LocalDate sleepDate,
            LocalDateTime bedTime,
            LocalDateTime wakeTime,
            int minutes,
            Integer quality,
            String notes) {
    }

    /**
     * averageMinutes covers the nights logged in the last 7 days (null when none);
     * entries are the last {@code days} days, newest first.
     */
    public record SleepSummary(
            Integer averageMinutes,
            int nightsLoggedThisWeek,
            int goalMinutes,
            int nightsOnGoalThisWeek,
            List<SleepEntry> entries) {
    }
}
