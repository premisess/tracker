package com.fitness.tracker.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDate;

@Data
public class WaterIntakeDTO {
    @NotNull(message = "Amount is required")
    @Min(value = 1, message = "Amount must be at least 1 ml")
    @Max(value = 10000, message = "That's more than 10 litres in one entry")
    private Integer amountMl;

    @NotNull(message = "Date is required")
    private LocalDate date;
}
