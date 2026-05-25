package com.finovara.corebackend.usersetting.notificationemail.action.accountdeleted.service;

import com.finovara.activityservice.contracts.event.settings.SettingsActivityEvent;
import com.finovara.activityservice.contracts.model.activity.SettingActivityStatus;
import com.finovara.activityservice.contracts.model.activity.SettingType;
import com.finovara.corebackend.user.model.User;
import com.finovara.corebackend.usersetting.notificationemail.dto.NotificationEmailDto;
import com.finovara.corebackend.usersetting.notificationemail.model.NotificationEmailSettings;
import com.finovara.corebackend.usersetting.notificationemail.util.NotificationEmailSender;
import com.finovara.corebackend.usersetting.notificationemail.util.emailsender.AccountDeletedNotifier;
import com.finovara.corebackend.util.user.service.UserManagerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotifyOnAccountDeletedServiceTest {

    @Mock
    private UserManagerService userManagerService;

    @Mock
    private NotificationEmailSender notificationEmailSender;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Mock
    private AccountDeletedNotifier accountDeletedNotifier;

    @InjectMocks
    private NotifyOnAccountDeletedService notifyOnAccountDeletedService;

    private User user;

    private static final Long USER_ID = 1L;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(USER_ID)
                .build();
    }

    @Test
    void shouldReturnTrueWhenDtoIsEnabled() {
        NotificationEmailDto dto = new NotificationEmailDto(true);

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
        notifyOnAccountDeletedService.sendEmailToUser(user);

        verify(accountDeletedNotifier).sendEmail(user);
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

    @Test
    void shouldUseCorrectKafkaTopic() {
        notifyOnAccountDeletedService.handleActivity(USER_ID, true);

        verify(kafkaTemplate).send(eq("activity.settings"), any(SettingsActivityEvent.class));
    }
}