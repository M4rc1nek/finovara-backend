package com.finovara.corebackend.usersetting.notificationemail.util;

import com.finovara.corebackend.user.model.User;
import com.finovara.corebackend.usersetting.notificationemail.model.NotificationEmailSettings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NotificationEmailSenderTest {
    private NotificationEmailSender sender;
    private User user;

    @BeforeEach
    void setUp() {
        NotificationEmailSettings settings = new NotificationEmailSettings();
        user = new User();
        user.setNotificationEmailSettings(settings);
        sender = new NotificationEmailSender();
    }

    @Test
    void shouldExecuteActionIfConditionTrue() {
        Function<NotificationEmailSettings, Boolean> condition = settings -> true;

        AtomicBoolean actionExecuted = new AtomicBoolean(false);
        Consumer<User> action = user1 -> actionExecuted.set(true);

        sender.sendIfEnabled(user, condition, action);

        assertTrue(actionExecuted.get());
    }

    @Test
    void shouldNotExecuteActionIfConditionFalse() {
        Function<NotificationEmailSettings, Boolean> condition = s -> false;

        AtomicBoolean actionExecuted = new AtomicBoolean(false);
        Consumer<User> action = user1 -> actionExecuted.set(true);

        sender.sendIfEnabled(user, condition, action);

        assertFalse(actionExecuted.get());
    }

}