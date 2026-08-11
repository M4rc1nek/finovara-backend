/*
package com.finovara.authservice.settings.account.service.passwordpolicy.change;

import com.finovara.authservice.settings.account.dto.passwordpolicy.ChangePasswordDto;
import com.finovara.authservice.settings.account.service.verification.CredentialValidationService;
import com.finovara.authservice.settings.security.operationauthorization.service.AdditionalAuthorizationService;
import com.finovara.authservice.user.model.User;
import com.finovara.authservice.util.authorization.AdditionalAuthorizationCodeResolver;
import com.finovara.authservice.util.user.service.UserManagerService;
import com.finovara.contracts.auth.dto.ConfirmAuthorizationCodeDto;
import com.finovara.contracts.exception.notfound.RequestedEntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChangePasswordServiceTest {

    @Mock
    private UserManagerService userManagerService;

    @Mock
    private CredentialValidationService credentialValidationService;

    @Mock
    private PasswordUpdateService passwordUpdateService;

    @Mock
    private AdditionalAuthorizationService additionalAuthorizationService;

    @Mock
    private AdditionalAuthorizationCodeResolver additionalAuthorizationCodeResolver;

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private ChangePasswordService changePasswordService;

    private static final Long USER_ID = 1L;
    private static final String NEW_PASSWORD = "newPass123";
    private static final String CONFIRM_NEW_PASSWORD = "newPass123";
    private static final String OLD_PASSWORD = "oldPass123";
    private static final String AUTHORIZATION_CODE = "auth-code";

    private ChangePasswordDto changePasswordDto;
    private User user;
    private ConfirmAuthorizationCodeDto resolvedAuthorizationCode;

    @BeforeEach
    void setUp() {
        changePasswordDto = new ChangePasswordDto(NEW_PASSWORD, CONFIRM_NEW_PASSWORD, AUTHORIZATION_CODE);
        user = new User();
        user.setId(USER_ID);
        user.setPassword(OLD_PASSWORD);
        resolvedAuthorizationCode = mock(ConfirmAuthorizationCodeDto.class);
    }

    @Nested
    class ChangePassword {

        @BeforeEach
        void setUp() {
            when(additionalAuthorizationCodeResolver.resolve(AUTHORIZATION_CODE)).thenReturn(resolvedAuthorizationCode);
        }

        @Test
        void shouldChangePasswordSuccessfullyWhenDataIsValid() {
            when(userManagerService.getUserByIdOrThrow(USER_ID)).thenReturn(user);

            changePasswordService.changePassword(USER_ID, changePasswordDto, request);

            verify(userManagerService).getUserByIdOrThrow(USER_ID);
            verify(credentialValidationService).validateNewPassword(NEW_PASSWORD, CONFIRM_NEW_PASSWORD, OLD_PASSWORD);
            verify(passwordUpdateService).updatePassword(user, NEW_PASSWORD, request);
        }

        @Test
        void shouldConfirmAdditionalAuthorizationCodeWithResolvedCodeWhenChangingPassword() {
            when(userManagerService.getUserByIdOrThrow(USER_ID)).thenReturn(user);

            changePasswordService.changePassword(USER_ID, changePasswordDto, request);

            verify(additionalAuthorizationCodeResolver).resolve(AUTHORIZATION_CODE);
            verify(additionalAuthorizationService).confirmAdditionalAuthorizationCode(USER_ID, resolvedAuthorizationCode);
        }

        @Test
        void shouldPassOldPasswordFromUserToValidationWhenChangingPassword() {
            when(userManagerService.getUserByIdOrThrow(USER_ID)).thenReturn(user);

            changePasswordService.changePassword(USER_ID, changePasswordDto, request);

            verify(credentialValidationService).validateNewPassword(eq(NEW_PASSWORD), eq(CONFIRM_NEW_PASSWORD), eq(OLD_PASSWORD));
        }

        @Test
        void shouldUpdatePasswordWithSameUserInstanceReturnedByManagerWhenChangingPassword() {
            when(userManagerService.getUserByIdOrThrow(USER_ID)).thenReturn(user);

            changePasswordService.changePassword(USER_ID, changePasswordDto, request);

            verify(passwordUpdateService).updatePassword(eq(user), eq(NEW_PASSWORD), eq(request));
        }

        @Test
        void shouldThrowExceptionWhenUserNotFound() {
            when(userManagerService.getUserByIdOrThrow(USER_ID)).thenThrow(new RequestedEntityNotFoundException("User not found"));

            assertThrows(RequestedEntityNotFoundException.class, () -> changePasswordService.changePassword(USER_ID, changePasswordDto, request));

            verifyNoInteractions(credentialValidationService);
            verifyNoInteractions(passwordUpdateService);
        }

        @Test
        void shouldNotUpdatePasswordWhenCredentialValidationFails() {
            when(userManagerService.getUserByIdOrThrow(USER_ID)).thenReturn(user);
            doThrow(new IllegalArgumentException("Passwords do not match"))
                    .when(credentialValidationService).validateNewPassword(NEW_PASSWORD, CONFIRM_NEW_PASSWORD, OLD_PASSWORD);

            assertThrows(IllegalArgumentException.class, () -> changePasswordService.changePassword(USER_ID, changePasswordDto, request));

            verify(passwordUpdateService, never()).updatePassword(any(), anyString(), any());
        }

        @Test
        void shouldNotFetchUserWhenAdditionalAuthorizationCodeConfirmationFails() {
            doThrow(new IllegalArgumentException("Invalid authorization code"))
                    .when(additionalAuthorizationService).confirmAdditionalAuthorizationCode(USER_ID, resolvedAuthorizationCode);

            assertThrows(IllegalArgumentException.class, () -> changePasswordService.changePassword(USER_ID, changePasswordDto, request));

            verifyNoInteractions(userManagerService);
            verifyNoInteractions(credentialValidationService);
            verifyNoInteractions(passwordUpdateService);
        }

        @Test
        void shouldPropagateExceptionWhenPasswordUpdateFails() {
            when(userManagerService.getUserByIdOrThrow(USER_ID)).thenReturn(user);
            doThrow(new IllegalStateException("Password update failed"))
                    .when(passwordUpdateService).updatePassword(user, NEW_PASSWORD, request);

            assertThrows(IllegalStateException.class, () -> changePasswordService.changePassword(USER_ID, changePasswordDto, request));

            verify(credentialValidationService).validateNewPassword(NEW_PASSWORD, CONFIRM_NEW_PASSWORD, OLD_PASSWORD);
        }
    }

    @Nested
    class AuthorizationCodeResolution {

        @Test
        void shouldThrowExceptionWhenAuthorizationCodeResolverFails() {
            when(additionalAuthorizationCodeResolver.resolve(AUTHORIZATION_CODE))
                    .thenThrow(new IllegalArgumentException("Invalid authorization code format"));

            assertThrows(IllegalArgumentException.class, () -> changePasswordService.changePassword(USER_ID, changePasswordDto, request));

            verifyNoInteractions(additionalAuthorizationService);
            verifyNoInteractions(userManagerService);
            verifyNoInteractions(credentialValidationService);
            verifyNoInteractions(passwordUpdateService);
        }

        @Test
        void shouldResolveNullAuthorizationCodeWhenAuthorizationCodeIsNull() {
            ChangePasswordDto dtoWithNullCode = new ChangePasswordDto(NEW_PASSWORD, CONFIRM_NEW_PASSWORD, null);
            when(additionalAuthorizationCodeResolver.resolve(null)).thenReturn(resolvedAuthorizationCode);
            when(userManagerService.getUserByIdOrThrow(USER_ID)).thenReturn(user);

            changePasswordService.changePassword(USER_ID, dtoWithNullCode, request);

            verify(additionalAuthorizationCodeResolver).resolve(null);
            verify(additionalAuthorizationService).confirmAdditionalAuthorizationCode(USER_ID, resolvedAuthorizationCode);
        }
    }
}*/
