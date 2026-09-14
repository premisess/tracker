package com.fitness.tracker.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateNameRequest {
    @NotBlank(message = "Name can't be empty")
    @Size(max = 100, message = "Name must be at most 100 characters")
    private String name;
}
