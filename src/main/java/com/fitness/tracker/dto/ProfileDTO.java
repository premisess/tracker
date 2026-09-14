package com.fitness.tracker.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

// Every field is optional so a profile can be filled in gradually, but what's sent must be plausible.
@Data
public class ProfileDTO {
    @Min(value = 10, message = "Age must be between 10 and 120")
    @Max(value = 120, message = "Age must be between 10 and 120")
    private Integer age;

    @Size(max = 20, message = "Gender must be at most 20 characters")
    private String gender;

    @DecimalMin(value = "20", message = "Weight must be between 20 and 500 kg")
    @DecimalMax(value = "500", message = "Weight must be between 20 and 500 kg")
    private Double weight;

    @DecimalMin(value = "50", message = "Height must be between 50 and 272 cm")
    @DecimalMax(value = "272", message = "Height must be between 50 and 272 cm")
    private Double height;

    @Size(max = 255, message = "Picture name is too long")
    private String profilePic;
}
