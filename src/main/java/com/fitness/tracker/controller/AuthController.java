package com.fitness.tracker.controller;

import jakarta.validation.Valid;
import com.fitness.tracker.dto.*;
import com.fitness.tracker.service.AuthService;
import com.fitness.tracker.service.EmailVerificationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final EmailVerificationService emailVerificationService;

    public AuthController(AuthService authService, EmailVerificationService emailVerificationService) {
        this.authService = authService;
        this.emailVerificationService = emailVerificationService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request,
                                                   HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        return ResponseEntity.ok(authService.register(request, httpRequest, httpResponse));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request,
                                                HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        return ResponseEntity.ok(authService.login(request, httpRequest, httpResponse));
    }

    @GetMapping("/providers")
    public ResponseEntity<AuthProvidersResponse> providers() {
        return ResponseEntity.ok(authService.providers());
    }

    @PostMapping("/google")
    public ResponseEntity<AuthResponse> google(@Valid @RequestBody GoogleSignInRequest request,
                                                 HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        return ResponseEntity.ok(authService.signInWithGoogle(request.getCredential(), httpRequest, httpResponse));
    }

    @PostMapping("/verify-email")
    public ResponseEntity<Map<String, String>> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        emailVerificationService.verify(request.getToken());
        return ResponseEntity.ok(Map.of("message", "Your email address is verified."));
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<Map<String, String>> resendVerification() {
        emailVerificationService.resendForCurrentUser();
        return ResponseEntity.ok(Map.of("message", "We've sent a new verification link to your email."));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        authService.logout(httpRequest, httpResponse);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ResponseEntity<AuthResponse> me() {
        return ResponseEntity.ok(authService.currentUser());
    }
}
