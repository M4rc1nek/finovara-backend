package com.finovara.notificationservice.notificationemail.service.digest.report.security.processor;

import com.finovara.contracts.authorization.dto.UserDataResponse;
import com.finovara.contracts.notification.email.digest.report.security.WeeklySecurityDigestReportDto;
import com.finovara.notificationservice.feignclient.ActivityLogBackendClient;
import com.finovara.notificationservice.feignclient.AuthBackendClient;
import com.finovara.notificationservice.notificationemail.model.ScheduledEmailNotificationType;
import com.finovara.notificationservice.notificationemail.service.EmailNotifier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class WeeklySecurityDigestReportEmailProcessor {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final String NOT_AVAILABLE = "—";

    private final AuthBackendClient authBackendClient;
    private final ActivityLogBackendClient activityLogBackendClient;
    private final EmailNotifier emailNotifier;

    public void sendWeeklySecurityDigestEmail() {
        List<WeeklySecurityDigestReportDto> reports = activityLogBackendClient.getSecurityDigestReport();
        reports.forEach(this::sendForUser);
    }

    private void sendForUser(WeeklySecurityDigestReportDto report) {
        UserDataResponse user = authBackendClient.getUserEmailData(report.userId());
        if (user.email().isEmpty()) {
            log.warn("Skipping security digest email - no email found for userId={}", report.userId());
            return;
        }

        Map<String, String> placeholders = buildPlaceholders(report, user);
        emailNotifier.send(ScheduledEmailNotificationType.WEEKLY_SECURITY_DIGEST_REPORT_EMAIL, user.email().get(), placeholders);
    }

    private Map<String, String> buildPlaceholders(WeeklySecurityDigestReportDto report, UserDataResponse user) {
        return Map.ofEntries(
                Map.entry("userName", user.username().orElse("Użytkowniku")),
                Map.entry("weekStart", formatDate(report.weekStart())),
                Map.entry("weekEnd", formatDate(report.weekEnd())),
                Map.entry("successfulLogins", String.valueOf(report.successfulLogins())),
                Map.entry("failedLogins", String.valueOf(report.failedLogins())),
                Map.entry("ipAddresses", formatList(report.ipAddresses())),
                Map.entry("locations", formatList(report.locations())),
                Map.entry("browsers", formatList(report.browsers())),
                Map.entry("passwordChanges", String.valueOf(report.passwordChanges())),
                Map.entry("lastPasswordChangeDate", formatDate(report.lastPasswordChangeDate())),
                Map.entry("emailChanges", String.valueOf(report.emailChanges())),
                Map.entry("lastEmailChangeDate", formatDate(report.lastEmailChangeDate())),
                Map.entry("usernameChanges", String.valueOf(report.usernameChanges())),
                Map.entry("lastUsernameChangeDate", formatDate(report.lastUsernameChangeDate())),
                Map.entry("profileImageChanges", String.valueOf(report.profileImageChanges())),
                Map.entry("lastProfileImageChangeDate", formatDate(report.lastProfileImageChangeDate())),
                Map.entry("profileImageDeleted", String.valueOf(report.profileImageDeleted())),
                Map.entry("lastProfileImageDeleteDate", formatDate(report.lastProfileImageDeleteDate()))

        );
    }


    private String formatList(List<String> values) {
        if (values == null || values.isEmpty()) {
            return NOT_AVAILABLE;
        }
        return String.join(", ", values);
    }

    private String formatDate(LocalDateTime value) {
        return Optional.ofNullable(value).map(DATE_FORMATTER::format).orElse(NOT_AVAILABLE);
    }
}