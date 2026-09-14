package com.fitness.tracker.service;

import com.fitness.tracker.exception.BadRequestException;
import com.fitness.tracker.exception.NotFoundException;
import com.fitness.tracker.exception.UnauthorizedException;
import com.fitness.tracker.dto.ExerciseSetDTO;
import com.fitness.tracker.dto.WorkoutDTO;
import com.fitness.tracker.dto.WorkoutExerciseDTO;
import com.fitness.tracker.dto.WorkoutExerciseResponse;
import com.fitness.tracker.dto.WorkoutResponse;
import com.fitness.tracker.entity.ExerciseSet;
import com.fitness.tracker.entity.Profile;
import com.fitness.tracker.entity.User;
import com.fitness.tracker.entity.Workout;
import com.fitness.tracker.entity.WorkoutExercise;
import com.fitness.tracker.enums.WorkoutType;
import com.fitness.tracker.repository.ExerciseRepository;
import com.fitness.tracker.repository.ProfileRepository;
import com.fitness.tracker.repository.UserRepository;
import com.fitness.tracker.repository.WorkoutRepository;
import com.fitness.tracker.security.SecurityUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
    private final ExerciseRepository exerciseRepository;
    private final GoalService goalService;
    private final CalorieService calorieService;
    private final ExerciseCatalogService exerciseCatalogService;
    private final PersonalRecordService personalRecordService;

    public WorkoutService(WorkoutRepository workoutRepository, UserRepository userRepository,
                           ProfileRepository profileRepository, ExerciseRepository exerciseRepository,
                           GoalService goalService, CalorieService calorieService,
                           ExerciseCatalogService exerciseCatalogService,
                           PersonalRecordService personalRecordService) {
        this.workoutRepository = workoutRepository;
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.exerciseRepository = exerciseRepository;
        this.goalService = goalService;
        this.calorieService = calorieService;
        this.exerciseCatalogService = exerciseCatalogService;
        this.personalRecordService = personalRecordService;
    }

    @Transactional
    public WorkoutResponse addWorkout(WorkoutDTO dto) {
        User user = getCurrentUser();
        WorkoutType type = resolveType(dto.getType());

        Workout workout = new Workout();
        workout.setUser(user);
        applyDetails(workout, user, type, dto);
        if (dto.getExercises() != null) {
            applyExercises(workout, dto.getExercises());
        }

        workout = workoutRepository.save(workout);

        // Auto-update goals based on this workout
        goalService.autoUpdateGoalsFromWorkout(
                user,
                workout.getType(),
                workout.getDuration(),
                workout.getCaloriesBurned()
        );

        WorkoutResponse response = toResponse(workout);
        response.setNewRecords(personalRecordService.newRecordsIn(workout));
        return response;
    }

    @Transactional(readOnly = true)
    public List<WorkoutResponse> getMyWorkouts() {
        User user = getCurrentUser();
        return workoutRepository.findByUserIdOrderByDateDesc(user.getId())
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public WorkoutResponse getWorkout(Long id) {
        Workout workout = workoutRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Workout not found"));
        checkOwnership(workout);
        return toResponse(workout);
    }

    @Transactional
    public WorkoutResponse updateWorkout(Long id, WorkoutDTO dto) {
        Workout workout = workoutRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Workout not found"));

        checkOwnership(workout);
        User user = getCurrentUser();
        WorkoutType type = resolveType(dto.getType());

        applyDetails(workout, user, type, dto);
        if (dto.getExercises() != null) {
            applyExercises(workout, dto.getExercises());
        }

        workout = workoutRepository.save(workout);
        WorkoutResponse response = toResponse(workout);
        response.setNewRecords(personalRecordService.newRecordsIn(workout));
        return response;
    }

    @Transactional
    public void deleteWorkout(Long id) {
        Workout workout = workoutRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Workout not found"));

        checkOwnership(workout);
        workoutRepository.delete(workout);
    }

    public List<WorkoutType> getWorkoutTypes() {
        return Arrays.asList(WorkoutType.values());
    }

    private void applyDetails(Workout workout, User user, WorkoutType type, WorkoutDTO dto) {
        workout.setType(type.getLabel());
        workout.setDuration(dto.getDuration());
        workout.setCaloriesBurned(calculateCalories(user, type, dto.getDuration()));
        workout.setDate(dto.getDate());
        workout.setNotes(dto.getNotes());
        workout.setTags(tagsToStorage(dto.getTags()));
    }

    // Replaces the workout's exercises; orphanRemoval deletes the old rows on flush.
    private void applyExercises(Workout workout, List<WorkoutExerciseDTO> exercises) {
        workout.getExercises().clear();
        int position = 0;
        for (WorkoutExerciseDTO dto : exercises) {
            WorkoutExercise we = new WorkoutExercise();
            we.setWorkout(workout);
            we.setExercise(exerciseRepository.findById(dto.getExerciseId())
                    .orElseThrow(() -> new BadRequestException("Exercise " + dto.getExerciseId() + " doesn't exist")));
            we.setSortOrder(position++);
            we.setNotes(dto.getNotes() == null || dto.getNotes().isBlank() ? null : dto.getNotes().trim());

            int setNumber = 1;
            for (ExerciseSetDTO setDto : dto.getSets()) {
                ExerciseSet set = new ExerciseSet();
                set.setWorkoutExercise(we);
                set.setSetNumber(setNumber++);
                set.setReps(setDto.getReps());
                set.setWeightKg(setDto.getWeightKg());
                set.setDurationSec(setDto.getDurationSec());
                set.setRpe(setDto.getRpe());
                set.setWarmup(Boolean.TRUE.equals(setDto.getWarmup()));
                we.getSets().add(set);
            }
            workout.getExercises().add(we);
        }
    }

    private WorkoutType resolveType(String type) {
        return WorkoutType.fromLabel(type)
                .orElseThrow(() -> new BadRequestException(
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
            throw new NotFoundException("Workout not found");
        }
    }

    private WorkoutResponse toResponse(Workout workout) {
        WorkoutResponse response = new WorkoutResponse();
        response.setId(workout.getId());
        response.setType(workout.getType());
        response.setDuration(workout.getDuration());
        response.setCaloriesBurned(workout.getCaloriesBurned());
        response.setDate(workout.getDate());
        response.setNotes(workout.getNotes());
        response.setTags(tagsFromStorage(workout.getTags()));
        response.setSource(workout.getSource() != null ? workout.getSource().name() : Workout.ActivitySource.MANUAL.name());
        response.setDistanceMeters(workout.getDistanceMeters());
        response.setMovingTimeSec(workout.getMovingTimeSec());
        response.setAvgPaceSecPerKm(workout.getAvgPaceSecPerKm());
        response.setElevationGainM(workout.getElevationGainM());

        List<WorkoutExerciseResponse> exercises = workout.getExercises().stream()
                .map(this::toExerciseResponse)
                .toList();
        response.setExercises(exercises);
        double volume = exercises.stream().mapToDouble(WorkoutExerciseResponse::getVolumeKg).sum();
        response.setTotalVolumeKg(volume > 0 ? StrengthMath.round1(volume) : null);
        return response;
    }

    private WorkoutExerciseResponse toExerciseResponse(WorkoutExercise we) {
        return new WorkoutExerciseResponse(
                we.getId(),
                we.getExercise().getId(),
                we.getExercise().getName(),
                exerciseCatalogService.thumbnailUrl(we.getExercise()),
                we.getExercise().getTrackingType().name(),
                we.getSortOrder(),
                we.getNotes(),
                we.getSets().stream().map(PersonalRecordService::toSetResponse).toList(),
                StrengthMath.volume(we.getSets()));
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
                .orElseThrow(() -> new UnauthorizedException("Your session has expired. Please sign in again."));
    }

    @Transactional(readOnly = true)
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
