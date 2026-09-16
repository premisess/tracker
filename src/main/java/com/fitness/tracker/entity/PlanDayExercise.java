package com.fitness.tracker.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/** A prescribed exercise within a plan session, e.g. 3 sets of "8-10" with 90 s rest. */
@Entity
@Table(name = "plan_day_exercises")
@Getter
@Setter
public class PlanDayExercise {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_day_id", nullable = false)
    private PlanDay planDay;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exercise_id", nullable = false)
    private Exercise exercise;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    @Column(nullable = false)
    private Integer sets;

    // Free text so it can say "8-10", "12 each leg" or "30-45 s".
    @Column(nullable = false, length = 20)
    private String reps;

    @Column(name = "rest_sec", nullable = false)
    private Integer restSec;
}
