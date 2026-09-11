package com.fitness.tracker.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.time.LocalDate;

@Data
@AllArgsConstructor
public class WaterIntakeResponse {
    private Long id;
    private Integer amountMl;
    private LocalDate date;
}