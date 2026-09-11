package com.fitness.tracker.service;

import com.fitness.tracker.entity.Goal;
import com.fitness.tracker.entity.User;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {

    private final JavaMailSender mailSender;

    public NotificationService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
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

    public void sendGoalAchievedEmail(User user, Goal goal) {
        send(user.getEmail(), "Goal achieved: " + goal.getTitle(),
                "Hi " + user.getName() + ",\n\n" +
                        "Congratulations! You just hit your goal \"" + goal.getTitle() + "\".\n\n" +
                        "Keep up the momentum and set your next one.\n\n" +
                        "FitTracker Team");
    }

    // Best-effort: a notification failure should never break the request that triggered it.
    private void send(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
        } catch (Exception ignored) {
        }
    }
}
