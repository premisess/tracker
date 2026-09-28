package com.fitness.tracker.service;

import com.fitness.tracker.entity.User;
import com.fitness.tracker.exception.ApiException;
import com.fitness.tracker.exception.BadRequestException;
import com.fitness.tracker.repository.UserRepository;
import com.fitness.tracker.security.CurrentUserService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HexFormat;

/**
 * Confirms that a user owns their email address with a 6-digit code sent by email. Only a SHA-256 of
 * the code is stored, and after a few wrong tries a new code is needed, so it can't be guessed.
 * While email sending is set up, unconfirmed accounts must enter the code before using the app.
 */
@Service
public class EmailVerificationService {

    private static final Duration CODE_LIFETIME = Duration.ofMinutes(15);
    private static final Duration RESEND_COOLDOWN = Duration.ofSeconds(60);
    private static final int MAX_ATTEMPTS = 5;

    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final CurrentUserService currentUserService;
    private final MailTransport mailTransport;
    private final SecureRandom random = new SecureRandom();

    public EmailVerificationService(UserRepository userRepository, NotificationService notificationService,
                                    CurrentUserService currentUserService, MailTransport mailTransport) {
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.currentUserService = currentUserService;
        this.mailTransport = mailTransport;
    }

    /** Whether this user has to enter a code before using the app. Never, when the app can't send email. */
    public boolean isRequired(User user) {
        return mailTransport.isConfigured() && !user.isEmailVerified();
    }

    /** Issues a new code, replacing any earlier one, and emails it. {@code welcome} words it for a brand-new account. */
    @Transactional
    public void sendCode(User user, boolean welcome) {
        String code = String.format("%06d", random.nextInt(1_000_000));
        user.setEmailVerificationTokenHash(hash(user.getId() + ":" + code));
        user.setEmailVerificationExpiresAt(LocalDateTime.now().plus(CODE_LIFETIME));
        user.setEmailVerificationAttempts(0);
        userRepository.save(user);

        notificationService.sendVerificationCode(user, code, welcome);
    }

    @Transactional(noRollbackFor = BadRequestException.class)
    public void verifyCode(String code) {
        User user = currentUserService.get();
        if (user.isEmailVerified()) {
            return;
        }
        if (user.getEmailVerificationTokenHash() == null || user.getEmailVerificationExpiresAt() == null
                || user.getEmailVerificationExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("This code has expired. Tap \"Send a new code\" to get another.");
        }
        if (user.getEmailVerificationAttempts() >= MAX_ATTEMPTS) {
            throw new BadRequestException("Too many wrong tries. Tap \"Send a new code\" to get another.");
        }
        String typed = code == null ? "" : code.replaceAll("\\s", "");
        if (!MessageDigest.isEqual(hash(user.getId() + ":" + typed).getBytes(StandardCharsets.UTF_8),
                user.getEmailVerificationTokenHash().getBytes(StandardCharsets.UTF_8))) {
            user.setEmailVerificationAttempts(user.getEmailVerificationAttempts() + 1);
            userRepository.save(user);
            int left = MAX_ATTEMPTS - user.getEmailVerificationAttempts();
            throw new BadRequestException(left > 0
                    ? "That code isn't right. " + left + (left == 1 ? " try" : " tries") + " left."
                    : "Too many wrong tries. Tap \"Send a new code\" to get another.");
        }
        user.setEmailVerified(true);
        user.setEmailVerificationTokenHash(null);
        user.setEmailVerificationExpiresAt(null);
        user.setEmailVerificationAttempts(0);
        userRepository.save(user);
    }

    @Transactional
    public void resendForCurrentUser() {
        User user = currentUserService.get();
        if (user.isEmailVerified()) {
            throw new BadRequestException("Your email address is already confirmed.");
        }
        LocalDateTime expiresAt = user.getEmailVerificationExpiresAt();
        if (expiresAt != null && expiresAt.minus(CODE_LIFETIME).plus(RESEND_COOLDOWN).isAfter(LocalDateTime.now())) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS,
                    "We just sent you a code. Please wait a minute before asking for another.");
        }
        sendCode(user, false);
    }

    static String hash(String value) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is always available", e);
        }
    }
}
