package com.fitness.tracker.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class RunSummaryResponse {
    private Long workoutId;
    private String type;
    private LocalDate date;
    // UTC
    private LocalDateTime startedAt;
    private Integer distanceMeters;
    private Integer movingTimeSec;
    private Integer avgPaceSecPerKm;
    private Integer elevationGainM;
    private Integer caloriesBurned;
    // Full route for the owner's own map; null once route data has been deleted.
    private String polyline;
}
