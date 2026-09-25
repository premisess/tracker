package com.fitness.tracker.service;

import com.fitness.tracker.dto.PlanDtos.ActivePlan;
import com.fitness.tracker.exception.UnauthorizedException;
import com.fitness.tracker.dto.DashboardSummary;
import com.fitness.tracker.entity.BmiRecord;
import com.fitness.tracker.entity.Goal;
import com.fitness.tracker.entity.Profile;
import com.fitness.tracker.entity.User;
import com.fitness.tracker.entity.WaterIntake;
import com.fitness.tracker.entity.Workout;
import com.fitness.tracker.repository.BmiRecordRepository;
import com.fitness.tracker.repository.GoalRepository;
import com.fitness.tracker.repository.ProfileRepository;
import com.fitness.tracker.repository.UserRepository;
import com.fitness.tracker.repository.WaterIntakeRepository;
import com.fitness.tracker.repository.WorkoutRepository;
import com.fitness.tracker.security.SecurityUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Aggregates the other feature areas into one call so the frontend "journey guide"
 * can tell a new user what to do next, in order: profile -> goal -> first workout -> keep the streak.
 */
@Service
public class DashboardService {

    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final GoalRepository goalRepository;
    private final WorkoutRepository workoutRepository;
    private final BmiRecordRepository bmiRecordRepository;
    private final WaterIntakeRepository waterIntakeRepository;
    private final StreakService streakService;
    private final BadgeService badgeService;
    private final NutritionService nutritionService;
    private final PlanService planService;

    public DashboardService(UserRepository userRepository, ProfileRepository profileRepository,
                             GoalRepository goalRepository, WorkoutRepository workoutRepository,
                             BmiRecordRepository bmiRecordRepository, WaterIntakeRepository waterIntakeRepository,
                             StreakService streakService, BadgeService badgeService,
                             NutritionService nutritionService, PlanService planService) {
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.goalRepository = goalRepository;
        this.workoutRepository = workoutRepository;
        this.bmiRecordRepository = bmiRecordRepository;
        this.waterIntakeRepository = waterIntakeRepository;
        this.streakService = streakService;
        this.badgeService = badgeService;
        this.nutritionService = nutritionService;
        this.planService = planService;
    }

    @Transactional(readOnly = true)
    public DashboardSummary getSummary() {
        User user = getCurrentUser();

        Optional<Profile> profileOpt = profileRepository.findByUserId(user.getId());
        boolean profileComplete = profileOpt.map(this::isComplete).orElse(false);

        List<Goal> goals = goalRepository.findByUserId(user.getId());
        long activeGoals = goals.stream().filter(g -> g.getStatus() == Goal.Status.IN_PROGRESS).count();

        List<Workout> workouts = workoutRepository.findByUserIdOrderByDateDesc(user.getId());
        long workoutCount = workouts.size();
        int totalCalories = workouts.stream()
                .mapToInt(w -> w.getCaloriesBurned() != null ? w.getCaloriesBurned() : 0)
                .sum();

        int currentStreak = streakService.getCurrentStreak(user.getId());
        int longestStreak = streakService.getLongestStreak(user.getId());

        List<WaterIntake> waterLogs = waterIntakeRepository.findByUserIdOrderByDateDesc(user.getId());
        int waterToday = waterLogs.stream()
                .filter(w -> LocalDate.now().equals(w.getDate()))
                .mapToInt(w -> w.getAmountMl() != null ? w.getAmountMl() : 0)
                .sum();

        List<BmiRecord> bmiHistory = bmiRecordRepository.findByUserIdOrderByDateDesc(user.getId());
        BmiRecord latestBmi = bmiHistory.isEmpty() ? null : bmiHistory.get(0);

        Optional<ActivePlan> plan = planService.activeFor(user);

        String nextStep;
        if (!profileComplete) {
            nextStep = "Complete your profile with your age, gender, weight and height so we can personalize your plan.";
        } else if (goals.isEmpty()) {
            nextStep = "Set your first goal to start tracking progress.";
        } else if (workoutCount == 0) {
            nextStep = "Log your first workout to kick off your streak.";
        } else if (plan.isPresent() && plan.get().nextSession() != null) {
            nextStep = "Your next session in " + plan.get().plan().name() + " is " + plan.get().nextSession().title() + ".";
        } else if (currentStreak == 0) {
            nextStep = "Your streak reset. Log a workout today to start a new one.";
        } else {
            nextStep = "You're on a " + currentStreak + " day streak. Keep it going!";
        }

        return new DashboardSummary(
                user.getName(),
                profileComplete,
                goals.size(),
                activeGoals,
                workoutCount,
                currentStreak,
                latestBmi != null ? latestBmi.getBmiValue() : null,
                latestBmi != null ? latestBmi.getCategory() : null,
                nextStep,
                totalCalories,
                waterToday,
                !waterLogs.isEmpty(),
                longestStreak,
                user.isEmailVerified(),
                badgeService.earnedCount(user),
                nutritionService.caloriesEaten(user, LocalDate.now()),
                nutritionService.targetsFor(user).calories(),
                plan.map(p -> p.plan().name()).orElse(null),
                plan.map(p -> p.plan().slug()).orElse(null),
                plan.map(ActivePlan::nextSession).map(s -> s.title()).orElse(null),
                plan.map(ActivePlan::progressPercent).orElse(null)
        );
    }

    private boolean isComplete(Profile profile) {
        return profile.getAge() != null && profile.getGender() != null
                && profile.getWeight() != null && profile.getHeight() != null;
    }

    private User getCurrentUser() {
        String email = SecurityUtil.getCurrentUserEmail();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException("Your session has expired. Please sign in again."));
    }
}
