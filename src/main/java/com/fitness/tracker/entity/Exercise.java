package com.fitness.tracker.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "exercises")
@Getter
@Setter
public class Exercise {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // The catalog's own id (e.g. "Barbell_Squat"); also the folder name of its demo photos.
    @Column(nullable = false, unique = true, length = 100)
    private String slug;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, length = 40)
    private String category;

    @Column(length = 40)
    private String equipment;

    @Column(length = 20)
    private String level;

    // "force" is a reserved word in MySQL.
    @Column(name = "force_type", length = 20)
    private String forceType;

    @Column(length = 20)
    private String mechanic;

    // Comma-separated, e.g. "quadriceps,glutes".
    @Column(name = "primary_muscles")
    private String primaryMuscles;

    @Column(name = "secondary_muscles")
    private String secondaryMuscles;

    // One step per line.
    @Column(columnDefinition = "TEXT")
    private String instructions;

    // Paths relative to the image CDN base URL, e.g. "Barbell_Squat/0.jpg" (start) and "/1.jpg" (end).
    @Column(name = "image_start", length = 120)
    private String imageStart;

    @Column(name = "image_end", length = 120)
    private String imageEnd;

    // Stored as varchar, not a MySQL enum, so new values don't need a table rebuild.
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "tracking_type", nullable = false, length = 20)
    private TrackingType trackingType;

    /** Decides which inputs a logged set shows: weight + reps, reps only, or a timer. */
    public enum TrackingType {
        WEIGHT_REPS, REPS, DURATION
    }
}
