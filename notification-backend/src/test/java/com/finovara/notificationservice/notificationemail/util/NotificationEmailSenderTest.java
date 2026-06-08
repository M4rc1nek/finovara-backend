package com.finovara.notificationservice.notificationemail.util;

import com.finovara.notificationservice.notificationemail.dto.UserEmailDataDto;
import com.finovara.notificationservice.notificationemail.model.NotificationEmailSettings;
import com.finovara.notificationservice.notificationemail.repository.NotificationEmailSettingsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class NotificationEmailSenderTest {

    private static final Long USER_ID = 1L;

    private NotificationEmailSettingsRepository repository;
    private NotificationEmailSender sender;
    private UserEmailDataDto userEmailData;

    @BeforeEach
    void setUp() {
        repository = mock(NotificationEmailSettingsRepository.class);
        sender = new NotificationEmailSender(repository);
        userEmailData = new UserEmailDataDto(USER_ID, "john", "john@example.com");
    }

    @Test
    void shouldExecuteActionIfConditionTrue() {
        NotificationEmailSettings settings = new NotificationEmailSettings();
        when(repository.findByUserId(USER_ID)).thenReturn(Optional.of(settings));

        Function<NotificationEmailSettings, Boolean> condition = notificationSettings -> true;

        AtomicBoolean actionExecuted = new AtomicBoolean(false);
        BiConsumer<Long, UserEmailDataDto> action = (userId, data) -> actionExecuted.set(true);

        sender.sendIfEnabled(USER_ID, userEmailData, condition, action);

        assertTrue(actionExecuted.get());
    }

    @Test
    void shouldNotExecuteActionIfConditionFalse() {
        NotificationEmailSettings settings = new NotificationEmailSettings();
        when(repository.findByUserId(USER_ID)).thenReturn(Optional.of(settings));

        Function<NotificationEmailSettings, Boolean> condition = notificationSettings -> false;

        AtomicBoolean actionExecuted = new AtomicBoolean(false);
        BiConsumer<Long, UserEmailDataDto> action = (userId, data) -> actionExecuted.set(true);

        sender.sendIfEnabled(USER_ID, userEmailData, condition, action);

        assertFalse(actionExecuted.get());
    }
}
