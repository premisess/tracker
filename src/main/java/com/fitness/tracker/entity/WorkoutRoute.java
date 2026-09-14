package com.fitness.tracker.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/** The recorded GPS route of an activity. Deleting it keeps the workout and its numbers. */
@Entity
@Table(name = "workout_routes")
@Getter
@Setter
public class WorkoutRoute {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workout_id", nullable = false, unique = true)
    private Workout workout;

    // Every fix as submitted: [[lat, lng, alt, epochMs, accuracy, segment], ...]. Kept so metrics can be recomputed.
    @Column(name = "points_json", nullable = false, columnDefinition = "LONGTEXT")
    private String pointsJson;

    // Encoded polyline of the filtered, simplified route; only ever shown to the owner.
    @Column(nullable = false, columnDefinition = "TEXT")
    private String polyline;

    // Same route with the privacy zone around start and finish removed; null if nothing is left.
    @Column(name = "share_polyline", columnDefinition = "TEXT")
    private String sharePolyline;

    @Column(name = "privacy_meters", nullable = false)
    private Integer privacyMeters;
}
