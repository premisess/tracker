package com.fitness.tracker.dto;

import com.fitness.tracker.entity.Message;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/** Friends, friend requests and messages. People are shown by name and photo only, never their email. */
public final class SocialDtos {

    private SocialDtos() {
    }

    /** picture is the profile photo's file name, served from /api/profile/picture/{picture}; null when none. */
    public record Person(Long id, String name, String picture) {
    }

    public record FriendView(Person person, LocalDateTime since) {
    }

    public record RequestView(Long id, Person person, LocalDateTime sentAt) {
    }

    public record FriendsOverview(List<FriendView> friends, List<RequestView> incoming, List<RequestView> outgoing) {
    }

    /** relation is NONE, FRIEND, REQUESTED (you asked them) or INCOMING (they asked you). */
    public record SearchResult(Person person, String relation) {
    }

    public record Suggestion(Person person, int mutualFriends) {
    }

    public record Alerts(long unreadMessages, long pendingRequests) {
    }

    public record InviteResult(String message) {
    }

    public record MessageView(Long id, boolean fromMe, Message.Kind kind, String body, LocalDateTime createdAt, boolean read) {
    }

    public record Conversation(Person person, MessageView last, long unread) {
    }

    @Data
    public static class FriendRequest {
        @NotNull(message = "Choose who to add")
        private Long userId;
    }

    @Data
    public static class InviteRequest {
        @NotBlank(message = "Enter an email address")
        @Email(message = "Enter a valid email address")
        private String email;
    }

    @Data
    public static class SendMessageRequest {
        @NotNull(message = "Choose a message type")
        private Message.Kind kind;

        @NotBlank(message = "Write a message")
        @Size(max = 1000, message = "Keep messages under 1000 characters")
        private String body;
    }
}
