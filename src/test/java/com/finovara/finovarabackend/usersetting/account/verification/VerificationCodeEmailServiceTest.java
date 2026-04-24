package com.finovara.finovarabackend.usersetting.account.verification;

import com.finovara.finovarabackend.exception.serviceunavailable.ServiceUnavailableException;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.usersetting.account.service.verification.VerificationCodeEmailService;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VerificationCodeEmailServiceTest {

    private JavaMailSender javaMailSender;
    private VerificationCodeEmailService service;
    private User user;

    @BeforeEach
    void SetUp() {
        javaMailSender = mock(JavaMailSender.class);
        service = new VerificationCodeEmailService(javaMailSender);

        ReflectionTestUtils.setField(service, "recipientAddress", "test@finovara.com");

        MimeMessage mimeMessage = mock(MimeMessage.class);
        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);

        user = new User();
        user.setId(1L);
        user.setUsername("TestUser");
    }

    @Test
    void ShouldSendEmailChangeCodeCorrectly() {
        service.sendEmailChangeCode(user, "user@test.com", 123456);

        verify(javaMailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    void ShouldSendPasswordResetCodeCorrectly() {
        service.sendPasswordResetCode(user, "user@test.com", 654321);

        verify(javaMailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    void ShouldThrowExceptionWhenMailSenderCreateFails() {
        when(javaMailSender.createMimeMessage()).thenThrow(new RuntimeException());

        assertThrows(ServiceUnavailableException.class, () -> service.sendEmailChangeCode(user, "fail@test.com", 111111));
    }

    @Test
    void ShouldThrowExceptionWhenMailSenderSendFails() {
        doThrow(new RuntimeException()).when(javaMailSender).send(any(MimeMessage.class));

        assertThrows(ServiceUnavailableException.class, () -> service.sendPasswordResetCode(user, "fail@test.com", 222222));
    }

}