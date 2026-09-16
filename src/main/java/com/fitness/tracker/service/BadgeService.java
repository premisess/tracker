package com.fitness.tracker.service;

import com.fitness.tracker.dto.BadgeDtos.Badge;
import com.fitness.tracker.dto.BadgeDtos.BadgeBoard;
import com.fitness.tracker.entity.Goal;
import com.fitness.tracker.entity.PlanEnrollment;
import com.fitness.tracker.entity.User;
import com.fitness.tracker.entity.UserBadge;
import com.fitness.tracker.entity.Workout;
import com.fitness.tracker.repository.FoodLogEntryRepository;
import com.fitness.tracker.repository.GoalRepository;
import com.fitness.tracker.repository.PlanEnrollmentRepository;
import com.fitness.tracker.repository.PlanSessionLogRepository;
import com.fitness.tracker.repository.UserBadgeRepository;
import com.fitness.tracker.repository.WaterIntakeRepository;
import com.fitness.tracker.repository.WorkoutRepository;
import com.fitness.tracker.security.CurrentUserService;
import com.fitness.tracker.service.BadgeDefinition.Metric;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Works out badges from the user's real activity. Badges are awarded whenever they're checked, so past
 * activity counts too, and each new badge is handed to the website exactly once for a celebration.
 */
@Service
public class BadgeService {

    private final UserBadgeRepository userBadgeRepository;
    private final WorkoutRepository workoutRepository;
    private final GoalRepository goalRepository;
    private final WaterIntakeRepository waterIntakeRepository;
    private final FoodLogEntryRepository foodLogEntryRepository;
    private final PlanSessionLogRepository planSessionLogRepository;
    private final PlanEnrollmentRepository planEnrollmentRepository;
    private final StreakService streakService;
    private final CurrentUserService currentUserService;

    public BadgeService(UserBadgeRepository userBadgeRepository, WorkoutRepository workoutRepository,
                        GoalRepository goalRepository, WaterIntakeRepository waterIntakeRepository,
                        FoodLogEntryRepository foodLogEntryRepository,
                        PlanSessionLogRepository planSessionLogRepository,
                        PlanEnrollmentRepository planEnrollmentRepository,
                        StreakService streakService, CurrentUserService currentUserService) {
        this.userBadgeRepository = userBadgeRepository;
        this.workoutRepository = workoutRepository;
        this.goalRepository = goalRepository;
        this.waterIntakeRepository = waterIntakeRepository;
        this.foodLogEntryRepository = foodLogEntryRepository;
        this.planSessionLogRepository = planSessionLogRepository;
        this.planEnrollmentRepository = planEnrollmentRepository;
        this.streakService = streakService;
        this.currentUserService = currentUserService;
    }

    /** Every badge with its progress, after awarding anything newly earned. */
    @Transactional
    public BadgeBoard board() {
        User user = currentUserService.get();
        Map<Metric, Long> stats = stats(user);
        Map<String, UserBadge> earned = award(user, stats);

        List<Badge> badges = Arrays.stream(BadgeDefinition.values())
                .map(def -> toBadge(def, earned.get(def.name()), stats))
                .toList();
        return new BadgeBoard(
                (int) badges.stream().filter(Badge::earned).count(),
                badges.size(),
                streakService.getCurrentStreak(user.getId()),
                stats.get(Metric.LONGEST_STREAK).intValue(),
                badges);
    }

    /** Awards anything newly earned and returns badges not yet shown to the user, marking them as shown. */
    @Transactional
    public List<Badge> collectNew() {
        User user = currentUserService.get();
        Map<Metric, Long> stats = stats(user);
        award(user, stats);

        List<UserBadge> unseen = userBadgeRepository.findByUserIdAndNotifiedFalseOrderByEarnedAtAsc(user.getId());
        List<Badge> result = new ArrayList<>();
        for (UserBadge badge : unseen) {
            badge.setNotified(true);
            BadgeDefinition def = definition(badge.getBadgeCode());
            if (def != null) {
                result.add(toBadge(def, badge, stats));
            }
        }
        userBadgeRepository.saveAll(unseen);
        return result;
    }

    public long earnedCount(User user) {
        return userBadgeRepository.countByUserId(user.getId());
    }

    private Map<String, UserBadge> award(User user, Map<Metric, Long> stats) {
        Map<String, UserBadge> earned = new HashMap<>();
        for (UserBadge badge : userBadgeRepository.findByUserId(user.getId())) {
            earned.put(badge.getBadgeCode(), badge);
        }
        List<UserBadge> fresh = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        for (BadgeDefinition def : BadgeDefinition.values()) {
            if (!earned.containsKey(def.name()) && stats.get(def.getMetric()) >= def.getTarget()) {
                UserBadge badge = new UserBadge();
                badge.setUser(user);
                badge.setBadgeCode(def.name());
                badge.setEarnedAt(now);
                badge.setNotified(false);
                fresh.add(badge);
                earned.put(def.name(), badge);
            }
        }
        userBadgeRepository.saveAll(fresh);
        return earned;
    }

    private Map<Metric, Long> stats(User user) {
        Long id = user.getId();
        Map<Metric, Long> stats = new EnumMap<>(Metric.class);
        stats.put(Metric.WORKOUTS, workoutRepository.countByUserId(id));
        stats.put(Metric.LONGEST_STREAK, (long) streakService.getLongestStreak(id));
        stats.put(Metric.GPS_ACTIVITIES, workoutRepository.countByUserIdAndSource(id, Workout.ActivitySource.GPS));
        stats.put(Metric.LONGEST_RUN_M, workoutRepository.longestGpsDistance(id, "Running"));
        stats.put(Metric.TOTAL_GPS_DISTANCE_M, workoutRepository.totalGpsDistance(id));
        stats.put(Metric.STRENGTH_SESSIONS, workoutRepository.countWithExercises(id));
        stats.put(Metric.GOALS_COMPLETED, goalRepository.countByUserIdAndStatus(id, Goal.Status.COMPLETED));
        stats.put(Metric.FOOD_LOG_DAYS, foodLogEntryRepository.countLoggedDays(id));
        stats.put(Metric.WATER_LOG_DAYS, waterIntakeRepository.countLoggedDays(id));
        stats.put(Metric.PLAN_SESSIONS, planSessionLogRepository.countCompletedForUser(id));
        stats.put(Metric.PLANS_COMPLETED, planEnrollmentRepository.countByUserIdAndStatus(id, PlanEnrollment.Status.COMPLETED));
        stats.put(Metric.EMAIL_VERIFIED, user.isEmailVerified() ? 1L : 0L);
        return stats;
    }

    private static Badge toBadge(BadgeDefinition def, UserBadge earned, Map<Metric, Long> stats) {
        long progress = earned != null ? def.getTarget() : Math.min(stats.get(def.getMetric()), def.getTarget());
        return new Badge(def.name(), def.getTitle(), def.getDescription(), def.getCategory(), def.getIcon(),
                earned != null, earned != null ? earned.getEarnedAt() : null, progress, def.getTarget());
    }

    private static BadgeDefinition definition(String code) {
        try {
            return BadgeDefinition.valueOf(code);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
