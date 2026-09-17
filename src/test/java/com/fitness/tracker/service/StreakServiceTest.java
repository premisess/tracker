package com.fitness.tracker.service;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class StreakServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 9, 17);

    private static LocalDate daysAgo(int n) {
        return TODAY.minusDays(n);
    }

    @Test
    void consecutiveDaysEndingTodayCount() {
        assertEquals(3, StreakService.currentStreak(Set.of(daysAgo(0), daysAgo(1), daysAgo(2)), Set.of(), TODAY));
    }

    @Test
    void notHavingWorkedOutYetTodayDoesNotBreakTheStreak() {
        assertEquals(2, StreakService.currentStreak(Set.of(daysAgo(1), daysAgo(2)), Set.of(), TODAY));
    }

    @Test
    void aMissedDayBreaksTheStreak() {
        assertEquals(0, StreakService.currentStreak(Set.of(daysAgo(2), daysAgo(3)), Set.of(), TODAY));
        assertEquals(1, StreakService.currentStreak(Set.of(daysAgo(0), daysAgo(2), daysAgo(3)), Set.of(), TODAY));
    }

    @Test
    void aFrozenDayBridgesTheGapWithoutAddingToTheCount() {
        Set<LocalDate> workouts = Set.of(daysAgo(0), daysAgo(2), daysAgo(3));

        assertEquals(3, StreakService.currentStreak(workouts, Set.of(daysAgo(1)), TODAY));
    }

    @Test
    void frozenYesterdayKeepsAStreakAliveBeforeTodaysWorkout() {
        assertEquals(2, StreakService.currentStreak(Set.of(daysAgo(2), daysAgo(3)), Set.of(daysAgo(1)), TODAY));
    }

    @Test
    void longestStreakUsesFreezesToo() {
        Set<LocalDate> workouts = Set.of(daysAgo(20), daysAgo(19), daysAgo(17), daysAgo(16), daysAgo(10));

        assertEquals(2, StreakService.longestStreak(workouts, Set.of()));
        assertEquals(4, StreakService.longestStreak(workouts, Set.of(daysAgo(18))));
    }

    @Test
    void noWorkoutsMeansNoStreak() {
        assertEquals(0, StreakService.currentStreak(Set.of(), Set.of(daysAgo(1)), TODAY));
        assertEquals(0, StreakService.longestStreak(Set.of(), Set.of()));
    }
}
