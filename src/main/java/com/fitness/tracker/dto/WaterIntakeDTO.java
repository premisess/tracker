package com.fitness.tracker.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class WaterIntakeDTO {
    private Integer amountMl;
    private LocalDate date;
}