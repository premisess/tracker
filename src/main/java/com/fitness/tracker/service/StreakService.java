package com.fitness.tracker.service;

import com.fitness.tracker.dto.StreakDtos.StreakStatus;
import com.fitness.tracker.entity.StreakFreeze;
import com.fitness.tracker.entity.User;
import com.fitness.tracker.exception.BadRequestException;
import com.fitness.tracker.exception.ConflictException;
import com.fitness.tracker.repository.StreakFreezeRepository;
import com.fitness.tracker.repository.WorkoutRepository;
import com.fitness.tracker.security.CurrentUserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Workout streaks: consecutive days with at least one workout. A frozen day (Ultimate) bridges a gap
 * without adding to the count. Today never breaks a streak, since the day isn't over yet.
 */
@Service
public class StreakService {

    public static final int FREEZES_PER_MONTH = 2;
    // How far back a missed day can still be frozen.
    public static final int FREEZE_WINDOW_DAYS = 7;

    private final WorkoutRepository workoutRepository;
    private final StreakFreezeRepository streakFreezeRepository;
    private final CurrentUserService currentUserService;
    private final UltimateGuard ultimateGuard;

    public StreakService(WorkoutRepository workoutRepository, StreakFreezeRepository streakFreezeRepository,
                         CurrentUserService currentUserService, UltimateGuard ultimateGuard) {
        this.workoutRepository = workoutRepository;
        this.streakFreezeRepository = streakFreezeRepository;
        this.currentUserService = currentUserService;
        this.ultimateGuard = ultimateGuard;
    }

    public int getCurrentStreak(Long userId) {
        return currentStreak(workoutDates(userId), frozenDates(userId), LocalDate.now());
    }

    public int getLongestStreak(Long userId) {
        return longestStreak(workoutDates(userId), frozenDates(userId));
    }

    @Transactional(readOnly = true)
    public StreakStatus status() {
        return statusFor(currentUserService.get());
    }

    /** Uses one of this month's freezes on a missed day from the last week. */
    @Transactional
    public StreakStatus freeze(LocalDate date) {
        User user = currentUserService.get();
        ultimateGuard.require(user, "Streak freezes");

        LocalDate today = LocalDate.now();
        if (!date.isBefore(today) || date.isBefore(today.minusDays(FREEZE_WINDOW_DAYS))) {
            throw new BadRequestException("You can freeze a missed day from the last " + FREEZE_WINDOW_DAYS + " days.");
        }
        Set<LocalDate> active = workoutDates(user.getId());
        if (active.contains(date)) {
            throw new BadRequestException("You worked out that day, so there's nothing to freeze.");
        }
        if (frozenDates(user.getId()).contains(date)) {
            throw new ConflictException("That day is already frozen.");
        }
        if (freezesUsedThisMonth(user) >= FREEZES_PER_MONTH) {
            throw new BadRequestException("You've used both streak freezes this month. You get " + FREEZES_PER_MONTH + " more on the 1st.");
        }

        StreakFreeze freeze = new StreakFreeze();
        freeze.setUser(user);
        freeze.setFreezeDate(date);
        freeze.setUsedAt(LocalDateTime.now());
        streakFreezeRepository.save(freeze);
        return statusFor(user);
    }

    private StreakStatus statusFor(User user) {
        LocalDate today = LocalDate.now();
        Set<LocalDate> active = workoutDates(user.getId());
        Set<LocalDate> frozen = frozenDates(user.getId());
        boolean ultimate = UltimateGuard.hasUltimate(user);

        // Only days after the first workout can be frozen; before that there was no streak to protect.
        List<LocalDate> freezable = new ArrayList<>();
        if (!active.isEmpty()) {
            LocalDate firstWorkout = Collections.min(active);
            for (int i = 1; i <= FREEZE_WINDOW_DAYS; i++) {
                LocalDate day = today.minusDays(i);
                if (day.isAfter(firstWorkout) && !active.contains(day) && !frozen.contains(day)) {
                    freezable.add(day);
                }
            }
        }
        List<LocalDate> recent = frozen.stream()
                .filter(d -> !d.isBefore(today.minusDays(30)))
                .sorted(Comparator.reverseOrder())
                .toList();

        return new StreakStatus(
                currentStreak(active, frozen, today),
                longestStreak(active, frozen),
                ultimate,
                FREEZES_PER_MONTH,
                ultimate ? (int) Math.max(0, FREEZES_PER_MONTH - freezesUsedThisMonth(user)) : 0,
                freezable,
                recent);
    }

    static int currentStreak(Set<LocalDate> active, Set<LocalDate> frozen, LocalDate today) {
        if (active.isEmpty()) {
            return 0;
        }
        LocalDate earliest = Collections.min(active);
        LocalDate day = today;
        if (!active.contains(day) && !frozen.contains(day)) {
            day = day.minusDays(1);
        }
        int streak = 0;
        while (!day.isBefore(earliest)) {
            if (active.contains(day)) {
                streak++;
            } else if (!frozen.contains(day)) {
                break;
            }
            day = day.minusDays(1);
        }
        return streak;
    }

    static int longestStreak(Set<LocalDate> active, Set<LocalDate> frozen) {
        if (active.isEmpty()) {
            return 0;
        }
        LocalDate last = Collections.max(active);
        int current = 0;
        int longest = 0;
        for (LocalDate day = Collections.min(active); !day.isAfter(last); day = day.plusDays(1)) {
            if (active.contains(day)) {
                current++;
                longest = Math.max(longest, current);
            } else if (!frozen.contains(day)) {
                current = 0;
            }
        }
        return longest;
    }

    private long freezesUsedThisMonth(User user) {
        LocalDateTime monthStart = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        return streakFreezeRepository.countByUserIdAndUsedAtGreaterThanEqual(user.getId(), monthStart);
    }

    private Set<LocalDate> workoutDates(Long userId) {
        return new HashSet<>(workoutRepository.findWorkoutDates(userId));
    }

    private Set<LocalDate> frozenDates(Long userId) {
        return new HashSet<>(streakFreezeRepository.findDates(userId));
    }
}
