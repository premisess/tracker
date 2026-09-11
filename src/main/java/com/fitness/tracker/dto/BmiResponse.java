package com.fitness.tracker.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.time.LocalDate;

@Data
@AllArgsConstructor
public class BmiResponse {
    private Long id;
    private Double weight;
    private Double height;
    private Double bmiValue;
    private String category;
    private LocalDate date;
}