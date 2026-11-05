package com.finovara.notificationservice.notificationemail.settings.action.newlogin.service;

import com.finovara.contracts.user.authorization.additionalcode.resolver.AdditionalAuthorizationCodeResolver;
import com.finovara.contracts.outbox.OutboxService;
import com.finovara.notificationservice.feignclient.AuthBackendClient;
import com.finovara.notificationservice.notificationemail.dto.NotificationEmailDto;
import com.finovara.notificationservice.notificationemail.model.NotificationEmailSettings;
import com.finovara.notificationservice.notificationemail.settings.repository.NotificationEmailSettingsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotifyNewLoginServiceActionTest {

    private static final Long USER_ID = 42L;

    @Mock
    private NotificationEmailSettingsRepository settingsRepository;

    @Mock
    private AuthBackendClient authBackendClient;

    @Mock
    private OutboxService outboxService;

    @Mock
    private AdditionalAuthorizationCodeResolver authorizationCodeResolver;

    @Mock
    private NotificationEmailSettings settings;

    private NotifyNewLoginServiceAction service;

    @BeforeEach
    void setUp() {
        service = new NotifyNewLoginServiceAction(
                settingsRepository, authBackendClient, outboxService, authorizationCodeResolver);
    }

    @Test
    void shouldReturnCurrentNewLoginSetting() {
        when(settingsRepository.findByUserId(USER_ID)).thenReturn(Optional.of(settings));
        when(settings.isNotifyOnNewLogin()).thenReturn(true);

        NotificationEmailDto result = service.getEmailNotification(USER_ID);

        assertTrue(result.enabled());
    }

    @Test
    void shouldSaveSettingAndPublishActivity() {
        when(settingsRepository.findByUserId(USER_ID)).thenReturn(Optional.of(settings));

        service.saveEmailNotification(USER_ID, new NotificationEmailDto(true, null));

        verify(settings).setNotifyOnNewLogin(true);
        verify(settingsRepository).save(settings);
        verify(outboxService).save(eq("NotificationSettings"), eq(USER_ID.toString()), eq("settings.changed"), any());
    }

    @Test
    void shouldReturnDisabledSettingWhenOff() {
        when(settingsRepository.findByUserId(USER_ID)).thenReturn(Optional.of(settings));
        when(settings.isNotifyOnNewLogin()).thenReturn(false);

        NotificationEmailDto result = service.getEmailNotification(USER_ID);

        assertFalse(result.enabled());
    }
}
