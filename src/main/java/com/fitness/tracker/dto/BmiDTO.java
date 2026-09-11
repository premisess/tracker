package com.fitness.tracker.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class BmiDTO {
    private Double weight;
    private Double height;
    private LocalDate date;
}