package com.fitness.tracker.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * Sends a plain-text email as "FitTracker", by the first of these that is configured:
 * <ol>
 *   <li>Brevo's HTTPS API (BREVO_API_KEY),</li>
 *   <li>a Google Apps Script web app on the Gmail account (MAIL_SCRIPT_URL, see deploy/gmail-sender.gs),</li>
 *   <li>Gmail over SMTP (MAIL_PASSWORD).</li>
 * </ol>
 * The first two work on hosts that block SMTP, such as Render's free plan. With none configured,
 * emails are skipped and logged.
 */
@Component
public class MailTransport {

    private static final Logger log = LoggerFactory.getLogger(MailTransport.class);
    private static final String BREVO_URL = "https://api.brevo.com/v3/smtp/email";

    private final JavaMailSender mailSender;
    private final String from;
    private final String smtpPassword;
    private final String brevoApiKey;
    private final String scriptUrl;
    private final String scriptSecret;
    private final RestClient http = RestClient.create();

    public MailTransport(JavaMailSender mailSender,
                         @Value("${spring.mail.username}") String from,
                         @Value("${spring.mail.password:}") String smtpPassword,
                         @Value("${app.mail.brevo-api-key:}") String brevoApiKey,
                         @Value("${app.mail.script-url:}") String scriptUrl,
                         @Value("${app.mail.script-secret:}") String scriptSecret) {
        this.mailSender = mailSender;
        this.from = from;
        this.smtpPassword = smtpPassword;
        this.brevoApiKey = brevoApiKey;
        this.scriptUrl = scriptUrl;
        this.scriptSecret = scriptSecret;
    }

    /** Throws if the email couldn't be handed over for delivery. */
    public void send(String to, String replyTo, String subject, String text) {
        if (!brevoApiKey.isBlank()) {
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
            return;
        }
        if (!scriptUrl.isBlank()) {
            // Apps Script runs the send on the POST, then answers with a redirect to its output,
            // so a 3xx here means it ran. A wrong secret is visible in the script's executions log.
            http.post()
                    .uri(scriptUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("secret", scriptSecret, "to", to, "replyTo", replyTo, "subject", subject, "text", text))
                    .retrieve()
                    .toBodilessEntity();
            return;
        }
        if (!smtpPassword.isBlank()) {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("FitTracker <" + from + ">");
            message.setReplyTo(replyTo);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);
            mailSender.send(message);
            return;
        }
        log.warn("Email is not set up (no BREVO_API_KEY, MAIL_SCRIPT_URL or MAIL_PASSWORD); skipped \"{}\" to {}", subject, to);
    }
}
