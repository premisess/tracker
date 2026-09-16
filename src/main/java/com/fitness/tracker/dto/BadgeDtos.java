package com.fitness.tracker.dto;

import java.time.LocalDateTime;
import java.util.List;

public final class BadgeDtos {

    private BadgeDtos() {
    }

    /** progress is capped at target. Distances are in metres. */
    public record Badge(
            String code,
            String title,
            String description,
            String category,
            String icon,
            boolean earned,
            LocalDateTime earnedAt,
            long progress,
            long target) {
    }

    public record BadgeBoard(int earnedCount, int totalCount, int currentStreak, int longestStreak, List<Badge> badges) {
    }
}
