package com.fitness.tracker.dto;

import lombok.Data;

@Data
public class ChangeEmailRequest {
    private String newEmail;
    private String currentPassword;
}
