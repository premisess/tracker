package com.fitness.tracker.controller;

import com.fitness.tracker.exception.UnauthorizedException;
import com.fitness.tracker.entity.User;
import com.fitness.tracker.repository.UserRepository;
import com.fitness.tracker.security.SecurityUtil;
import com.fitness.tracker.service.StreakService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/streak")
public class StreakController {

    private final StreakService streakService;
    private final UserRepository userRepository;

    public StreakController(StreakService streakService, UserRepository userRepository) {
        this.streakService = streakService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public ResponseEntity<Map<String, Integer>> getStreak() {
        String email = SecurityUtil.getCurrentUserEmail();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException("Your session has expired. Please sign in again."));

        Map<String, Integer> result = new HashMap<>();
        result.put("currentStreak", streakService.getCurrentStreak(user.getId()));
        result.put("longestStreak", streakService.getLongestStreak(user.getId()));

        return ResponseEntity.ok(result);
    }
}