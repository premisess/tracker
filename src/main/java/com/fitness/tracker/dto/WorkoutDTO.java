package com.fitness.tracker.dto;

import lombok.Data;
import java.time.LocalDate;
import java.util.List;

@Data
public class WorkoutDTO {
    private String type;
    private Integer duration;
    private LocalDate date;
    private String notes;
    private List<String> tags;
}