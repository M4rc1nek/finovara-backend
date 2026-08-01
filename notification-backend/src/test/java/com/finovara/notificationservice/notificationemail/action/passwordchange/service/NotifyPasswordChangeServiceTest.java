package com.finovara.notificationservice.notificationemail.action.passwordchange.service;

import com.finovara.contracts.event.activity.settings.SettingsActivityEvent;
import com.finovara.contracts.model.activity.SettingActivityStatus;
import com.finovara.contracts.model.activity.SettingType;
import com.finovara.notificationservice.notificationemail.dto.NotificationEmailDto;
import com.finovara.notificationservice.notificationemail.dto.UserEmailDataDto;
import com.finovara.notificationservice.notificationemail.model.EmailNotificationType;
import com.finovara.notificationservice.notificationemail.model.NotificationEmailSettings;
import com.finovara.notificationservice.notificationemail.repository.NotificationEmailSettingsRepository;
import com.finovara.notificationservice.notificationemail.util.NotificationEmailSender;
import com.finovara.notificationservice.notificationemail.util.emailsender.EmailNotifier;
import com.finovara.notificationservice.feignclient.AuthBackendClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotifyPasswordChangeServiceTest {

    @Mock
    private NotificationEmailSettingsRepository repository;
    @Mock
    private NotificationEmailSender notificationEmailSender;
    @Mock
    private AuthBackendClient authBackendClient;
    @Mock
    private EmailNotifier emailNotifier;
    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private NotifyPasswordChangeService notifyPasswordChangeService;

    private static final Long USER_ID = 1L;

    @Test
    void shouldReturnTrueWhenDtoIsEnabled() {
        NotificationEmailDto dto = new NotificationEmailDto(true, "auth-code");

        boolean result = notifyPasswordChangeService.isEnabled(dto);

        assertThat(result).isTrue();
    }

    @Test
    void shouldApplySettingToSettings() {
        NotificationEmailSettings settings = new NotificationEmailSettings();

        notifyPasswordChangeService.applySetting(settings, true);

        assertThat(settings.isNotifyOnPasswordChange()).isTrue();
    }

    @Test
    void shouldReturnCorrectSettingFromSettings() {
        NotificationEmailSettings settings = new NotificationEmailSettings();
        settings.setNotifyOnPasswordChange(true);

        boolean result = notifyPasswordChangeService.isNotificationEmailSettingsEnabled(settings);

        assertThat(result).isTrue();
    }

    @Test
    void shouldMapSettingsToDto() {
        NotificationEmailSettings settings = new NotificationEmailSettings();
        settings.setNotifyOnPasswordChange(true);

        NotificationEmailDto dto = notifyPasswordChangeService.mapToDto(settings);

        assertThat(dto.enabled()).isTrue();
    }

    @Test
    void shouldSendEmailToUser() {
        UserEmailDataDto data = new UserEmailDataDto(USER_ID, "john", "john@example.com");

        notifyPasswordChangeService.sendEmailToUser(USER_ID, data);

        verify(emailNotifier).send(EmailNotificationType.PASSWORD_CHANGED, USER_ID, "john", "john@example.com");
    }

    @Test
    void shouldSendSettingsActivityWhenEnabled() {
        notifyPasswordChangeService.handleActivity(USER_ID, true);

        ArgumentCaptor<SettingsActivityEvent> captor =
                ArgumentCaptor.forClass(SettingsActivityEvent.class);

        verify(kafkaTemplate).send(eq("activity.settings"), captor.capture());

        SettingsActivityEvent event = captor.getValue();

        assertThat(event.userId()).isEqualTo(USER_ID);
        assertThat(event.settingType()).isEqualTo(SettingType.NOTIFICATION_PASSWORD_CHANGED);
        assertThat(event.status()).isEqualTo(SettingActivityStatus.ENABLED);
        assertThat(event.occurredAt()).isNotNull();
    }

    @Test
    void shouldSendSettingsActivityWhenDisabled() {
        notifyPasswordChangeService.handleActivity(USER_ID, false);

        ArgumentCaptor<SettingsActivityEvent> captor =
                ArgumentCaptor.forClass(SettingsActivityEvent.class);

        verify(kafkaTemplate).send(eq("activity.settings"), captor.capture());

        SettingsActivityEvent event = captor.getValue();

        assertThat(event.status()).isEqualTo(SettingActivityStatus.DISABLED);
    }
}
