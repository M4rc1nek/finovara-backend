package com.finovara.finovarabackend.util.user.accountmanagment.accountpolicy.emailtemplate;

import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.usersetting.notificationemail.util.emailtemplate.EmailTemplateService;
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

        User user = new User();
        user.setUsername("john_doe");
        user.setEmail("john@example.com");

        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailTemplateService.sendEmail(user, "Test Subject", "email/account-deleted.html", "john_doe", "john@example.com");

        verify(javaMailSender).send(mimeMessage);
    }
}