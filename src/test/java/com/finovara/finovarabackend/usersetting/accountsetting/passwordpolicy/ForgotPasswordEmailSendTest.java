package com.finovara.finovarabackend.usersetting.accountsetting.passwordpolicy;

import com.finovara.finovarabackend.exception.badrequest.InvalidInputException;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.user.repository.UserRepository;
import com.finovara.finovarabackend.usersetting.account.dto.passwordpolicy.ForgotPasswordDto;
import com.finovara.finovarabackend.usersetting.account.model.AccountSettings;
import com.finovara.finovarabackend.usersetting.account.repository.AccountRepository;
import com.finovara.finovarabackend.usersetting.account.service.passwordpolicy.ForgotPasswordService;
import com.finovara.finovarabackend.util.service.user.service.UserManagerService;
import jakarta.mail.Session;
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
class ForgotPasswordEmailSendTest {

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(forgotPasswordService, "recipientAddress", "noreply@test.com");
    }

    @Mock
    private UserManagerService userManagerService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private JavaMailSender javaMailSender;

    @InjectMocks
    private ForgotPasswordService forgotPasswordService;

    private final String EMAIL = "test@test.com";

    @Test
    void shouldThrowExceptionWhenEmailDoesNotExist() {
        when(userRepository.existsByEmail(EMAIL)).thenReturn(false);

        assertThrows(InvalidInputException.class, () -> forgotPasswordService.validateEmailExists(EMAIL));
    }

    @Test
    void shouldSendEmail() {
        MimeMessage mimeMessage = new MimeMessage((Session) null);
        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);

        User user = new User();
        AccountSettings accountSettings = new AccountSettings();
        user.setAccountSettings(accountSettings);

        when(userManagerService.getUserByEmailOrThrow(EMAIL)).thenReturn(user);

        ForgotPasswordDto dto = new ForgotPasswordDto(EMAIL, 123456);

        forgotPasswordService.emailSend(EMAIL, dto);

        verify(javaMailSender).send(any(MimeMessage.class));
    }
}