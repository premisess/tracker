package com.fitness.tracker.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * Sends a plain-text email as "FitTracker". Uses Gmail over SMTP by default; when BREVO_API_KEY is set
 * it goes through Brevo's HTTPS API instead, for hosts that block SMTP (Render's free plan does).
 */
@Component
public class MailTransport {

    private static final String BREVO_URL = "https://api.brevo.com/v3/smtp/email";

    private final JavaMailSender mailSender;
    private final String from;
    private final String brevoApiKey;
    private final RestClient http = RestClient.create();

    public MailTransport(JavaMailSender mailSender,
                         @Value("${spring.mail.username}") String from,
                         @Value("${app.mail.brevo-api-key:}") String brevoApiKey) {
        this.mailSender = mailSender;
        this.from = from;
        this.brevoApiKey = brevoApiKey;
    }

    /** Throws if the email couldn't be handed over for delivery. */
    public void send(String to, String replyTo, String subject, String text) {
        if (brevoApiKey.isBlank()) {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("FitTracker <" + from + ">");
            message.setReplyTo(replyTo);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);
            mailSender.send(message);
            return;
        }
        http.post()
                .uri(BREVO_URL)
                .header("api-key", brevoApiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "sender", Map.of("name", "FitTracker", "email", from),
                        "to", List.of(Map.of("email", to)),
                        "replyTo", Map.of("email", replyTo),
                        "subject", subject,
                        "textContent", text))
                .retrieve()
                .toBodilessEntity();
    }
}
