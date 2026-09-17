package com.fitness.tracker.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * A training program: a number of weeks, each with the same count of sessions. Built-in plans have no
 * owner; a plan with an owner was built by that user and only they can see it.
 */
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private User owner;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("weekNumber, dayNumber")
    private List<PlanDay> days = new ArrayList<>();

    public int totalSessions() {
        return durationWeeks * daysPerWeek;
    }

    public boolean isVisibleTo(User user) {
        return owner == null || owner.getId().equals(user.getId());
    }
}
