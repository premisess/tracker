package com.fitness.tracker.service;

import com.fitness.tracker.entity.User;
import com.fitness.tracker.repository.UserRepository;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class PasswordResetService {

    private final UserRepository userRepository;
    private final JavaMailSender mailSender;
    private final PasswordEncoder passwordEncoder;

    public PasswordResetService(UserRepository userRepository, JavaMailSender mailSender, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.mailSender = mailSender;
        this.passwordEncoder = passwordEncoder;
    }

    public void sendResetEmail(String email) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) return; // don't reveal if email exists

        User user = userOpt.get();
        String token = UUID.randomUUID().toString();
        user.setResetToken(token);
        user.setResetTokenExpiry(LocalDateTime.now().plusMinutes(30));
        userRepository.save(user);

        String resetLink = "http://localhost:5173/reset-password?token=" + token;

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("FitTracker - Password Reset Request");
        message.setText(
                "Hello " + user.getName() + ",\n\n" +
                        "You requested to reset your password.\n\n" +
                        "Click the link below to reset it (valid for 30 minutes):\n" +
                        resetLink + "\n\n" +
                        "If you did not request this, ignore this email.\n\n" +
                        "FitTracker Team"
        );

        mailSender.send(message);
    }

    public boolean resetPassword(String token, String newPassword) {
        Optional<User> userOpt = userRepository.findByResetToken(token);
        if (userOpt.isEmpty()) return false;

        User user = userOpt.get();

        if (user.getResetTokenExpiry().isBefore(LocalDateTime.now())) {
            return false; // token expired
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setResetToken(null);
        user.setResetTokenExpiry(null);
        userRepository.save(user);

        return true;
    }
}