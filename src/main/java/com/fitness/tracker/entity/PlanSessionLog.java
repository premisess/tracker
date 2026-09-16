package com.fitness.tracker.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** Records that a plan session was done (linked to the workout that did it) or skipped. */
@Entity
@Table(name = "plan_session_logs")
@Getter
@Setter
public class PlanSessionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "enrollment_id", nullable = false)
    private PlanEnrollment enrollment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_day_id", nullable = false)
    private PlanDay planDay;

    @Column(name = "session_number", nullable = false)
    private Integer sessionNumber;

    // Null when skipped, or when the workout was deleted afterwards.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workout_id")
    private Workout workout;

    @Column(nullable = false)
    private boolean skipped;

    @Column(name = "logged_at", nullable = false)
    private LocalDateTime loggedAt;
}
