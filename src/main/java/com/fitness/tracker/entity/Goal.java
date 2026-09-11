package com.fitness.tracker.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

@Entity
@Table(name = "goals")
@Data
public class Goal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String title;

    @Enumerated(EnumType.STRING)
    private GoalType goalType;

    private Double targetValue;
    private Double currentProgress;
    private String unit;
    private LocalDate deadline;
    private Boolean autoTrack = true;

    @Enumerated(EnumType.STRING)
    private Status status = Status.IN_PROGRESS;

    public enum GoalType {
        LOSE_WEIGHT,
        GAIN_WEIGHT,
        BUILD_STRENGTH,
        RUN_MORE,
        STAY_ACTIVE
    }

    public enum Status {
        IN_PROGRESS, COMPLETED, FAILED
    }
}