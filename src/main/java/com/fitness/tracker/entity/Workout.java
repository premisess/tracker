package com.fitness.tracker.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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

    // How the workout was recorded: typed in by hand, or tracked live with GPS.
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 10)
    private ActivitySource source = ActivitySource.MANUAL;

    // GPS activities only (UTC).
    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "distance_meters")
    private Integer distanceMeters;

    @Column(name = "moving_time_sec")
    private Integer movingTimeSec;

    @Column(name = "elevation_gain_m")
    private Integer elevationGainM;

    @Column(name = "avg_pace_sec_per_km")
    private Integer avgPaceSecPerKm;

    // Child collections are excluded from toString/equals/hashCode: each child points back here.
    @OneToMany(mappedBy = "workout", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<WorkoutExercise> exercises = new ArrayList<>();

    @OneToOne(mappedBy = "workout", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private WorkoutRoute route;

    @OneToMany(mappedBy = "workout", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("splitIndex")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<RunSplit> splits = new ArrayList<>();

    public enum ActivitySource {
        MANUAL, GPS
    }
}
