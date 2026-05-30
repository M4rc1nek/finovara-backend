package com.finovara.corebackend.usersetting.notificationemail.util;

import com.finovara.corebackend.user.model.User;
import com.finovara.corebackend.usersetting.notificationemail.model.NotificationEmailSettings;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;
import java.util.function.Function;

@Component
public class NotificationEmailSender {

    public void sendIfEnabled(
            User user,
            Function<NotificationEmailSettings, Boolean> condition,
            Consumer<User> action
    ) {
        NotificationEmailSettings settings = user.getNotificationEmailSettings();

        if (!Boolean.TRUE.equals(condition.apply(settings))) return;

        action.accept(user);
    }
}
