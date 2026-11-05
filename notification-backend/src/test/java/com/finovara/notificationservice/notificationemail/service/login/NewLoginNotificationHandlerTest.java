package com.finovara.notificationservice.notificationemail.service.login;

import com.finovara.contracts.mainaccount.activity.event.secure.login.activity.LoginActivityEvent;
import com.finovara.contracts.mainaccount.activity.model.LoginActivityStatus;
import com.finovara.contracts.user.authorization.dto.UserDataResponse;
import com.finovara.notificationservice.feignclient.AuthBackendClient;
import com.finovara.notificationservice.notificationemail.model.ActionEmailNotificationType;
import com.finovara.notificationservice.notificationemail.model.NotificationEmailSettings;
import com.finovara.notificationservice.notificationemail.settings.repository.NotificationEmailSettingsRepository;
import com.finovara.notificationservice.notificationemail.util.EmailNotifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NewLoginNotificationHandlerTest {

    private static final Long USER_ID = 42L;
    private static final String EMAIL = "user@example.com";
    private static final LocalDateTime LOGIN_TIME = LocalDateTime.of(2026, 11, 5, 10, 15, 30);

    @Mock
    private NotificationEmailSettingsRepository settingsRepository;

    @Mock
    private AuthBackendClient authBackendClient;

    @Mock
    private EmailNotifier emailNotifier;

    private NewLoginNotificationHandler handler;

    @BeforeEach
    void setUp() {
        handler = new NewLoginNotificationHandler(settingsRepository, authBackendClient, emailNotifier);
    }

    @Test
    void shouldIgnoreUnsuccessfulLogins() {
        handler.handle(new LoginActivityEvent(USER_ID, LoginActivityStatus.UNSUCCESSFUL,
                "browser", "192.0.2.1", "Warsaw, Poland", LOGIN_TIME));

        verifyNoInteractions(settingsRepository, authBackendClient, emailNotifier);
    }

    @Test
    void shouldNotLookUpUserWhenNotificationIsDisabled() {
        NotificationEmailSettings settings = settings(false);
        when(settingsRepository.findByUserId(USER_ID)).thenReturn(Optional.of(settings));

        handler.handle(successfulLogin("192.0.2.1", "Warsaw, Poland"));

        verifyNoInteractions(authBackendClient, emailNotifier);
    }

    @Test
    void shouldSendLoginDetailsWhenNotificationIsEnabled() {
        when(settingsRepository.findByUserId(USER_ID)).thenReturn(Optional.of(settings(true)));
        when(authBackendClient.getUserData(USER_ID)).thenReturn(
                new UserDataResponse(USER_ID, Optional.of("Jan"), Optional.of(EMAIL)));
        ArgumentCaptor<Map<String, String>> placeholdersCaptor = ArgumentCaptor.forClass(Map.class);

        handler.handle(successfulLogin("192.0.2.1", "Warsaw, Poland"));

        verify(emailNotifier).send(eq(ActionEmailNotificationType.NEW_LOGIN), eq(EMAIL), placeholdersCaptor.capture());
        assertEquals("Jan", placeholdersCaptor.getValue().get("username"));
        assertEquals("192.0.2.1", placeholdersCaptor.getValue().get("ipAddress"));
        assertEquals("Warsaw, Poland", placeholdersCaptor.getValue().get("location"));
        assertEquals("05.11.2026 11:15:30", placeholdersCaptor.getValue().get("date"));
    }

    @Test
    void shouldApplyPolishDaylightSavingTimeToLoginDate() {
        when(settingsRepository.findByUserId(USER_ID)).thenReturn(Optional.of(settings(true)));
        when(authBackendClient.getUserData(USER_ID)).thenReturn(
                new UserDataResponse(USER_ID, Optional.of("Jan"), Optional.of(EMAIL)));
        ArgumentCaptor<Map<String, String>> placeholdersCaptor = ArgumentCaptor.forClass(Map.class);
        LocalDateTime summerLoginTime = LocalDateTime.of(2026, 7, 5, 10, 15, 30);

        handler.handle(new LoginActivityEvent(USER_ID, LoginActivityStatus.SUCCESSFUL,
                "browser", "192.0.2.1", "Warsaw, Poland", summerLoginTime));

        verify(emailNotifier).send(any(), any(), placeholdersCaptor.capture());
        assertEquals("05.07.2026 12:15:30", placeholdersCaptor.getValue().get("date"));
    }

    @Test
    void shouldEscapeValuesBeforeAddingThemToHtmlTemplate() {
        when(settingsRepository.findByUserId(USER_ID)).thenReturn(Optional.of(settings(true)));
        when(authBackendClient.getUserData(USER_ID)).thenReturn(
                new UserDataResponse(USER_ID, Optional.of("<script>"), Optional.of(EMAIL)));
        ArgumentCaptor<Map<String, String>> placeholdersCaptor = ArgumentCaptor.forClass(Map.class);

        handler.handle(successfulLogin("<127.0.0.1>", "City & Country"));

        verify(emailNotifier).send(any(), any(), placeholdersCaptor.capture());
        assertEquals("&lt;script&gt;", placeholdersCaptor.getValue().get("username"));
        assertEquals("&lt;127.0.0.1&gt;", placeholdersCaptor.getValue().get("ipAddress"));
        assertEquals("City &amp; Country", placeholdersCaptor.getValue().get("location"));
    }

    @Test
    void shouldNotSendWhenUserHasNoEmail() {
        when(settingsRepository.findByUserId(USER_ID)).thenReturn(Optional.of(settings(true)));
        when(authBackendClient.getUserData(USER_ID)).thenReturn(
                new UserDataResponse(USER_ID, Optional.of("Jan"), Optional.empty()));

        handler.handle(successfulLogin("192.0.2.1", "Warsaw, Poland"));

        verify(emailNotifier, never()).send(any(), any(), any());
    }

    private NotificationEmailSettings settings(boolean enabled) {
        return NotificationEmailSettings.builder().userId(USER_ID).notifyOnNewLogin(enabled).build();
    }

    private LoginActivityEvent successfulLogin(String ipAddress, String location) {
        return new LoginActivityEvent(USER_ID, LoginActivityStatus.SUCCESSFUL,
                "browser", ipAddress, location, LOGIN_TIME);
    }
}
