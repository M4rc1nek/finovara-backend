package com.finovara.authservice.settings.security.operationauthorization.service;

import com.finovara.authservice.settings.security.SecuritySettings;
import com.finovara.authservice.settings.security.SecuritySettingsRepository;
import com.finovara.authservice.settings.security.operationauthorization.dto.AdditionalAuthorizationRequest;
import com.finovara.authservice.settings.security.operationauthorization.dto.AdditionalAuthorizationSettingsResponse;
import com.finovara.authservice.util.confirmationpassword.service.PasswordValidator;
import com.finovara.contracts.authorization.dto.ConfirmAuthorizationCodeDto;
import com.finovara.contracts.authorization.dto.ConfirmPasswordDto;
import com.finovara.contracts.clientdata.browser.UserBrowser;
import com.finovara.contracts.clientdata.ip.ClientIp;
import com.finovara.contracts.clientdata.location.UserLocation;
import com.finovara.contracts.event.activity.secure.accountchange.activity.AccountChangesActivityEvent;
import com.finovara.contracts.exception.forbidden.InvalidPasswordException;
import com.finovara.contracts.model.activity.AccountChangesActivityType;
import com.finovara.contracts.outbox.OutboxService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdditionalAuthorizationServiceTest {

    private static final Long USER_ID = 1L;
    private static final String IP_ADDRESS = "192.168.0.1";
    private static final String BROWSER = "Chrome";
    private static final String LOCATION = "Warsaw, PL";

    @Mock
    private SecuritySettingsRepository securitySettingsRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private PasswordValidator passwordValidator;

    @Mock
    private SecuritySettings securitySettings;

    @Mock
    private OutboxService outboxService;

    @Mock
    private SecuritySettingsVisibilityService securitySettingsVisibilityService;

    @Mock
    private HttpServletRequest httpServletRequest;

    private AdditionalAuthorizationService additionalAuthorizationService;

    private MockedStatic<ClientIp> clientIpMockedStatic;
    private MockedStatic<UserBrowser> userBrowserMockedStatic;
    private MockedStatic<UserLocation> userLocationMockedStatic;

    @BeforeEach
    void setUp() {
        additionalAuthorizationService = new AdditionalAuthorizationService(
                passwordEncoder, securitySettingsRepository, passwordValidator, outboxService, securitySettingsVisibilityService);
    }

    private void mockClientDataUtils() {
        clientIpMockedStatic = mockStatic(ClientIp.class);
        userBrowserMockedStatic = mockStatic(UserBrowser.class);
        userLocationMockedStatic = mockStatic(UserLocation.class);

        clientIpMockedStatic.when(() -> ClientIp.getClientIpAddress(httpServletRequest)).thenReturn(IP_ADDRESS);
        userBrowserMockedStatic.when(() -> UserBrowser.getBrowser(httpServletRequest)).thenReturn(BROWSER);
        userLocationMockedStatic.when(() -> UserLocation.getLocationFromIp(IP_ADDRESS)).thenReturn(LOCATION);
    }

    @AfterEach
    void tearDown() {
        if (clientIpMockedStatic != null) {
            clientIpMockedStatic.close();
        }
        if (userBrowserMockedStatic != null) {
            userBrowserMockedStatic.close();
        }
        if (userLocationMockedStatic != null) {
            userLocationMockedStatic.close();
        }
    }

    @Nested
    class SaveAdditionalAuthorization {

        private ConfirmPasswordDto confirmPasswordDto;

        @BeforeEach
        void setUp() {
            confirmPasswordDto = new ConfirmPasswordDto("Password123");
        }

        @Test
        void shouldDisableAdditionalAuthorizationWhenEnabledFalse() {
            mockClientDataUtils();
            AdditionalAuthorizationRequest request = new AdditionalAuthorizationRequest(false, confirmPasswordDto);
            when(securitySettingsRepository.findByUserId(USER_ID)).thenReturn(securitySettings);

            additionalAuthorizationService.saveAdditionalAuthorization(USER_ID, request, httpServletRequest);

            verify(passwordValidator, times(1)).validatePassword(USER_ID, confirmPasswordDto);
            verify(securitySettings, times(1)).setAdditionalAuthorizationEnabled(false);
            verify(securitySettings, times(1)).setAdditionalAuthorizationCode(null);
            verify(securitySettingsRepository, times(1)).save(securitySettings);
        }

        @Test
        void shouldPublishDisabledEventWhenEnabledFalse() {
            mockClientDataUtils();
            AdditionalAuthorizationRequest request = new AdditionalAuthorizationRequest(false, confirmPasswordDto);
            when(securitySettingsRepository.findByUserId(USER_ID)).thenReturn(securitySettings);

            additionalAuthorizationService.saveAdditionalAuthorization(USER_ID, request, httpServletRequest);

            ArgumentCaptor<AccountChangesActivityEvent> eventCaptor = ArgumentCaptor.forClass(AccountChangesActivityEvent.class);
            verify(outboxService, times(1)).save(eq("User"), eq(USER_ID.toString()), eq("activity.account-changes"), eventCaptor.capture());

            AccountChangesActivityEvent publishedEvent = eventCaptor.getValue();
            assertEquals(AccountChangesActivityType.ADDITIONAL_AUTHORIZATION_DISABLED, publishedEvent.type());
            assertEquals(USER_ID, publishedEvent.userId());
            assertEquals(IP_ADDRESS, publishedEvent.ipAddress());
            assertEquals(BROWSER, publishedEvent.browser());
            assertEquals(LOCATION, publishedEvent.location());
        }

        @Test
        void shouldNotModifySecuritySettingsWhenEnabledTrue() {
            AdditionalAuthorizationRequest request = new AdditionalAuthorizationRequest(true, confirmPasswordDto);

            additionalAuthorizationService.saveAdditionalAuthorization(USER_ID, request, httpServletRequest);

            verify(passwordValidator, times(1)).validatePassword(USER_ID, confirmPasswordDto);
            verify(securitySettingsRepository, never()).findByUserId(any());
            verify(securitySettingsRepository, never()).save(any());
        }

        @Test
        void shouldNotPublishAnyEventWhenEnabledTrue() {
            // Włączenie 2FA finalizuje się dopiero po potwierdzeniu kodu email
            // w AdditionalAuthorizationEmailVerificationService — tutaj nic nie powinno się wydarzyć.
            AdditionalAuthorizationRequest request = new AdditionalAuthorizationRequest(true, confirmPasswordDto);

            additionalAuthorizationService.saveAdditionalAuthorization(USER_ID, request, httpServletRequest);

            verifyNoInteractions(outboxService);
        }

        @Test
        void shouldThrowExceptionWhenPasswordValidationFails() {
            AdditionalAuthorizationRequest request = new AdditionalAuthorizationRequest(false, confirmPasswordDto);
            doThrow(new InvalidPasswordException("Invalid password")).when(passwordValidator).validatePassword(USER_ID, confirmPasswordDto);

            assertThrows(InvalidPasswordException.class, () -> additionalAuthorizationService.saveAdditionalAuthorization(USER_ID, request, httpServletRequest));

            verify(securitySettingsRepository, never()).findByUserId(any());
            verify(securitySettingsRepository, never()).save(any());
            verifyNoInteractions(outboxService);
        }
    }

    @Nested
    class GetAdditionalAuthorizationSettings {

        @Test
        void shouldReturnEnabledTrueWhenAdditionalAuthorizationEnabled() {
            when(securitySettingsRepository.findByUserId(USER_ID)).thenReturn(securitySettings);
            when(securitySettings.isAdditionalAuthorizationEnabled()).thenReturn(true);

            AdditionalAuthorizationSettingsResponse response = additionalAuthorizationService.getAdditionalAuthorizationSettings(USER_ID);

            assertTrue(response.additionalAuthorizationEnabled());
        }

        @Test
        void shouldReturnEnabledFalseWhenAdditionalAuthorizationDisabled() {
            when(securitySettingsRepository.findByUserId(USER_ID)).thenReturn(securitySettings);
            when(securitySettings.isAdditionalAuthorizationEnabled()).thenReturn(false);

            AdditionalAuthorizationSettingsResponse response = additionalAuthorizationService.getAdditionalAuthorizationSettings(USER_ID);

            assertFalse(response.additionalAuthorizationEnabled());
        }
    }

    @Nested
    class RegenerateCode {

        private ConfirmPasswordDto confirmPasswordDto;

        @BeforeEach
        void setUp() {
            confirmPasswordDto = new ConfirmPasswordDto("Password123");
        }

        @Test
        void shouldRegenerateCodeWhenAdditionalAuthorizationEnabled() {
            when(securitySettingsRepository.findByUserId(USER_ID)).thenReturn(securitySettings);
            when(securitySettings.isAdditionalAuthorizationEnabled()).thenReturn(true);

            additionalAuthorizationService.regenerateCode(USER_ID, confirmPasswordDto);

            verify(passwordValidator, times(1)).validatePassword(USER_ID, confirmPasswordDto);
        }

        @Test
        void shouldThrowExceptionWhenAdditionalAuthorizationNotEnabled() {
            when(securitySettingsRepository.findByUserId(USER_ID)).thenReturn(securitySettings);
            when(securitySettings.isAdditionalAuthorizationEnabled()).thenReturn(false);

            assertThrows(InvalidPasswordException.class, () -> additionalAuthorizationService.regenerateCode(USER_ID, confirmPasswordDto));
        }

        @Test
        void shouldThrowExceptionWhenPasswordValidationFails() {
            doThrow(new InvalidPasswordException("Invalid password")).when(passwordValidator).validatePassword(USER_ID, confirmPasswordDto);

            assertThrows(InvalidPasswordException.class, () -> additionalAuthorizationService.regenerateCode(USER_ID, confirmPasswordDto));

            verify(securitySettingsRepository, never()).findByUserId(any());
        }
    }

    @Nested
    class VisibleAdditionalAuthorization {

        @Test
        void shouldReturnTrueWhenAdditionalAuthorizationVisible() {
            when(securitySettingsRepository.findByUserId(USER_ID)).thenReturn(securitySettings);
            when(securitySettings.isAdditionalAuthorizationVisible()).thenReturn(true);

            Boolean result = additionalAuthorizationService.visibleAdditionalAuthorization(USER_ID);

            assertTrue(result);
        }

        @Test
        void shouldReturnFalseWhenAdditionalAuthorizationNotVisible() {
            when(securitySettingsRepository.findByUserId(USER_ID)).thenReturn(securitySettings);
            when(securitySettings.isAdditionalAuthorizationVisible()).thenReturn(false);

            Boolean result = additionalAuthorizationService.visibleAdditionalAuthorization(USER_ID);

            assertFalse(result);
        }
    }

    @Nested
    class ConfirmAdditionalAuthorizationCode {

        @Test
        void shouldReturnWithoutValidationWhenAdditionalAuthorizationDisabled() {
            when(securitySettingsRepository.findByUserId(USER_ID)).thenReturn(securitySettings);
            when(securitySettings.isAdditionalAuthorizationEnabled()).thenReturn(false);

            additionalAuthorizationService.confirmAdditionalAuthorizationCode(USER_ID, null);

            verify(passwordEncoder, never()).matches(any(), any());
            verifyNoInteractions(securitySettingsVisibilityService);
            verify(securitySettingsRepository, never()).save(any());
        }

        @Test
        void shouldMarkVisibleAndThrowExceptionWhenDtoIsNullAndAuthorizationEnabled() {
            when(securitySettingsRepository.findByUserId(USER_ID)).thenReturn(securitySettings);
            when(securitySettings.isAdditionalAuthorizationEnabled()).thenReturn(true);

            assertThrows(InvalidPasswordException.class, () -> additionalAuthorizationService.confirmAdditionalAuthorizationCode(USER_ID, null));

            verify(securitySettingsVisibilityService, times(1)).markVisible(securitySettings);
            verify(securitySettings, never()).setAdditionalAuthorizationVisible(false);
            verify(securitySettingsRepository, never()).save(any());
        }

        @Test
        void shouldMarkVisibleAndThrowExceptionWhenCodeIsNullAndAuthorizationEnabled() {
            when(securitySettingsRepository.findByUserId(USER_ID)).thenReturn(securitySettings);
            when(securitySettings.isAdditionalAuthorizationEnabled()).thenReturn(true);
            ConfirmAuthorizationCodeDto dto = new ConfirmAuthorizationCodeDto(null);

            assertThrows(InvalidPasswordException.class, () -> additionalAuthorizationService.confirmAdditionalAuthorizationCode(USER_ID, dto));

            verify(securitySettingsVisibilityService, times(1)).markVisible(securitySettings);
            verify(securitySettingsRepository, never()).save(any());
        }

        @Test
        void shouldMarkVisibleAndThrowExceptionWhenCodeDoesNotMatch() {
            when(securitySettingsRepository.findByUserId(USER_ID)).thenReturn(securitySettings);
            when(securitySettings.isAdditionalAuthorizationEnabled()).thenReturn(true);
            when(securitySettings.getAdditionalAuthorizationCode()).thenReturn("encodedCode");
            ConfirmAuthorizationCodeDto dto = new ConfirmAuthorizationCodeDto("123456");
            when(passwordEncoder.matches("123456", "encodedCode")).thenReturn(false);

            assertThrows(InvalidPasswordException.class, () -> additionalAuthorizationService.confirmAdditionalAuthorizationCode(USER_ID, dto));

            verify(securitySettingsVisibilityService, times(1)).markVisible(securitySettings);
            verify(securitySettingsRepository, never()).save(any());
        }

        @Test
        void shouldMarkNotVisibleAndNotThrowExceptionWhenCodeMatches() {
            when(securitySettingsRepository.findByUserId(USER_ID)).thenReturn(securitySettings);
            when(securitySettings.isAdditionalAuthorizationEnabled()).thenReturn(true);
            when(securitySettings.getAdditionalAuthorizationCode()).thenReturn("encodedCode");
            ConfirmAuthorizationCodeDto dto = new ConfirmAuthorizationCodeDto("123456");
            when(passwordEncoder.matches("123456", "encodedCode")).thenReturn(true);

            additionalAuthorizationService.confirmAdditionalAuthorizationCode(USER_ID, dto);

            verify(passwordEncoder, times(1)).matches("123456", "encodedCode");
            verify(securitySettings, times(1)).setAdditionalAuthorizationVisible(false);
            verify(securitySettingsRepository, times(1)).save(securitySettings);
            verifyNoInteractions(securitySettingsVisibilityService);
        }
    }
}