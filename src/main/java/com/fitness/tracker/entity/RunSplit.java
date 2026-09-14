package com.fitness.tracker.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/** One kilometre of a GPS activity (the last split may be a partial kilometre). */
@Entity
@Table(name = "run_splits")
@Getter
@Setter
public class RunSplit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workout_id", nullable = false)
    private Workout workout;

    @Column(name = "split_index", nullable = false)
    private Integer splitIndex;

    @Column(name = "distance_m", nullable = false)
    private Integer distanceM;

    @Column(name = "duration_sec", nullable = false)
    private Integer durationSec;

    @Column(name = "pace_sec_per_km", nullable = false)
    private Integer paceSecPerKm;

    @Column(name = "elevation_gain_m", nullable = false)
    private Integer elevationGainM;
}
