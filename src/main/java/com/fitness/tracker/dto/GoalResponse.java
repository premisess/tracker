package com.fitness.tracker.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.time.LocalDate;

@Data
@AllArgsConstructor
public class GoalResponse {
    private Long id;
    private String title;
    private String goalType;
    private Double targetValue;
    private Double currentProgress;
    private String unit;
    private LocalDate deadline;
    private String status;
    private Double progressPercent;
    private Boolean autoTrack;
}