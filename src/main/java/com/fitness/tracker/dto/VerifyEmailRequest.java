package com.fitness.tracker.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class VerifyEmailRequest {
    @NotBlank(message = "This verification link is incomplete. Open the link from your email again.")
    @Size(max = 200, message = "This verification link is invalid.")
    private String token;
}
