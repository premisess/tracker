package com.fitness.tracker.controller;

import com.fitness.tracker.dto.AuthResponse;
import com.fitness.tracker.dto.ChangeEmailRequest;
import com.fitness.tracker.dto.ChangePasswordRequest;
import com.fitness.tracker.entity.User;
import com.fitness.tracker.service.AccountService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/account")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PutMapping("/password")
    public ResponseEntity<Void> changePassword(@RequestBody ChangePasswordRequest request) {
        accountService.changePassword(request);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/email")
    public ResponseEntity<AuthResponse> changeEmail(@RequestBody ChangeEmailRequest request,
                                                      HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        User user = accountService.changeEmail(request, httpRequest, httpResponse);
        return ResponseEntity.ok(new AuthResponse(user.getName(), user.getEmail(), user.getRole().name()));
    }

    @PutMapping("/name")
    public ResponseEntity<Void> updateName(@RequestBody Map<String, String> body) {
        accountService.updateName(body.get("name"));
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteAccount(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        accountService.deleteAccount(httpRequest, httpResponse);
        return ResponseEntity.noContent().build();
    }
}
