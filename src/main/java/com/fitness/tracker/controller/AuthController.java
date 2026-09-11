package com.fitness.tracker.controller;

import com.fitness.tracker.dto.*;
import com.fitness.tracker.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest request,
                                                   HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        return ResponseEntity.ok(authService.register(request, httpRequest, httpResponse));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request,
                                                HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        return ResponseEntity.ok(authService.login(request, httpRequest, httpResponse));
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
