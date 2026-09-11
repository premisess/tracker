package com.fitness.tracker.service;

import com.fitness.tracker.entity.Workout;
import com.fitness.tracker.repository.WorkoutRepository;
import com.fitness.tracker.repository.UserRepository;
import com.fitness.tracker.security.SecurityUtil;
import com.fitness.tracker.entity.User;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class StreakService {

    private final WorkoutRepository workoutRepository;
    private final UserRepository userRepository;

    public StreakService(WorkoutRepository workoutRepository, UserRepository userRepository) {
        this.workoutRepository = workoutRepository;
        this.userRepository = userRepository;
    }

    public int getCurrentStreak(Long userId) {
        List<LocalDate> workoutDates = workoutRepository
                .findByUserIdOrderByDateDesc(userId)
                .stream()
                .map(Workout::getDate)
                .distinct()
                .sorted((a, b) -> b.compareTo(a))
                .collect(Collectors.toList());

        if (workoutDates.isEmpty()) return 0;

        int streak = 0;
        LocalDate expected = LocalDate.now();

        // Allow today or yesterday as start
        if (!workoutDates.get(0).equals(expected) && !workoutDates.get(0).equals(expected.minusDays(1))) {
            return 0;
        }

        expected = workoutDates.get(0);

        for (LocalDate date : workoutDates) {
            if (date.equals(expected)) {
                streak++;
                expected = expected.minusDays(1);
            } else {
                break;
            }
        }

        return streak;
    }

    public int getLongestStreak(Long userId) {
        List<LocalDate> workoutDates = workoutRepository
                .findByUserIdOrderByDateDesc(userId)
                .stream()
                .map(Workout::getDate)
                .distinct()
                .sorted()
                .collect(Collectors.toList());

        if (workoutDates.isEmpty()) return 0;

        int longest = 1;
        int current = 1;

        for (int i = 1; i < workoutDates.size(); i++) {
            if (workoutDates.get(i).equals(workoutDates.get(i - 1).plusDays(1))) {
                current++;
                longest = Math.max(longest, current);
            } else {
                current = 1;
            }
        }

        return longest;
    }
}