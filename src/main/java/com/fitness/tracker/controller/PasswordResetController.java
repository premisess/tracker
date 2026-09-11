package com.fitness.tracker.controller;

import com.fitness.tracker.service.PasswordResetService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class PasswordResetController {

    private final PasswordResetService passwordResetService;

    public PasswordResetController(PasswordResetService passwordResetService) {
        this.passwordResetService = passwordResetService;
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(@RequestBody Map<String, String> body) {
        passwordResetService.sendResetEmail(body.get("email"));
        return ResponseEntity.ok("If this email exists, a reset link has been sent.");
    }

    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@RequestBody Map<String, String> body) {
        boolean success = passwordResetService.resetPassword(body.get("token"), body.get("newPassword"));
        if (success) {
            return ResponseEntity.ok("Password reset successfully!");
        } else {
            return ResponseEntity.badRequest().body("Invalid or expired token.");
        }
    }
}