package com.fitness.tracker.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

@Entity
@Table(name = "workouts")
@Data
public class Workout {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String type;

    private Integer duration;

    private Integer caloriesBurned;

    private LocalDate date;

    private String notes;

    // Stored as a comma-separated list, e.g. "morning,legs,pr".
    private String tags;
}