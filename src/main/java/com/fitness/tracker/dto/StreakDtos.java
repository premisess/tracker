package com.fitness.tracker.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

public final class StreakDtos {

    private StreakDtos() {
    }

    /**
     * freezableDates are recent missed days a freeze could still cover (newest first);
     * recentFreezes are days already frozen in the last 30 days.
     */
    public record StreakStatus(
            int currentStreak,
            int longestStreak,
            boolean ultimate,
            int freezesPerMonth,
            int freezesLeft,
            List<LocalDate> freezableDates,
            List<LocalDate> recentFreezes) {
    }

    @Data
    public static class FreezeRequest {
        @NotNull(message = "Choose the day to freeze")
        private LocalDate date;
    }
}
