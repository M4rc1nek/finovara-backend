package com.finovara.notificationservice.notificationemail.action.accountdeleted.service;

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
class NotifyOnAccountDeletedServiceTest {

    @Mock
    private NotificationEmailSettingsRepository repository;
    @Mock
    private NotificationEmailSender notificationEmailSender;
    @Mock
    private AuthBackendClient authBackendClient;
    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;
    @Mock
    private EmailNotifier emailNotifier;

    @InjectMocks
    private NotifyOnAccountDeletedService notifyOnAccountDeletedService;

    private static final Long USER_ID = 1L;

    @Test
    void shouldReturnTrueWhenDtoIsEnabled() {
        NotificationEmailDto dto = new NotificationEmailDto(true, "auth-code");

        boolean result = notifyOnAccountDeletedService.isEnabled(dto);

        assertThat(result).isTrue();
    }

    @Test
    void shouldApplySettingToNotificationEmailSettings() {
        NotificationEmailSettings settings = new NotificationEmailSettings();

        notifyOnAccountDeletedService.applySetting(settings, true);

        assertThat(settings.isNotifyOnAccountDeleted()).isTrue();
    }

    @Test
    void shouldReturnCorrectSettingStateFromSettings() {
        NotificationEmailSettings settings = new NotificationEmailSettings();
        settings.setNotifyOnAccountDeleted(true);

        boolean result = notifyOnAccountDeletedService.isNotificationEmailSettingsEnabled(settings);

        assertThat(result).isTrue();
    }

    @Test
    void shouldMapSettingsToDto() {
        NotificationEmailSettings settings = new NotificationEmailSettings();
        settings.setNotifyOnAccountDeleted(true);

        NotificationEmailDto dto = notifyOnAccountDeletedService.mapToDto(settings);

        assertThat(dto.enabled()).isTrue();
    }

    @Test
    void shouldSendEmailToUser() {
        UserEmailDataDto data = new UserEmailDataDto(USER_ID, "john", "john@example.com");

        notifyOnAccountDeletedService.sendEmailToUser(USER_ID, data);

        verify(emailNotifier).send(EmailNotificationType.ACCOUNT_DELETED, USER_ID, "john", "john@example.com");
    }

    @Test
    void shouldPublishSettingsActivityWhenEnabled() {
        notifyOnAccountDeletedService.handleActivity(USER_ID, true);

        ArgumentCaptor<SettingsActivityEvent> captor =
                ArgumentCaptor.forClass(SettingsActivityEvent.class);

        verify(kafkaTemplate).send(eq("activity.settings"), captor.capture());

        SettingsActivityEvent event = captor.getValue();

        assertThat(event.userId()).isEqualTo(USER_ID);
        assertThat(event.settingType()).isEqualTo(SettingType.NOTIFICATION_ACCOUNT_DELETED);
        assertThat(event.status()).isEqualTo(SettingActivityStatus.ENABLED);
        assertThat(event.occurredAt()).isNotNull();
    }

    @Test
    void shouldPublishSettingsActivityWhenDisabled() {
        notifyOnAccountDeletedService.handleActivity(USER_ID, false);

        ArgumentCaptor<SettingsActivityEvent> captor =
                ArgumentCaptor.forClass(SettingsActivityEvent.class);

        verify(kafkaTemplate).send(eq("activity.settings"), captor.capture());

        SettingsActivityEvent event = captor.getValue();

        assertThat(event.status()).isEqualTo(SettingActivityStatus.DISABLED);
    }
}
