package com.fitness.tracker.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.time.LocalDate;
import java.util.List;

@Data
@AllArgsConstructor
public class WorkoutResponse {
    private Long id;
    private String type;
    private Integer duration;
    private Integer caloriesBurned;
    private LocalDate date;
    private String notes;
    private List<String> tags;
}
