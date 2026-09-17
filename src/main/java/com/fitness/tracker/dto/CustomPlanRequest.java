package com.fitness.tracker.dto;

import com.fitness.tracker.entity.Goal;
import com.fitness.tracker.entity.PlanDay;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/** A plan built by the user: one session per training day, repeated every week. */
@Data
public class CustomPlanRequest {
    @NotBlank(message = "Give your plan a name")
    @Size(max = 100, message = "Plan name must be at most 100 characters")
    private String name;

    @Size(max = 255, message = "Summary must be at most 255 characters")
    private String summary;

    @Size(max = 4000, message = "Description must be at most 4000 characters")
    private String description;

    @NotNull(message = "Choose the plan's goal")
    private Goal.GoalType goal;

    @NotNull(message = "Choose a level")
    @Pattern(regexp = "beginner|intermediate|expert", message = "Level must be beginner, intermediate or expert")
    private String level;

    @Size(max = 60, message = "Equipment must be at most 60 characters")
    private String equipment;

    @NotNull(message = "Choose how many weeks the plan lasts")
    @Min(value = 1, message = "A plan lasts at least 1 week")
    @Max(value = 52, message = "A plan can last at most 52 weeks")
    private Integer durationWeeks;

    @NotNull(message = "Choose how many days a week you train")
    @Min(value = 1, message = "Train at least 1 day a week")
    @Max(value = 7, message = "There are only 7 days in a week")
    private Integer daysPerWeek;

    @NotNull(message = "Add your sessions")
    @Size(min = 1, max = 7, message = "Add between 1 and 7 sessions")
    private List<@Valid @NotNull Session> sessions;

    @AssertTrue(message = "Add one session for each training day")
    public boolean isOneSessionPerDay() {
        return sessions == null || daysPerWeek == null || sessions.size() == daysPerWeek;
    }

    @Data
    public static class Session {
        @NotBlank(message = "Every session needs a title")
        @Size(max = 80, message = "Session titles must be at most 80 characters")
        private String title;

        @Size(max = 160, message = "Session focus must be at most 160 characters")
        private String focus;

        @NotNull(message = "Choose strength or run for every session")
        private PlanDay.Activity activity;

        @NotBlank(message = "Choose a workout type for every session")
        private String workoutType;

        @NotNull(message = "Set how long each session takes")
        @Min(value = 5, message = "A session takes at least 5 minutes")
        @Max(value = 300, message = "A session can take at most 300 minutes")
        private Integer targetMinutes;

        @Min(value = 100, message = "Run distance must be at least 100 m")
        @Max(value = 100000, message = "Run distance must be at most 100 km")
        private Integer targetDistanceM;

        @Size(max = 500, message = "Instructions must be at most 500 characters")
        private String instructions;

        @Size(max = 20, message = "At most 20 exercises per session")
        private List<@Valid @NotNull SessionExercise> exercises;

        @AssertTrue(message = "Add at least one exercise to every strength session")
        public boolean isStrengthSessionHasExercises() {
            return activity != PlanDay.Activity.STRENGTH || (exercises != null && !exercises.isEmpty());
        }
    }

    @Data
    public static class SessionExercise {
        @NotNull(message = "Pick an exercise")
        private Long exerciseId;

        @NotNull(message = "Set the number of sets")
        @Min(value = 1, message = "At least 1 set")
        @Max(value = 20, message = "At most 20 sets")
        private Integer sets;

        @NotBlank(message = "Set the reps, e.g. 8-12 or 30 s")
        @Size(max = 20, message = "Reps must be at most 20 characters")
        private String reps;

        @NotNull(message = "Set the rest time")
        @Min(value = 0, message = "Rest can't be negative")
        @Max(value = 600, message = "Rest can be at most 10 minutes")
        private Integer restSec;
    }
}
