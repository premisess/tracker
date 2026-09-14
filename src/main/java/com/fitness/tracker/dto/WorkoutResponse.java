package com.fitness.tracker.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import java.time.LocalDate;
import java.util.List;

@Data
public class WorkoutResponse {
    private Long id;
    private String type;
    private Integer duration;
    private Integer caloriesBurned;
    private LocalDate date;
    private String notes;
    private List<String> tags;
    private List<WorkoutExerciseResponse> exercises;
    private Double totalVolumeKg;

    // MANUAL or GPS. The fields below are only filled for GPS activities.
    private String source;
    private Integer distanceMeters;
    private Integer movingTimeSec;
    private Integer avgPaceSecPerKm;
    private Integer elevationGainM;

    // Only present right after creating or updating a workout.
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<PersonalRecordHit> newRecords;
}
