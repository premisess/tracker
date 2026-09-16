package com.fitness.tracker.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.List;

/** A ready-made training program: a number of weeks, each with the same count of sessions. */
@Entity
@Table(name = "workout_plans")
@Getter
@Setter
public class WorkoutPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 60)
    private String slug;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    private String summary;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private Goal.GoalType goal;

    @Column(nullable = false, length = 20)
    private String level;

    @Column(nullable = false, length = 60)
    private String equipment;

    @Column(name = "duration_weeks", nullable = false)
    private Integer durationWeeks;

    @Column(name = "days_per_week", nullable = false)
    private Integer daysPerWeek;

    @OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("weekNumber, dayNumber")
    private List<PlanDay> days = new ArrayList<>();

    public int totalSessions() {
        return durationWeeks * daysPerWeek;
    }
}
