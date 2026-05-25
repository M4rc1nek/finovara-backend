package com.finovara.corebackend.usersetting.notificationemail.action.passwordchange.service;

import com.finovara.activityservice.contracts.event.settings.SettingsActivityEvent;
import com.finovara.activityservice.contracts.model.activity.SettingActivityStatus;
import com.finovara.activityservice.contracts.model.activity.SettingType;
import com.finovara.corebackend.user.model.User;
import com.finovara.corebackend.usersetting.notificationemail.dto.NotificationEmailDto;
import com.finovara.corebackend.usersetting.notificationemail.model.NotificationEmailSettings;
import com.finovara.corebackend.usersetting.notificationemail.util.NotificationEmailSender;
import com.finovara.corebackend.usersetting.notificationemail.util.emailsender.PasswordChangeNotifier;
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
class NotifyPasswordChangeServiceTest {

    @Mock
    private UserManagerService userManagerService;

    @Mock
    private NotificationEmailSender notificationEmailSender;

    @Mock
    private PasswordChangeNotifier passwordChangeNotifier;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private NotifyPasswordChangeService notifyPasswordChangeService;

    private static final Long USER_ID = 1L;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(USER_ID)
                .build();
    }

    @Test
    void shouldReturnTrueWhenDtoIsEnabled() {
        NotificationEmailDto dto = new NotificationEmailDto(true);

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
        notifyPasswordChangeService.sendEmailToUser(user);

        verify(passwordChangeNotifier).sendEmail(user);
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

    @Test
    void shouldUseCorrectKafkaTopic() {
        notifyPasswordChangeService.handleActivity(USER_ID, true);

        verify(kafkaTemplate).send(eq("activity.settings"), any(SettingsActivityEvent.class));
    }
}