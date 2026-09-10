package com.example.wms.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Sends transactional email via Brevo's HTTPS REST API instead of raw SMTP.
 *
 * Why: most free-tier hosts (including Render's free plan) block outbound
 * SMTP ports (25/465/587) to cut down on spam abuse, so talking to
 * smtp.gmail.com on port 587 will always time out there no matter what
 * credentials are used. Brevo's API runs over plain HTTPS on port 443,
 * which is never blocked, so this keeps real email working on the same
 * free hosting plan.
 *
 * One-time setup (done in Brevo's dashboard, not in code):
 *   1. Sign up free at https://www.brevo.com (300 emails/day free, no card).
 *   2. Settings -> SMTP & API -> API Keys -> generate a new key.
 *   3. Settings -> Senders & IP -> add the address you want to send FROM
 *      (e.g. your Gmail address) and click the confirmation link Brevo
 *      emails you - this only has to be done once per sender address.
 *   4. Set two environment variables wherever the app runs (Render's
 *      Environment tab, or locally before `mvn spring-boot:run`):
 *        BREVO_API_KEY=<the key from step 2>
 *        MAIL_FROM=<the verified address from step 3>
 *
 * Both public methods stay @Async for the same reason as before: email is
 * a side effect of the action the caller cares about (registering,
 * resetting a password), not the outcome itself, so it shouldn't block the
 * response thread. The real send logic lives in the private doSend() so
 * both public entry points go through Spring's async proxy.
 */
@Service
public class MailService {

    private static final Logger log = LoggerFactory.getLogger(MailService.class);
    private static final String BREVO_ENDPOINT = "https://api.brevo.com/v3/smtp/email";

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    private final ObjectMapper json = new ObjectMapper();

    @Value("${brevo.api-key:}")
    private String apiKey;

    @Value("${mail.from:}")
    private String fromAddress;

    @Async
    public void sendPasswordResetEmail(String toEmail, String resetLink) {
        doSend(
                toEmail,
                "Reset your WMS password",
                "Click the link below to reset your password. It's valid for 30 minutes.\n\n"
                        + resetLink
                        + "\n\nIf you didn't request this, you can safely ignore this email."
        );
    }

    /**
     * General-purpose notification email — used for account-created and
     * login alerts (see NotificationService) as well as the password reset
     * above. Kept generic on purpose: MailService only knows HOW to send
     * mail; NotificationService decides WHAT to send and WHO to send it to.
     */
    @Async
    public void sendNotification(String toEmail, String subject, String body) {
        doSend(toEmail, subject, body);
    }

    private void doSend(String toEmail, String subject, String body) {
        if (apiKey == null || apiKey.isBlank() || fromAddress == null || fromAddress.isBlank()) {
            log.error("Email not sent to {}: BREVO_API_KEY or MAIL_FROM is not set", toEmail);
            return;
        }

        try {
            Map<String, Object> sender = new LinkedHashMap<>();
            sender.put("email", fromAddress);

            Map<String, Object> recipient = new LinkedHashMap<>();
            recipient.put("email", toEmail);

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("sender", sender);
            payload.put("to", List.of(recipient));
            payload.put("subject", subject);
            payload.put("textContent", body);

            String requestBody = json.writeValueAsString(payload);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BREVO_ENDPOINT))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .header("api-key", apiKey)
                    .timeout(Duration.ofSeconds(10))
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                log.info("Email sent to {} (status {})", toEmail, response.statusCode());
            } else {
                log.error("Failed to send email to {}: Brevo returned {} - {}",
                        toEmail, response.statusCode(), response.body());
            }
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", toEmail, e.getMessage());
        }
    }
}
