package com.fitness.tracker.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/** One exercise performed within a workout, in the order it was done. */
@Entity
@Table(name = "workout_exercises")
@Getter
@Setter
public class WorkoutExercise {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workout_id", nullable = false)
    private Workout workout;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exercise_id", nullable = false)
    private Exercise exercise;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    private String notes;

    @OneToMany(mappedBy = "workoutExercise", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("setNumber")
    private List<ExerciseSet> sets = new ArrayList<>();
}
