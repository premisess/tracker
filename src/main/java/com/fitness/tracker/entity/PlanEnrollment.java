package com.fitness.tracker.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** A user following a plan. Only one enrollment per user is ACTIVE at a time. */
@Entity
@Table(name = "plan_enrollments")
@Getter
@Setter
public class PlanEnrollment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_id", nullable = false)
    private WorkoutPlan plan;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 10)
    private Status status;

    @Column(name = "started_on", nullable = false)
    private LocalDate startedOn;

    // Sessions done or skipped so far; the next session is number completedSessions + 1.
    @Column(name = "completed_sessions", nullable = false)
    private Integer completedSessions = 0;

    // When the plan was completed or quit.
    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    public enum Status {
        ACTIVE, COMPLETED, CANCELLED
    }
}
