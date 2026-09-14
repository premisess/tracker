package com.fitness.tracker.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChangeEmailRequest {
    @NotBlank(message = "New email is required")
    @Email(message = "Enter a valid email address")
    @Size(max = 255, message = "Email is too long")
    private String newEmail;

    @NotBlank(message = "Current password is required")
    private String currentPassword;
}
