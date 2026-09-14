package com.fitness.tracker.service;

import com.fitness.tracker.exception.UnauthorizedException;
import com.fitness.tracker.dto.AnalyticsSummary;
import com.fitness.tracker.dto.DailyPoint;
import com.fitness.tracker.dto.WorkoutTypeBreakdown;
import com.fitness.tracker.entity.BmiRecord;
import com.fitness.tracker.entity.User;
import com.fitness.tracker.entity.Workout;
import com.fitness.tracker.repository.BmiRecordRepository;
import com.fitness.tracker.repository.UserRepository;
import com.fitness.tracker.repository.WorkoutRepository;
import com.fitness.tracker.security.SecurityUtil;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AnalyticsService {

    private final WorkoutRepository workoutRepository;
    private final BmiRecordRepository bmiRecordRepository;
    private final UserRepository userRepository;
    private final StreakService streakService;

    public AnalyticsService(WorkoutRepository workoutRepository, BmiRecordRepository bmiRecordRepository,
                             UserRepository userRepository, StreakService streakService) {
        this.workoutRepository = workoutRepository;
        this.bmiRecordRepository = bmiRecordRepository;
        this.userRepository = userRepository;
        this.streakService = streakService;
    }

    public AnalyticsSummary getSummary() {
        User user = getCurrentUser();
        List<Workout> workouts = workoutRepository.findByUserIdOrderByDateDesc(user.getId());

        long totalWorkouts = workouts.size();
        int totalCalories = workouts.stream().mapToInt(w -> w.getCaloriesBurned() != null ? w.getCaloriesBurned() : 0).sum();
        int totalDuration = workouts.stream().mapToInt(w -> w.getDuration() != null ? w.getDuration() : 0).sum();
        double avgCalories = totalWorkouts > 0 ? Math.round((totalCalories / (double) totalWorkouts) * 10.0) / 10.0 : 0.0;

        List<WorkoutTypeBreakdown> byType = workouts.stream()
                .collect(Collectors.groupingBy(Workout::getType))
                .entrySet().stream()
                .map(e -> new WorkoutTypeBreakdown(
                        e.getKey(),
                        e.getValue().size(),
                        e.getValue().stream().mapToInt(w -> w.getCaloriesBurned() != null ? w.getCaloriesBurned() : 0).sum(),
                        e.getValue().stream().mapToInt(w -> w.getDuration() != null ? w.getDuration() : 0).sum()
                ))
                .sorted(Comparator.comparingLong(WorkoutTypeBreakdown::getCount).reversed())
                .collect(Collectors.toList());

        List<DailyPoint> caloriesOverTime = workouts.stream()
                .collect(Collectors.groupingBy(Workout::getDate,
                        Collectors.summingInt(w -> w.getCaloriesBurned() != null ? w.getCaloriesBurned() : 0)))
                .entrySet().stream()
                .map(e -> new DailyPoint(e.getKey(), e.getValue()))
                .sorted(Comparator.comparing(DailyPoint::getDate))
                .collect(Collectors.toList());

        List<DailyPoint> weightTrend = bmiRecordRepository.findByUserIdOrderByDateDesc(user.getId())
                .stream()
                .filter(b -> b.getDate() != null && b.getWeight() != null)
                .sorted(Comparator.comparing(BmiRecord::getDate))
                .map(b -> new DailyPoint(b.getDate(), b.getWeight()))
                .collect(Collectors.toList());

        return new AnalyticsSummary(
                totalWorkouts,
                totalCalories,
                totalDuration,
                avgCalories,
                streakService.getCurrentStreak(user.getId()),
                streakService.getLongestStreak(user.getId()),
                byType,
                caloriesOverTime,
                weightTrend
        );
    }

    private User getCurrentUser() {
        String email = SecurityUtil.getCurrentUserEmail();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException("Your session has expired. Please sign in again."));
    }
}
