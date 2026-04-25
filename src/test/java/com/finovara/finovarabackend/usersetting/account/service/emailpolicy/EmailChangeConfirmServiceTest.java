package com.finovara.finovarabackend.usersetting.account.service.emailpolicy;

import com.finovara.finovarabackend.exception.badrequest.InvalidInputException;
import com.finovara.finovarabackend.exception.badrequest.InvalidVerificationCodeException;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.usersetting.account.dto.AttemptsDto;
import com.finovara.finovarabackend.usersetting.account.dto.emailpolicy.EmailChangeConfirmDto;
import com.finovara.finovarabackend.usersetting.account.model.AccountSettings;
import com.finovara.finovarabackend.usersetting.account.service.emailpolicy.EmailChangeService;
import com.finovara.finovarabackend.usersetting.account.service.emailpolicy.EmailUpdateService;
import com.finovara.finovarabackend.usersetting.account.service.verification.VerificationCodeManager;
import com.finovara.finovarabackend.util.user.service.UserManagerService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class EmailChangeConfirmServiceTest {
    @Mock
    private UserManagerService userManagerService;
    @Mock
    private VerificationCodeManager verificationCodeManager;

    @Mock
    private EmailUpdateService emailUpdateService;
    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private EmailChangeService emailChangeService;

    private User user;
    private AccountSettings settings;
    private Long userId;
    private String pendingEmail;

    @BeforeEach
    void setUp() {
        userId = 1L;
        user = new User();
        settings = new AccountSettings();

        user.setAccountSettings(settings);

        pendingEmail = "email@mail.com";
        settings.setPendingEmail(pendingEmail);
    }

    @Test
    void shouldConfirmEmailChangeSuccessfully() {
        Integer code = 123456;

        EmailChangeConfirmDto dto = new EmailChangeConfirmDto(code);
        AttemptsDto attemptsDto = new AttemptsDto(1, 5, 4);

        when(userManagerService.getUserByIdOrThrow(userId)).thenReturn(user);
        when(verificationCodeManager.getCurrentEmailChangeAttempts(userId)).thenReturn(attemptsDto);

        AttemptsDto result = emailChangeService.confirmEmailChange(userId, dto, request);

        assertNotNull(result);
        assertEquals(1, result.used());
        assertEquals(5, result.max());
        assertEquals(4, result.remaining());

        verify(verificationCodeManager).verifyEmailChangeCode(settings, code);
        verify(verificationCodeManager).removeEmailChangeCode(settings);
        verify(emailUpdateService).updateEmail(user, pendingEmail, request);
    }

    @Test
    void shouldCallServicesInCorrectOrder() {
        Integer code = 123456;

        EmailChangeConfirmDto dto = new EmailChangeConfirmDto(code);
        AttemptsDto attemptsDto = new AttemptsDto(0, 5, 5);

        when(userManagerService.getUserByIdOrThrow(userId)).thenReturn(user);
        when(verificationCodeManager.getCurrentEmailChangeAttempts(userId)).thenReturn(attemptsDto);

        emailChangeService.confirmEmailChange(userId, dto, request);

        InOrder inOrder = inOrder(verificationCodeManager, emailUpdateService);

        inOrder.verify(verificationCodeManager).verifyEmailChangeCode(settings, code);
        inOrder.verify(verificationCodeManager).getCurrentEmailChangeAttempts(userId);
        inOrder.verify(verificationCodeManager).removeEmailChangeCode(settings);
        inOrder.verify(emailUpdateService).updateEmail(user, pendingEmail, request);
    }

    @Test
    void shouldThrowExceptionWhenVerificationCodeIsInvalid() {
        Integer code = 123456;
        String errorMessage = "invalid code";

        EmailChangeConfirmDto dto = new EmailChangeConfirmDto(code);
        AttemptsDto attemptsDto = new AttemptsDto(2, 5, 3);

        when(userManagerService.getUserByIdOrThrow(userId)).thenReturn(user);

        doThrow(new InvalidInputException(errorMessage)).when(verificationCodeManager).verifyEmailChangeCode(settings, code);

        when(verificationCodeManager.verifyEmailChangeAttemptsCode(userId, settings)).thenReturn(attemptsDto);

        InvalidVerificationCodeException exception = assertThrows(InvalidVerificationCodeException.class,
                () -> emailChangeService.confirmEmailChange(userId, dto, request));

        assertEquals(errorMessage, exception.getMessage());
        assertNotNull(exception.getAttempts());
        assertEquals(2, exception.getAttempts().used());
        assertEquals(5, exception.getAttempts().max());
        assertEquals(3, exception.getAttempts().remaining());

        verify(verificationCodeManager).verifyEmailChangeAttemptsCode(userId, settings);
        verify(emailUpdateService, never()).updateEmail(any(), any(), any());
    }
}