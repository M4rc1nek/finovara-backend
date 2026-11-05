package com.finovara.notificationservice.notificationemail.service.login;

import com.finovara.contracts.mainaccount.activity.event.secure.login.activity.LoginActivityEvent;
import com.finovara.contracts.mainaccount.activity.model.LoginActivityStatus;
import com.finovara.contracts.user.authorization.dto.UserDataResponse;
import com.finovara.notificationservice.feignclient.AuthBackendClient;
import com.finovara.notificationservice.notificationemail.model.ActionEmailNotificationType;
import com.finovara.notificationservice.notificationemail.model.NotificationEmailSettings;
import com.finovara.notificationservice.notificationemail.settings.repository.NotificationEmailSettingsRepository;
import com.finovara.notificationservice.notificationemail.util.EmailNotifier;
import com.finovara.notificationservice.util.time.PolishTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import java.time.format.DateTimeFormatter;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class NewLoginNotificationHandler {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");
    private final NotificationEmailSettingsRepository notificationEmailSettingsRepository;
    private final AuthBackendClient authBackendClient;
    private final EmailNotifier emailNotifier;

    public void handle(LoginActivityEvent event) {
        if (event.status() != LoginActivityStatus.SUCCESSFUL) {
            return;
        }

        notificationEmailSettingsRepository.findByUserId(event.userId())
                .filter(NotificationEmailSettings::isNotifyOnNewLogin)
                .ifPresent(settings -> sendNotification(event));
    }

    private void sendNotification(LoginActivityEvent event) {
        UserDataResponse userData = authBackendClient.getUserData(event.userId());
        if (userData.email().isEmpty()) {
            log.warn("Skipping new login notification - no email found for userId={}", event.userId());
            return;
        }

        Map<String, String> placeholders = Map.of(
                "username", escape(userData.username().orElse("Użytkowniku")),
                "ipAddress", escape(valueOrDefault(event.ipAddress(), "Nieznany")),
                "location", escape(valueOrDefault(event.location(), "Nieznana lokalizacja")),
                "date", event.occurredAt() == null
                        ? "Nieznana data"
                        : PolishTime.formatUtcAsPolish(event.occurredAt(), DATE_FORMATTER)
        );

        emailNotifier.send(ActionEmailNotificationType.NEW_LOGIN, userData.email().get(), placeholders);
        log.info("Sent new login notification for userId={}", event.userId());
    }

    private String valueOrDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private String escape(String value) {
        return HtmlUtils.htmlEscape(value);
    }
}
