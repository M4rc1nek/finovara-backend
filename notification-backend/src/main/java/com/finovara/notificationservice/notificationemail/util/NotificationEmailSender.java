package com.finovara.notificationservice.notificationemail.util;

import com.finovara.notificationservice.notificationemail.dto.UserEmailDataDto;
import com.finovara.notificationservice.notificationemail.model.NotificationEmailSettings;
import com.finovara.notificationservice.notificationemail.repository.NotificationEmailSettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.function.BiConsumer;
import java.util.function.Function;

@Component
@RequiredArgsConstructor
public class NotificationEmailSender {
    
    private final NotificationEmailSettingsRepository notificationEmailSettingsRepository;

    public void sendIfEnabled(Long userId, UserEmailDataDto userEmailData, Function<NotificationEmailSettings, Boolean> condition, BiConsumer<Long, UserEmailDataDto> action) {
        NotificationEmailSettings settings = notificationEmailSettingsRepository.findByUserId(userId)
                .orElse(null);

        if (settings == null || !Boolean.TRUE.equals(condition.apply(settings))) return;

        action.accept(userId, userEmailData);
    }
}
