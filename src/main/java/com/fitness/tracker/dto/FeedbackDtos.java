package com.fitness.tracker.dto;

import com.fitness.tracker.entity.Feedback;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

public final class FeedbackDtos {

    private FeedbackDtos() {
    }

    @Data
    public static class FeedbackRequest {
        @NotNull(message = "Choose what you're sending")
        private Feedback.Kind kind;

        @Min(value = 1, message = "Rating is from 1 to 5")
        @Max(value = 5, message = "Rating is from 1 to 5")
        private Integer rating;

        @NotBlank(message = "Write your message")
        @Size(max = 2000, message = "Keep it under 2000 characters")
        private String message;
    }

    /** An admin marking progress and optionally replying. */
    @Data
    public static class FeedbackUpdate {
        @NotNull(message = "Choose a status")
        private Feedback.Status status;

        @Size(max = 1000, message = "Keep the reply under 1000 characters")
        private String reply;
    }

    public record FeedbackView(
            Long id,
            Feedback.Kind kind,
            Integer rating,
            String message,
            Feedback.Status status,
            String reply,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {
    }

    /** What admins see: the submission plus who sent it. */
    public record AdminFeedbackView(FeedbackView feedback, Long userId, String userName, String userEmail) {
    }

    public record AdminFeedbackList(long newCount, List<AdminFeedbackView> items) {
    }
}
