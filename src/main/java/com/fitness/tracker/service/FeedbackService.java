package com.fitness.tracker.service;

import com.fitness.tracker.dto.FeedbackDtos.AdminFeedbackList;
import com.fitness.tracker.dto.FeedbackDtos.AdminFeedbackView;
import com.fitness.tracker.dto.FeedbackDtos.FeedbackRequest;
import com.fitness.tracker.dto.FeedbackDtos.FeedbackUpdate;
import com.fitness.tracker.dto.FeedbackDtos.FeedbackView;
import com.fitness.tracker.entity.Feedback;
import com.fitness.tracker.entity.User;
import com.fitness.tracker.exception.BadRequestException;
import com.fitness.tracker.exception.NotFoundException;
import com.fitness.tracker.repository.FeedbackRepository;
import com.fitness.tracker.security.CurrentUserService;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/** Feedback, recommendations and problem reports from users, and the team's replies. */
@Service
public class FeedbackService {

    // Enough for real use; stops one account flooding the inbox.
    private static final int MAX_PER_DAY = 10;
    private static final int ADMIN_LIST_LIMIT = 200;

    private final FeedbackRepository feedbackRepository;
    private final CurrentUserService currentUserService;
    private final NotificationService notificationService;

    public FeedbackService(FeedbackRepository feedbackRepository, CurrentUserService currentUserService,
                           NotificationService notificationService) {
        this.feedbackRepository = feedbackRepository;
        this.currentUserService = currentUserService;
        this.notificationService = notificationService;
    }

    @Transactional
    public FeedbackView submit(FeedbackRequest request) {
        User user = currentUserService.get();
        if (feedbackRepository.countByUserIdAndCreatedAtAfter(user.getId(), LocalDateTime.now().minusDays(1)) >= MAX_PER_DAY) {
            throw new BadRequestException("You've sent " + MAX_PER_DAY + " messages today. Thanks! Try again tomorrow.");
        }
        Feedback f = new Feedback();
        f.setUser(user);
        f.setKind(request.getKind());
        f.setRating(request.getRating());
        f.setMessage(request.getMessage().trim());
        f.setStatus(Feedback.Status.NEW);
        LocalDateTime now = LocalDateTime.now();
        f.setCreatedAt(now);
        f.setUpdatedAt(now);
        Feedback saved = feedbackRepository.save(f);
        notificationService.sendFeedbackNotice(user.getName(), user.getEmail(), label(saved.getKind()), saved.getRating(), saved.getMessage());
        return view(saved);
    }

    @Transactional(readOnly = true)
    public List<FeedbackView> mine() {
        User user = currentUserService.get();
        return feedbackRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream().map(FeedbackService::view).toList();
    }

    @Transactional(readOnly = true)
    public AdminFeedbackList all() {
        List<AdminFeedbackView> items = feedbackRepository.findAllWithUser(PageRequest.of(0, ADMIN_LIST_LIMIT)).stream()
                .map(f -> new AdminFeedbackView(view(f), f.getUser().getId(), f.getUser().getName(), f.getUser().getEmail()))
                .toList();
        return new AdminFeedbackList(feedbackRepository.countByStatus(Feedback.Status.NEW), items);
    }

    @Transactional
    public FeedbackView update(Long id, FeedbackUpdate update) {
        Feedback f = feedbackRepository.findById(id).orElseThrow(() -> new NotFoundException("Feedback not found"));
        f.setStatus(update.getStatus());
        String reply = update.getReply();
        f.setReply(reply == null || reply.isBlank() ? null : reply.trim());
        f.setUpdatedAt(LocalDateTime.now());
        return view(feedbackRepository.save(f));
    }

    private static String label(Feedback.Kind kind) {
        return switch (kind) {
            case FEEDBACK -> "Feedback";
            case RECOMMENDATION -> "Recommendation";
            case PROBLEM -> "Problem report";
        };
    }

    private static FeedbackView view(Feedback f) {
        return new FeedbackView(f.getId(), f.getKind(), f.getRating(), f.getMessage(), f.getStatus(), f.getReply(),
                f.getCreatedAt(), f.getUpdatedAt());
    }
}
