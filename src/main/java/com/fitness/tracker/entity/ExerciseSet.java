package com.fitness.tracker.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * A single set. Which fields are filled depends on the exercise's tracking type:
 * weight + reps for loaded lifts, reps for bodyweight moves, duration for holds and cardio.
 */
@Entity
@Table(name = "exercise_sets")
@Getter
@Setter
public class ExerciseSet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workout_exercise_id", nullable = false)
    private WorkoutExercise workoutExercise;

    @Column(name = "set_number", nullable = false)
    private Integer setNumber;

    private Integer reps;

    @Column(name = "weight_kg")
    private Double weightKg;

    @Column(name = "duration_sec")
    private Integer durationSec;

    // Rate of perceived exertion, 1-10.
    private Double rpe;

    // Warm-up sets are shown but never count toward records or volume.
    @Column(nullable = false)
    private boolean warmup;
}
