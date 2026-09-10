package com.example.wms.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Sends password-reset emails via SMTP, using the credentials in
 * application.yml's spring.mail block. JavaMailSender is a Spring Boot bean
 * that gets auto-created as soon as spring.mail.host is set — we just ask
 * for it as a normal constructor dependency, same as any repository.
 *
 * The try/catch below is the one piece of defensiveness kept: email is a
 * side effect of resetting a password, not the actual outcome the caller
 * depends on, so a bad SMTP credential or a network hiccup shouldn't turn
 * into a 500 error for someone just trying to reset their password.
 */
@Service
public class MailService {

    private static final Logger log = LoggerFactory.getLogger(MailService.class);

    private final JavaMailSender mailSender;

    public MailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendPasswordResetEmail(String toEmail, String resetLink) {
        sendNotification(
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
    public void sendNotification(String toEmail, String subject, String body) {
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
