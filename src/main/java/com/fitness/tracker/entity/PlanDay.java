package com.fitness.tracker.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.List;

/**
 * One session of a plan. With no week number it repeats every week; with one it applies only to that
 * week, which lets progressive plans (like a first 5K) change the session week by week.
 */
@Entity
@Table(name = "plan_days")
@Getter
@Setter
public class PlanDay {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_id", nullable = false)
    private WorkoutPlan plan;

    @Column(name = "week_number")
    private Integer weekNumber;

    @Column(name = "day_number", nullable = false)
    private Integer dayNumber;

    @Column(nullable = false, length = 80)
    private String title;

    @Column(length = 160)
    private String focus;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 10)
    private Activity activity;

    // A WorkoutType label, e.g. "Weightlifting" or "Running".
    @Column(name = "workout_type", nullable = false, length = 30)
    private String workoutType;

    @Column(name = "target_minutes", nullable = false)
    private Integer targetMinutes;

    @Column(name = "target_distance_m")
    private Integer targetDistanceM;

    @Column(length = 500)
    private String instructions;

    @OneToMany(mappedBy = "planDay", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder")
    private List<PlanDayExercise> exercises = new ArrayList<>();

    /** STRENGTH sessions are logged with sets; RUN sessions are recorded with GPS (or logged by hand). */
    public enum Activity {
        STRENGTH, RUN
    }
}
