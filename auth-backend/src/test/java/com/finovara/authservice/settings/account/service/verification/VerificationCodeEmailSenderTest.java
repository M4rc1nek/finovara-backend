package com.finovara.authservice.settings.account.service.verification;

import com.finovara.contracts.exception.serviceunavailable.ServiceUnavailableException;
import com.finovara.authservice.user.model.User;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VerificationCodeEmailSenderTest {

    @Mock
    private JavaMailSender javaMailSender;

    @InjectMocks
    private VerificationCodeEmailSender verificationCodeEmailSender;

    private User user;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(verificationCodeEmailSender, "recipientAddress", "test@finovara.com");

        MimeMessage mimeMessage = mock(MimeMessage.class);
        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);

        user = new User();
        user.setId(1L);
        user.setUsername("TestUser");
    }

    @Test
    void shouldSendEmailChangeCodeCorrectly() {
        verificationCodeEmailSender.sendEmailChangeCode(user, "user@test.com", 123456);

        verify(javaMailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    void shouldSendPasswordResetCodeCorrectly() {
        verificationCodeEmailSender.sendPasswordResetCode(user, "user@test.com", 654321);

        verify(javaMailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    void shouldSendAdditionalAuthorizationCodeCorrectly() {
        verificationCodeEmailSender.sendAuthorizationConfirmCode(user, "user@test.com", 612930);

        verify(javaMailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    void shouldThrowExceptionWhenCreateMimeMessageFails() {
        when(javaMailSender.createMimeMessage()).thenThrow(new RuntimeException());

        assertThrows(ServiceUnavailableException.class, () -> verificationCodeEmailSender.sendEmailChangeCode(user, "fail@test.com", 111111));
    }

    @Test
    void shouldThrowExceptionWhenSendFails() {
        MimeMessage mimeMessage = mock(MimeMessage.class);
        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);

        doThrow(new RuntimeException()).when(javaMailSender).send(any(MimeMessage.class));

        assertThrows(ServiceUnavailableException.class, () -> verificationCodeEmailSender.sendPasswordResetCode(user, "fail@test.com", 222222));
    }
}