package com.fitness.tracker.controller;

import com.fitness.tracker.dto.SocialDtos.Alerts;
import com.fitness.tracker.dto.SocialDtos.Conversation;
import com.fitness.tracker.dto.SocialDtos.FriendRequest;
import com.fitness.tracker.dto.SocialDtos.FriendsOverview;
import com.fitness.tracker.dto.SocialDtos.InviteRequest;
import com.fitness.tracker.dto.SocialDtos.InviteResult;
import com.fitness.tracker.dto.SocialDtos.MessageView;
import com.fitness.tracker.dto.SocialDtos.SearchResult;
import com.fitness.tracker.dto.SocialDtos.SendMessageRequest;
import com.fitness.tracker.dto.SocialDtos.Suggestion;
import com.fitness.tracker.service.FriendService;
import com.fitness.tracker.service.MessageService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/friends")
public class FriendController {

    private final FriendService friendService;
    private final MessageService messageService;

    public FriendController(FriendService friendService, MessageService messageService) {
        this.friendService = friendService;
        this.messageService = messageService;
    }

    @GetMapping
    public ResponseEntity<FriendsOverview> overview() {
        return ResponseEntity.ok(friendService.overview());
    }

    @GetMapping("/search")
    public ResponseEntity<List<SearchResult>> search(@RequestParam String q) {
        return ResponseEntity.ok(friendService.search(q));
    }

    @GetMapping("/suggestions")
    public ResponseEntity<List<Suggestion>> suggestions() {
        return ResponseEntity.ok(friendService.suggestions());
    }

    @PostMapping("/requests")
    public ResponseEntity<Void> request(@Valid @RequestBody FriendRequest request) {
        friendService.request(request.getUserId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/requests/{id}/accept")
    public ResponseEntity<Void> accept(@PathVariable Long id) {
        friendService.acceptRequest(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/requests/{id}")
    public ResponseEntity<Void> dismiss(@PathVariable Long id) {
        friendService.dismissRequest(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> unfriend(@PathVariable Long userId) {
        friendService.unfriend(userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/invite")
    public ResponseEntity<InviteResult> invite(@Valid @RequestBody InviteRequest request) {
        return ResponseEntity.ok(friendService.invite(request.getEmail()));
    }

    @GetMapping("/alerts")
    public ResponseEntity<Alerts> alerts() {
        return ResponseEntity.ok(messageService.alerts());
    }

    @GetMapping("/messages")
    public ResponseEntity<List<Conversation>> conversations() {
        return ResponseEntity.ok(messageService.conversations());
    }

    @GetMapping("/{userId}/messages")
    public ResponseEntity<List<MessageView>> thread(@PathVariable Long userId) {
        return ResponseEntity.ok(messageService.thread(userId));
    }

    @PostMapping("/{userId}/messages")
    public ResponseEntity<MessageView> send(@PathVariable Long userId, @Valid @RequestBody SendMessageRequest request) {
        return ResponseEntity.ok(messageService.send(userId, request));
    }
}
