package com.fitness.tracker.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class GoogleSignInRequest {
    // The ID token (JWT) Google Identity Services hands the website after the user picks an account.
    @NotBlank(message = "Google sign-in failed. Please try again.")
    @Size(max = 4096, message = "Google sign-in failed. Please try again.")
    private String credential;
}
