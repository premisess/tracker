package com.fitness.tracker.dto;

import com.fitness.tracker.entity.Goal;
import lombok.Data;
import java.time.LocalDate;

@Data
public class GoalDTO {
    private String title;
    private Goal.GoalType goalType;
    private Double targetValue;
    private Double currentProgress;
    private String unit;
    private LocalDate deadline;
    private Boolean autoTrack;
}