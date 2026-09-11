package com.fitness.tracker.dto;

import lombok.Data;
import lombok.AllArgsConstructor;

@Data
@AllArgsConstructor
public class AuthResponse {
    private String name;
    private String email;
    private String role;
}