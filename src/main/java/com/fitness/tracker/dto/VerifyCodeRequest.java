package com.fitness.tracker.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class VerifyCodeRequest {
    @NotBlank(message = "Enter the 6-digit code from your email")
    @Pattern(regexp = "\\s*\\d{3}\\s*\\d{3}\\s*", message = "The code is 6 digits")
    private String code;
}
