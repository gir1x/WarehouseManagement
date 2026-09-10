package com.example.wms.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Sends password-reset emails via SMTP, using the credentials in
 * application.yml's spring.mail block. JavaMailSender is a Spring Boot bean
 * that gets auto-created as soon as spring.mail.host is set — we just ask
 * for it as a normal constructor dependency, same as any repository.
 *
 * Both public methods are @Async: SMTP can be slow or unreachable (e.g. some
 * hosts block outbound port 587), and email is a side effect of the action
 * the caller actually cares about (registering, resetting a password) — it
 * shouldn't block the HTTP response thread while it waits on a mail server.
 * The real send logic lives in the private doSend() so both public entry
 * points go through Spring's async proxy (a call from one public method to
 * another on the same bean would otherwise skip the proxy and run inline).
 *
 * The try/catch is the one piece of defensiveness kept: a bad SMTP
 * credential or a network hiccup gets logged, not thrown back to a caller
 * that isn't waiting on this thread anyway.
 */
@Service
public class MailService {

    private static final Logger log = LoggerFactory.getLogger(MailService.class);

    private final JavaMailSender mailSender;

    public MailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

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
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject(subject);
        message.setText(body);

        try {
            mailSender.send(message);
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", toEmail, e.getMessage());
        }
    }
}
