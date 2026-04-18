package com.finovara.finovarabackend.usersetting.accountsetting.passwordpolicy.forgotpassword;

import com.finovara.finovarabackend.exception.serviceunavailable.ServiceUnavailableException;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.user.repository.UserRepository;
import com.finovara.finovarabackend.usersetting.account.dto.passwordpolicy.ForgotPasswordDto;
import com.finovara.finovarabackend.usersetting.account.model.AccountSettings;
import com.finovara.finovarabackend.usersetting.account.repository.AccountRepository;
import com.finovara.finovarabackend.usersetting.account.service.passwordpolicy.ForgotPasswordService;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ForgotPasswordEmailSendTest {

    @Mock
    private JavaMailSender javaMailSender;
    @Mock
    private UserRepository userRepository;
    @Mock
    private AccountRepository accountRepository;
    @InjectMocks
    private ForgotPasswordService forgotPasswordService;

    private User user;
    private AccountSettings accountSettings;

    private final String EMAIL = "test@test.com";

    @BeforeEach
    void setup() {
        user = new User();
        user.setUsername("testUser");

        accountSettings = new AccountSettings();
        user.setAccountSettings(accountSettings);

        ReflectionTestUtils.setField(forgotPasswordService, "recipientAddress", "test@finovara.com");
    }

    @Test
    void shouldSendEmailSuccessfully() {
        ForgotPasswordDto dto = new ForgotPasswordDto(EMAIL, null);

        when(javaMailSender.createMimeMessage()).thenReturn(mock(MimeMessage.class));
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        forgotPasswordService.emailSend(EMAIL, dto);

        verify(javaMailSender).send(any(MimeMessage.class));

        verify(accountRepository).save(accountSettings);
    }

    @Test
    void shouldGenerateForgotPasswordCodeAndExpiration() {
        ForgotPasswordDto dto = new ForgotPasswordDto(EMAIL, null);

        when(javaMailSender.createMimeMessage()).thenReturn(mock(MimeMessage.class));
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        forgotPasswordService.emailSend(EMAIL, dto);

        assertNotNull(accountSettings.getForgotPasswordCode());
        assertNotNull(accountSettings.getForgotPasswordCodeExpiresAt());
    }

    @Test
    void shouldThrowExceptionWhenEmailSendingFails() {
        ForgotPasswordDto dto = new ForgotPasswordDto(EMAIL, null);

        when(javaMailSender.createMimeMessage()).thenThrow(new RuntimeException("Mail error"));

        assertThrows(ServiceUnavailableException.class, () -> forgotPasswordService.emailSend(EMAIL, dto));
    }
}