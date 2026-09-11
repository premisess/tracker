package com.fitness.tracker.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.time.LocalDate;

@Data
@AllArgsConstructor
public class DailyPoint {
    private LocalDate date;
    private double value;
}
