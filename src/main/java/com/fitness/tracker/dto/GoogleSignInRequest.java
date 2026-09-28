package com.fitness.tracker.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** One of the two: an ID token from Google's own button, or an access token from Google's sign-in popup. */
@Data
public class GoogleSignInRequest {
    // The ID token (JWT) Google Identity Services hands the website after the user picks an account.
    @Size(max = 4096, message = "Google sign-in failed. Please try again.")
    private String credential;

    // An access token from Google's sign-in popup, opened by the website's own button.
    @Size(max = 4096, message = "Google sign-in failed. Please try again.")
    private String accessToken;

    @AssertTrue(message = "Google sign-in failed. Please try again.")
    public boolean isComplete() {
        return (credential != null && !credential.isBlank()) || (accessToken != null && !accessToken.isBlank());
    }
}
