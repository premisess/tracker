package com.fitness.tracker.controller;

import com.fitness.tracker.dto.ForgotPasswordRequest;
import com.fitness.tracker.dto.ResetPasswordRequest;
import com.fitness.tracker.exception.BadRequestException;
import com.fitness.tracker.service.PasswordResetService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class PasswordResetController {

    private final PasswordResetService passwordResetService;

    public PasswordResetController(PasswordResetService passwordResetService) {
        this.passwordResetService = passwordResetService;
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        passwordResetService.sendResetEmail(request.getEmail());
        return ResponseEntity.ok("If this email exists, a reset link has been sent.");
    }

    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        if (!passwordResetService.resetPassword(request.getToken(), request.getNewPassword())) {
            throw new BadRequestException("This reset link is invalid or has expired. Please request a new one.");
        }
        return ResponseEntity.ok("Password reset successfully!");
    }
}
