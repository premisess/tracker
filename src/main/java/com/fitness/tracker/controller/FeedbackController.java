package com.fitness.tracker.controller;

import com.fitness.tracker.dto.FeedbackDtos.AdminFeedbackList;
import com.fitness.tracker.dto.FeedbackDtos.FeedbackRequest;
import com.fitness.tracker.dto.FeedbackDtos.FeedbackUpdate;
import com.fitness.tracker.dto.FeedbackDtos.FeedbackView;
import com.fitness.tracker.service.FeedbackService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Users send and read their own feedback; /api/admin/** (admins only, see SecurityConfig) sees and answers all of it. */
@RestController
public class FeedbackController {

    private final FeedbackService feedbackService;

    public FeedbackController(FeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    @PostMapping("/api/feedback")
    public ResponseEntity<FeedbackView> submit(@Valid @RequestBody FeedbackRequest request) {
        return ResponseEntity.ok(feedbackService.submit(request));
    }

    @GetMapping("/api/feedback")
    public ResponseEntity<List<FeedbackView>> mine() {
        return ResponseEntity.ok(feedbackService.mine());
    }

    @GetMapping("/api/admin/feedback")
    public ResponseEntity<AdminFeedbackList> all() {
        return ResponseEntity.ok(feedbackService.all());
    }

    @PatchMapping("/api/admin/feedback/{id}")
    public ResponseEntity<FeedbackView> update(@PathVariable Long id, @Valid @RequestBody FeedbackUpdate update) {
        return ResponseEntity.ok(feedbackService.update(id, update));
    }
}
