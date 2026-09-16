package com.fitness.tracker.service;

import com.fitness.tracker.entity.User;
import com.fitness.tracker.exception.ApiException;
import com.fitness.tracker.exception.BadRequestException;
import com.fitness.tracker.repository.UserRepository;
import com.fitness.tracker.security.CurrentUserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Confirms that a user owns their email address. The link carries a random token; only its SHA-256
 * is stored, so a leaked database can't be used to verify anyone. Unverified accounts can still sign in.
 */
@Service
public class EmailVerificationService {

    private static final Duration LINK_LIFETIME = Duration.ofHours(24);
    private static final Duration RESEND_COOLDOWN = Duration.ofSeconds(60);

    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final CurrentUserService currentUserService;
    private final String frontendUrl;
    private final SecureRandom random = new SecureRandom();

    public EmailVerificationService(UserRepository userRepository, NotificationService notificationService,
                                    CurrentUserService currentUserService,
                                    @Value("${app.frontend-url}") String frontendUrl) {
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.currentUserService = currentUserService;
        this.frontendUrl = frontendUrl.endsWith("/") ? frontendUrl.substring(0, frontendUrl.length() - 1) : frontendUrl;
    }

    /** Issues a new link, replacing any earlier one, and emails it. {@code welcome} words it for a brand-new account. */
    @Transactional
    public void sendLink(User user, boolean welcome) {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        user.setEmailVerificationTokenHash(hash(token));
        user.setEmailVerificationExpiresAt(LocalDateTime.now().plus(LINK_LIFETIME));
        userRepository.save(user);

        notificationService.sendVerificationEmail(user, frontendUrl + "/verify-email?token=" + token, welcome);
    }

    @Transactional
    public void verify(String token) {
        User user = userRepository.findByEmailVerificationTokenHash(hash(token.trim()))
                .filter(u -> u.getEmailVerificationExpiresAt() != null
                        && u.getEmailVerificationExpiresAt().isAfter(LocalDateTime.now()))
                .orElseThrow(() -> new BadRequestException(
                        "This verification link is invalid or has expired. Sign in and request a new one."));
        user.setEmailVerified(true);
        user.setEmailVerificationTokenHash(null);
        user.setEmailVerificationExpiresAt(null);
        userRepository.save(user);
    }

    @Transactional
    public void resendForCurrentUser() {
        User user = currentUserService.get();
        if (user.isEmailVerified()) {
            throw new BadRequestException("Your email address is already verified.");
        }
        LocalDateTime expiresAt = user.getEmailVerificationExpiresAt();
        if (expiresAt != null && expiresAt.minus(LINK_LIFETIME).plus(RESEND_COOLDOWN).isAfter(LocalDateTime.now())) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS,
                    "We just sent you a link. Please wait a minute before asking for another.");
        }
        sendLink(user, false);
    }

    static String hash(String token) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is always available", e);
        }
    }
}
