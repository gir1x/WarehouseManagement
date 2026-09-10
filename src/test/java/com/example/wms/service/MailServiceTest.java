package com.example.wms.service;

import org.junit.jupiter.api.Test;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class MailServiceTest {

    private final JavaMailSender mailSender = mock(JavaMailSender.class);
    private final MailService mailService = new MailService(mailSender);

    @Test
    void sendsThroughTheMailSender() {
        mailService.sendPasswordResetEmail("priya@example.com", "http://link");

        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    void doesNotThrowWhenSendingFails() {
        doThrow(new RuntimeException("bad credentials")).when(mailSender).send(any(SimpleMailMessage.class));

        assertThatCode(() -> mailService.sendPasswordResetEmail("priya@example.com", "http://link"))
                .doesNotThrowAnyException();
    }
}
