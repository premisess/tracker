package com.fitness.tracker.service;

import com.fitness.tracker.dto.WorkoutDTO;
import com.fitness.tracker.dto.WorkoutResponse;
import com.fitness.tracker.entity.Profile;
import com.fitness.tracker.entity.User;
import com.fitness.tracker.entity.Workout;
import com.fitness.tracker.enums.WorkoutType;
import com.fitness.tracker.repository.ProfileRepository;
import com.fitness.tracker.repository.UserRepository;
import com.fitness.tracker.repository.WorkoutRepository;
import com.fitness.tracker.security.SecurityUtil;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class WorkoutService {

    private final WorkoutRepository workoutRepository;
    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final GoalService goalService;
    private final CalorieService calorieService;

    public WorkoutService(WorkoutRepository workoutRepository, UserRepository userRepository,
                           ProfileRepository profileRepository, GoalService goalService,
                           CalorieService calorieService) {
        this.workoutRepository = workoutRepository;
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.goalService = goalService;
        this.calorieService = calorieService;
    }

    public WorkoutResponse addWorkout(WorkoutDTO dto) {
        User user = getCurrentUser();
        WorkoutType type = resolveType(dto.getType());

        Workout workout = new Workout();
        workout.setUser(user);
        workout.setType(type.getLabel());
        workout.setDuration(dto.getDuration());
        workout.setCaloriesBurned(calculateCalories(user, type, dto.getDuration()));
        workout.setDate(dto.getDate());
        workout.setNotes(dto.getNotes());
        workout.setTags(tagsToStorage(dto.getTags()));

        workout = workoutRepository.save(workout);

        // Auto-update goals based on this workout
        goalService.autoUpdateGoalsFromWorkout(
                user,
                workout.getType(),
                workout.getDuration(),
                workout.getCaloriesBurned()
        );

        return toResponse(workout);
    }

    public List<WorkoutResponse> getMyWorkouts() {
        User user = getCurrentUser();
        return workoutRepository.findByUserIdOrderByDateDesc(user.getId())
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public WorkoutResponse updateWorkout(Long id, WorkoutDTO dto) {
        Workout workout = workoutRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Workout not found"));

        checkOwnership(workout);
        User user = getCurrentUser();
        WorkoutType type = resolveType(dto.getType());

        workout.setType(type.getLabel());
        workout.setDuration(dto.getDuration());
        workout.setCaloriesBurned(calculateCalories(user, type, dto.getDuration()));
        workout.setDate(dto.getDate());
        workout.setNotes(dto.getNotes());
        workout.setTags(tagsToStorage(dto.getTags()));

        workout = workoutRepository.save(workout);
        return toResponse(workout);
    }

    public void deleteWorkout(Long id) {
        Workout workout = workoutRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Workout not found"));

        checkOwnership(workout);
        workoutRepository.delete(workout);
    }

    public List<WorkoutType> getWorkoutTypes() {
        return Arrays.asList(WorkoutType.values());
    }

    private WorkoutType resolveType(String type) {
        return WorkoutType.fromLabel(type)
                .orElseThrow(() -> new RuntimeException(
                        "Unknown workout type '" + type + "'. Valid types: " +
                                Arrays.stream(WorkoutType.values()).map(WorkoutType::getLabel).collect(Collectors.joining(", "))
                ));
    }

    private int calculateCalories(User user, WorkoutType type, Integer durationMinutes) {
        int duration = durationMinutes != null ? durationMinutes : 0;
        Double weightKg = profileRepository.findByUserId(user.getId())
                .map(Profile::getWeight)
                .orElse(null);
        return calorieService.calculate(type, duration, weightKg);
    }

    private void checkOwnership(Workout workout) {
        User user = getCurrentUser();
        if (!workout.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("You do not have permission to modify this workout");
        }
    }

    private WorkoutResponse toResponse(Workout workout) {
        return new WorkoutResponse(
                workout.getId(),
                workout.getType(),
                workout.getDuration(),
                workout.getCaloriesBurned(),
                workout.getDate(),
                workout.getNotes(),
                tagsFromStorage(workout.getTags())
        );
    }

    private String tagsToStorage(List<String> tags) {
        if (tags == null || tags.isEmpty()) return null;
        return tags.stream()
                .map(String::trim)
                .filter(t -> !t.isEmpty())
                .collect(Collectors.joining(","));
    }

    private List<String> tagsFromStorage(String stored) {
        if (stored == null || stored.isBlank()) return Collections.emptyList();
        return Arrays.asList(stored.split(","));
    }

    private User getCurrentUser() {
        String email = SecurityUtil.getCurrentUserEmail();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    public List<WorkoutResponse> searchWorkouts(String type, String startDate, String endDate, String tag) {
        User user = getCurrentUser();
        LocalDate start = startDate != null && !startDate.isEmpty() ? LocalDate.parse(startDate) : null;
        LocalDate end = endDate != null && !endDate.isEmpty() ? LocalDate.parse(endDate) : null;
        String workoutType = type != null && !type.isEmpty() ? type : null;

        List<WorkoutResponse> results = workoutRepository.searchWorkouts(user.getId(), workoutType, start, end)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        if (tag != null && !tag.isBlank()) {
            String needle = tag.trim().toLowerCase();
            results = results.stream()
                    .filter(w -> w.getTags().stream().anyMatch(t -> t.equalsIgnoreCase(needle)))
                    .collect(Collectors.toList());
        }

        return results;
    }
}
