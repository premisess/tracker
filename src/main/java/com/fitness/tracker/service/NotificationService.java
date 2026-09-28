package com.fitness.tracker.service;

import com.fitness.tracker.entity.Goal;
import com.fitness.tracker.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final MailTransport mail;
    private final String replyTo;

    public NotificationService(MailTransport mail, @Value("${app.contact-email}") String replyTo) {
        this.mail = mail;
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

    public void sendVerificationCode(User user, String code, boolean welcome) {
        String subject = code + " is your FitTracker confirmation code";
        String intro = welcome
                ? "Welcome to FitTracker! To finish creating your account, enter the code below.\n\n"
                : "Enter the code below in FitTracker to confirm your email address.\n\n";
        send(user.getEmail(), subject,
                "Hi " + user.getName() + ",\n\n" +
                        intro + "    " + code + "\n\n" +
                        "The code works for 15 minutes. If you didn't create a FitTracker account, you can ignore this email.\n\n" +
                        "FitTracker Team");
    }

    public void sendGoalAchievedEmail(User user, Goal goal) {
        send(user.getEmail(), "Goal achieved: " + goal.getTitle(),
                "Hi " + user.getName() + ",\n\n" +
                        "Congratulations! You just hit your goal \"" + goal.getTitle() + "\".\n\n" +
                        "Keep up the momentum and set your next one.\n\n" +
                        "FitTracker Team");
    }

    /** Tells the team about new feedback. Replying to this email goes straight to the user. */
    public void sendFeedbackNotice(String userName, String userEmail, String kind, Integer rating, String message) {
        try {
            mail.send(replyTo, userEmail, "New " + kind.toLowerCase() + " from " + userName,
                    userName + " (" + userEmail + ") sent " + kind.toLowerCase()
                            + (rating != null ? " with a " + rating + " star rating" : "") + ".\n\n"
                            + message + "\n\n"
                            + "Answer it in the admin dashboard, or reply to this email to write to them directly.");
        } catch (Exception e) {
            log.warn("Could not send feedback notice: {}", e.getMessage());
        }
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
        mail.send(user.getEmail(), replyTo, "FitTracker password reset", withFooter(
                "Hi " + user.getName() + ",\n\n" +
                        "You asked to reset your password. Open this link to choose a new one. It works for 30 minutes.\n" +
                        link + "\n\n" +
                        "If you didn't ask for this, you can ignore this email and your password stays the same.\n\n" +
                        "FitTracker Team"));
    }

    // Replies from users land in the FitTracker inbox.
    private static String withFooter(String body) {
        return body + "\n\nQuestions? Just reply to this email.";
    }

    // Best-effort: a notification failure should never break the request that triggered it.
    private void send(String to, String subject, String body) {
        try {
            mail.send(to, replyTo, subject, withFooter(body));
        } catch (Exception e) {
            log.warn("Could not send \"{}\" email: {}", subject, e.getMessage());
        }
    }
}
