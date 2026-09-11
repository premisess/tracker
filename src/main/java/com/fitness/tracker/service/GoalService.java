package com.fitness.tracker.service;

import com.fitness.tracker.dto.GoalDTO;
import com.fitness.tracker.dto.GoalResponse;
import com.fitness.tracker.entity.Goal;
import com.fitness.tracker.entity.User;
import com.fitness.tracker.repository.GoalRepository;
import com.fitness.tracker.repository.UserRepository;
import com.fitness.tracker.security.SecurityUtil;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class GoalService {

    private final GoalRepository goalRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public GoalService(GoalRepository goalRepository, UserRepository userRepository,
                        NotificationService notificationService) {
        this.goalRepository = goalRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    public GoalResponse addGoal(GoalDTO dto) {
        User user = getCurrentUser();

        Goal goal = new Goal();
        goal.setUser(user);
        goal.setTitle(dto.getTitle());
        goal.setGoalType(dto.getGoalType());
        goal.setTargetValue(dto.getTargetValue());
        goal.setCurrentProgress(dto.getCurrentProgress() != null ? dto.getCurrentProgress() : 0.0);
        goal.setUnit(dto.getUnit());
        goal.setDeadline(dto.getDeadline());
        goal.setAutoTrack(dto.getAutoTrack() != null ? dto.getAutoTrack() : true);
        goal.setStatus(Goal.Status.IN_PROGRESS);

        goal = goalRepository.save(goal);
        return toResponse(goal);
    }

    public List<GoalResponse> getMyGoals() {
        User user = getCurrentUser();
        return goalRepository.findByUserId(user.getId())
                .stream()
                .map(this::toResponseWithStatusCheck)
                .collect(Collectors.toList());
    }

    public GoalResponse updateProgress(Long id, Double newProgress) {
        Goal goal = goalRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Goal not found"));
        checkOwnership(goal);
        goal.setCurrentProgress(newProgress);
        updateStatus(goal);
        goal = goalRepository.save(goal);
        return toResponse(goal);
    }

    public GoalResponse updateGoal(Long id, GoalDTO dto) {
        Goal goal = goalRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Goal not found"));
        checkOwnership(goal);
        goal.setTitle(dto.getTitle());
        goal.setGoalType(dto.getGoalType());
        goal.setTargetValue(dto.getTargetValue());
        goal.setCurrentProgress(dto.getCurrentProgress());
        goal.setUnit(dto.getUnit());
        goal.setDeadline(dto.getDeadline());
        goal.setAutoTrack(dto.getAutoTrack() != null ? dto.getAutoTrack() : true);
        updateStatus(goal);
        goal = goalRepository.save(goal);
        return toResponse(goal);
    }

    public void deleteGoal(Long id) {
        Goal goal = goalRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Goal not found"));
        checkOwnership(goal);
        goalRepository.delete(goal);
    }

    // Called automatically when a workout is logged
    public void autoUpdateGoalsFromWorkout(User user, String workoutType, Integer duration, Integer caloriesBurned) {
        List<Goal> goals = goalRepository.findByUserId(user.getId());

        for (Goal goal : goals) {
            if (!Boolean.TRUE.equals(goal.getAutoTrack())) continue;
            if (goal.getStatus() != Goal.Status.IN_PROGRESS) continue;

            switch (goal.getGoalType()) {
                case LOSE_WEIGHT:
                    // 7700 kcal = 1 kg lost
                    double kgLost = caloriesBurned / 7700.0;
                    goal.setCurrentProgress(
                            Math.min(
                                    (goal.getCurrentProgress() == null ? 0 : goal.getCurrentProgress()) + kgLost,
                                    goal.getTargetValue()
                            )
                    );
                    break;

                case RUN_MORE:
                    // Only count Running workouts
                    if (workoutType.equalsIgnoreCase("Running")) {
                        // Estimate km: average running speed 8km/h
                        double km = (duration / 60.0) * 8.0;
                        goal.setCurrentProgress(
                                (goal.getCurrentProgress() == null ? 0 : goal.getCurrentProgress()) + km
                        );
                    }
                    break;

                case BUILD_STRENGTH:
                    // Count Weightlifting, HIIT, Boxing workouts
                    if (workoutType.equalsIgnoreCase("Weightlifting") ||
                            workoutType.equalsIgnoreCase("HIIT") ||
                            workoutType.equalsIgnoreCase("Boxing")) {
                        goal.setCurrentProgress(
                                (goal.getCurrentProgress() == null ? 0 : goal.getCurrentProgress()) + 1
                        );
                    }
                    break;

                case STAY_ACTIVE:
                    // Any workout counts
                    goal.setCurrentProgress(
                            (goal.getCurrentProgress() == null ? 0 : goal.getCurrentProgress()) + 1
                    );
                    break;

                case GAIN_WEIGHT:
                    // Gain weight goals are manual — user updates after weighing themselves
                    break;
            }

            updateStatus(goal);
            goalRepository.save(goal);
        }
    }

    private void updateStatus(Goal goal) {
        Goal.Status previousStatus = goal.getStatus();

        if (goal.getCurrentProgress() != null && goal.getTargetValue() != null
                && goal.getCurrentProgress() >= goal.getTargetValue()) {
            goal.setStatus(Goal.Status.COMPLETED);
        } else if (goal.getDeadline() != null && goal.getDeadline().isBefore(LocalDate.now())) {
            goal.setStatus(Goal.Status.FAILED);
        } else {
            goal.setStatus(Goal.Status.IN_PROGRESS);
        }

        if (goal.getStatus() == Goal.Status.COMPLETED && previousStatus != Goal.Status.COMPLETED) {
            notificationService.sendGoalAchievedEmail(goal.getUser(), goal);
        }
    }

    private GoalResponse toResponseWithStatusCheck(Goal goal) {
        updateStatus(goal);
        goalRepository.save(goal);
        return toResponse(goal);
    }

    private void checkOwnership(Goal goal) {
        User user = getCurrentUser();
        if (!goal.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("You do not have permission to modify this goal");
        }
    }

    private GoalResponse toResponse(Goal goal) {
        double percent = 0.0;
        if (goal.getTargetValue() != null && goal.getTargetValue() > 0 && goal.getCurrentProgress() != null) {
            percent = (goal.getCurrentProgress() / goal.getTargetValue()) * 100;
            if (percent > 100) percent = 100;
        }

        return new GoalResponse(
                goal.getId(),
                goal.getTitle(),
                goal.getGoalType() != null ? goal.getGoalType().name() : null,
                goal.getTargetValue(),
                goal.getCurrentProgress(),
                goal.getUnit(),
                goal.getDeadline(),
                goal.getStatus().name(),
                Math.round(percent * 10.0) / 10.0,
                goal.getAutoTrack()
        );
    }

    private User getCurrentUser() {
        String email = SecurityUtil.getCurrentUserEmail();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}