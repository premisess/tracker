package com.fitness.tracker.service;

import com.fitness.tracker.entity.Goal;
import com.fitness.tracker.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final JavaMailSender mailSender;
    private final String from;
    private final String replyTo;

    public NotificationService(JavaMailSender mailSender,
                               @Value("${spring.mail.username}") String from,
                               @Value("${app.contact-email}") String replyTo) {
        this.mailSender = mailSender;
        this.from = from;
        this.replyTo = replyTo;
    }

    public void sendWelcomeEmail(User user) {
        send(user.getEmail(), "Welcome to FitTracker!",
                "Hi " + user.getName() + ",\n\n" +
                        "Welcome to FitTracker! Your account is ready.\n\n" +
                        "Next steps:\n" +
                        "1. Complete your profile\n" +
                        "2. Set a goal\n" +
                        "3. Log your first workout\n\n" +
                        "FitTracker Team");
    }

    public void sendVerificationEmail(User user, String link, boolean welcome) {
        String subject = welcome ? "Welcome to FitTracker - please confirm your email" : "FitTracker - confirm your email";
        String intro = welcome
                ? "Welcome to FitTracker! Your account is ready.\n\nPlease confirm your email address so we can keep your account secure:\n"
                : "Please confirm your email address by opening this link:\n";
        send(user.getEmail(), subject,
                "Hi " + user.getName() + ",\n\n" +
                        intro + link + "\n\n" +
                        "The link works for 24 hours. If you didn't create a FitTracker account, you can ignore this email.\n\n" +
                        "FitTracker Team");
    }

    public void sendGoalAchievedEmail(User user, Goal goal) {
        send(user.getEmail(), "Goal achieved: " + goal.getTitle(),
                "Hi " + user.getName() + ",\n\n" +
                        "Congratulations! You just hit your goal \"" + goal.getTitle() + "\".\n\n" +
                        "Keep up the momentum and set your next one.\n\n" +
                        "FitTracker Team");
    }

    public void sendFriendInvite(String inviterName, String email, String signUpLink) {
        send(email, inviterName + " invited you to FitTracker",
                "Hi,\n\n" +
                        inviterName + " wants to train with you on FitTracker, a free app for workouts, runs, meals, water and sleep.\n\n" +
                        "Create your free account with the link below, then add them as a friend.\n" +
                        signUpLink + "\n\n" +
                        "FitTracker Team");
    }

    /** Unlike the others, a failed reset email is reported to the caller instead of being logged and skipped. */
    public void sendPasswordResetEmail(User user, String link) {
        mailSender.send(message(user.getEmail(), "FitTracker password reset",
                "Hi " + user.getName() + ",\n\n" +
                        "You asked to reset your password. Open this link to choose a new one. It works for 30 minutes.\n" +
                        link + "\n\n" +
                        "If you didn't ask for this, you can ignore this email and your password stays the same.\n\n" +
                        "FitTracker Team"));
    }

    private SimpleMailMessage message(String to, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("FitTracker <" + from + ">");
        // Replies from users land in the FitTracker inbox.
        message.setReplyTo(replyTo);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body + "\n\nQuestions? Just reply to this email.");
        return message;
    }

    // Best-effort: a notification failure should never break the request that triggered it.
    private void send(String to, String subject, String body) {
        try {
            mailSender.send(message(to, subject, body));
        } catch (Exception e) {
            log.warn("Could not send \"{}\" email: {}", subject, e.getMessage());
        }
    }
}
