package com.fitness.tracker.dto;

import com.fitness.tracker.entity.User;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AuthResponse {
    private String name;
    private String email;
    private String role;
    private boolean emailVerified;
    private String authProvider;
    // True while the user must enter the emailed code before using the app.
    private boolean verificationRequired;

    public static AuthResponse of(User user, boolean verificationRequired) {
        return new AuthResponse(user.getName(), user.getEmail(), user.getRole().name(),
                user.isEmailVerified(), user.getAuthProvider().name(), verificationRequired);
    }
}
