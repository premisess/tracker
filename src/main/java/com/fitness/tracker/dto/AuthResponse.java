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

    public static AuthResponse of(User user) {
        return new AuthResponse(user.getName(), user.getEmail(), user.getRole().name(),
                user.isEmailVerified(), user.getAuthProvider().name());
    }
}
