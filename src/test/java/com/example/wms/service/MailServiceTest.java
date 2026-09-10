package com.example.wms.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * MailService now sends via Brevo's HTTPS API (see the class comment on
 * MailService for why) instead of JavaMailSender/SMTP, so there's no
 * mail-sender bean left to mock here. With no BREVO_API_KEY/MAIL_FROM
 * configured (as in a plain unit test with no Spring context), doSend()
 * logs and returns early rather than making a network call - these tests
 * confirm that path never throws back at the caller, same intent as the
 * original "doesNotThrowWhenSendingFails" test had for the SMTP version.
 */
class MailServiceTest {

    private final MailService mailService = new MailService();

    @Test
    void doesNotThrowWhenPasswordResetEmailHasNoApiKeyConfigured() {
        assertThatCode(() -> mailService.sendPasswordResetEmail("priya@example.com", "http://link"))
                .doesNotThrowAnyException();
    }

    @Test
    void doesNotThrowWhenNotificationHasNoApiKeyConfigured() {
        assertThatCode(() -> mailService.sendNotification("priya@example.com", "subject", "body"))
                .doesNotThrowAnyException();
    }
}
