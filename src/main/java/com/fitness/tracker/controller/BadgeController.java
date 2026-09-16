package com.fitness.tracker.controller;

import com.fitness.tracker.dto.BadgeDtos.Badge;
import com.fitness.tracker.dto.BadgeDtos.BadgeBoard;
import com.fitness.tracker.service.BadgeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/badges")
public class BadgeController {

    private final BadgeService badgeService;

    public BadgeController(BadgeService badgeService) {
        this.badgeService = badgeService;
    }

    @GetMapping
    public ResponseEntity<BadgeBoard> board() {
        return ResponseEntity.ok(badgeService.board());
    }

    /** Called by the website after the user does something; returns badges to celebrate (usually none). */
    @PostMapping("/check")
    public ResponseEntity<List<Badge>> check() {
        return ResponseEntity.ok(badgeService.collectNew());
    }
}
