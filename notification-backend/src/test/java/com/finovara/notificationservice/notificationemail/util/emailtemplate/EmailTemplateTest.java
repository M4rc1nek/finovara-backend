package com.finovara.notificationservice.notificationemail.util.emailtemplate;

import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailTemplateTest {

    @Mock
    private JavaMailSender javaMailSender;

    @InjectMocks
    private EmailTemplateService emailTemplateService;

    @Mock
    private MimeMessage mimeMessage;

    @Test
    void shouldCallJavaMailSenderSend() {
        ReflectionTestUtils.setField(emailTemplateService, "senderAddress", "test@mail.com");

        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailTemplateService.sendEmail("john@example.com", "Test Subject", "email/account-deleted.html", "john_doe", "john@example.com");

        verify(javaMailSender).send(mimeMessage);
    }
}
