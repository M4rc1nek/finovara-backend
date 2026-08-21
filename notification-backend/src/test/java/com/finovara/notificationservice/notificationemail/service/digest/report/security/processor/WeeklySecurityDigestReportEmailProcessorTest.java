package com.finovara.notificationservice.notificationemail.service.digest.report.security.processor;

import com.finovara.contracts.authorization.dto.UserDataResponse;
import com.finovara.contracts.notification.email.digest.report.security.WeeklySecurityDigestReportDto;
import com.finovara.notificationservice.feignclient.ActivityLogBackendClient;
import com.finovara.notificationservice.feignclient.AuthBackendClient;
import com.finovara.notificationservice.notificationemail.model.ScheduledEmailNotificationType;
import com.finovara.notificationservice.notificationemail.service.EmailNotifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WeeklySecurityDigestReportEmailProcessorTest {

    private static final Long USER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;
    private static final String USER_EMAIL = "user@finovara.com";

    @Mock
    private AuthBackendClient authBackendClient;

    @Mock
    private ActivityLogBackendClient activityLogBackendClient;

    @Mock
    private EmailNotifier emailNotifier;

    @Mock
    private WeeklySecurityDigestReportDto report;

    @Mock
    private WeeklySecurityDigestReportDto otherReport;

    @Mock
    private UserDataResponse userDataResponse;

    @Mock
    private UserDataResponse otherUserDataResponse;

    @InjectMocks
    private WeeklySecurityDigestReportEmailProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new WeeklySecurityDigestReportEmailProcessor(authBackendClient, activityLogBackendClient, emailNotifier);
    }

    private void stubFullReport(WeeklySecurityDigestReportDto reportMock, Long userId) {
        when(reportMock.userId()).thenReturn(userId);
        when(reportMock.weekStart()).thenReturn(LocalDateTime.of(2026, 8, 10, 0, 0));
        when(reportMock.weekEnd()).thenReturn(LocalDateTime.of(2026, 8, 17, 0, 0));
        when(reportMock.successfulLogins()).thenReturn(5L);
        when(reportMock.failedLogins()).thenReturn(2L);
        when(reportMock.ipAddresses()).thenReturn(List.of("1.1.1.1", "2.2.2.2"));
        when(reportMock.locations()).thenReturn(List.of("Warsaw"));
        when(reportMock.browsers()).thenReturn(List.of("Chrome"));
        when(reportMock.passwordChanges()).thenReturn(1L);
        when(reportMock.lastPasswordChangeDate()).thenReturn(LocalDateTime.of(2026, 8, 11, 10, 0));
        when(reportMock.emailChanges()).thenReturn(1L);
        when(reportMock.lastEmailChangeDate()).thenReturn(LocalDateTime.of(2026, 8, 12, 10, 0));
        when(reportMock.usernameChanges()).thenReturn(1L);
        when(reportMock.lastUsernameChangeDate()).thenReturn(LocalDateTime.of(2026, 8, 13, 10, 0));
        when(reportMock.profileImageChanges()).thenReturn(1L);
        when(reportMock.lastProfileImageChangeDate()).thenReturn(LocalDateTime.of(2026, 8, 14, 10, 0));
        when(reportMock.profileImageDeleted()).thenReturn(1L);
        when(reportMock.lastProfileImageDeleteDate()).thenReturn(LocalDateTime.of(2026, 8, 15, 10, 0));
    }

    @Nested
    class SendWeeklySecurityDigestEmail {

        @Test
        void shouldNotCallEmailNotifierWhenNoReportsExist() {
            when(activityLogBackendClient.getSecurityDigestReport()).thenReturn(List.of());

            processor.sendWeeklySecurityDigestEmail();

            verifyNoInteractions(emailNotifier);
        }

        @Test
        void shouldSendEmailWhenUserHasEmail() {
            stubFullReport(report, USER_ID);
            when(activityLogBackendClient.getSecurityDigestReport()).thenReturn(List.of(report));
            when(authBackendClient.getUserEmailData(USER_ID)).thenReturn(userDataResponse);
            when(userDataResponse.email()).thenReturn(Optional.of(USER_EMAIL));
            when(userDataResponse.username()).thenReturn(Optional.of("john"));

            processor.sendWeeklySecurityDigestEmail();

            verify(emailNotifier).send(eq(ScheduledEmailNotificationType.WEEKLY_SECURITY_DIGEST_REPORT_EMAIL), eq(USER_EMAIL), anyMap());
        }

        @Test
        void shouldUseDefaultUsernameWhenUsernameIsEmpty() {
            stubFullReport(report, USER_ID);
            when(activityLogBackendClient.getSecurityDigestReport()).thenReturn(List.of(report));
            when(authBackendClient.getUserEmailData(USER_ID)).thenReturn(userDataResponse);
            when(userDataResponse.email()).thenReturn(Optional.of(USER_EMAIL));
            when(userDataResponse.username()).thenReturn(Optional.empty());

            ArgumentCaptor<Map<String, String>> placeholdersCaptor = captor();
            processor.sendWeeklySecurityDigestEmail();

            verify(emailNotifier).send(eq(ScheduledEmailNotificationType.WEEKLY_SECURITY_DIGEST_REPORT_EMAIL), eq(USER_EMAIL), placeholdersCaptor.capture());
            assertEquals("Użytkowniku", placeholdersCaptor.getValue().get("userName"));
        }

        @Test
        void shouldFormatDatesUsingConfiguredPatternWhenDatesArePresent() {
            stubFullReport(report, USER_ID);
            when(activityLogBackendClient.getSecurityDigestReport()).thenReturn(List.of(report));
            when(authBackendClient.getUserEmailData(USER_ID)).thenReturn(userDataResponse);
            when(userDataResponse.email()).thenReturn(Optional.of(USER_EMAIL));
            when(userDataResponse.username()).thenReturn(Optional.of("john"));

            ArgumentCaptor<Map<String, String>> placeholdersCaptor = captor();
            processor.sendWeeklySecurityDigestEmail();

            verify(emailNotifier).send(eq(ScheduledEmailNotificationType.WEEKLY_SECURITY_DIGEST_REPORT_EMAIL), eq(USER_EMAIL), placeholdersCaptor.capture());
            Map<String, String> placeholders = placeholdersCaptor.getValue();
            assertEquals("10.08.2026", placeholders.get("weekStart"));
            assertEquals("17.08.2026", placeholders.get("weekEnd"));
            assertEquals("11.08.2026", placeholders.get("lastPasswordChangeDate"));
        }

        @Test
        void shouldUseDashPlaceholderWhenDateIsNull() {
            stubFullReport(report, USER_ID);
            when(report.lastEmailChangeDate()).thenReturn(null);
            when(activityLogBackendClient.getSecurityDigestReport()).thenReturn(List.of(report));
            when(authBackendClient.getUserEmailData(USER_ID)).thenReturn(userDataResponse);
            when(userDataResponse.email()).thenReturn(Optional.of(USER_EMAIL));
            when(userDataResponse.username()).thenReturn(Optional.of("john"));

            ArgumentCaptor<Map<String, String>> placeholdersCaptor = captor();
            processor.sendWeeklySecurityDigestEmail();

            verify(emailNotifier).send(eq(ScheduledEmailNotificationType.WEEKLY_SECURITY_DIGEST_REPORT_EMAIL), eq(USER_EMAIL), placeholdersCaptor.capture());
            assertEquals("—", placeholdersCaptor.getValue().get("lastEmailChangeDate"));
        }

        @Test
        void shouldUseDashPlaceholderWhenListIsEmpty() {
            stubFullReport(report, USER_ID);
            when(report.locations()).thenReturn(List.of());
            when(activityLogBackendClient.getSecurityDigestReport()).thenReturn(List.of(report));
            when(authBackendClient.getUserEmailData(USER_ID)).thenReturn(userDataResponse);
            when(userDataResponse.email()).thenReturn(Optional.of(USER_EMAIL));
            when(userDataResponse.username()).thenReturn(Optional.of("john"));

            ArgumentCaptor<Map<String, String>> placeholdersCaptor = captor();
            processor.sendWeeklySecurityDigestEmail();

            verify(emailNotifier).send(eq(ScheduledEmailNotificationType.WEEKLY_SECURITY_DIGEST_REPORT_EMAIL), eq(USER_EMAIL), placeholdersCaptor.capture());
            assertEquals("—", placeholdersCaptor.getValue().get("locations"));
        }

        @Test
        void shouldJoinListValuesWithCommaWhenListHasMultipleElements() {
            stubFullReport(report, USER_ID);
            when(activityLogBackendClient.getSecurityDigestReport()).thenReturn(List.of(report));
            when(authBackendClient.getUserEmailData(USER_ID)).thenReturn(userDataResponse);
            when(userDataResponse.email()).thenReturn(Optional.of(USER_EMAIL));
            when(userDataResponse.username()).thenReturn(Optional.of("john"));

            ArgumentCaptor<Map<String, String>> placeholdersCaptor = captor();
            processor.sendWeeklySecurityDigestEmail();

            verify(emailNotifier).send(eq(ScheduledEmailNotificationType.WEEKLY_SECURITY_DIGEST_REPORT_EMAIL), eq(USER_EMAIL), placeholdersCaptor.capture());
            assertEquals("1.1.1.1, 2.2.2.2", placeholdersCaptor.getValue().get("ipAddresses"));
        }

        @Test
        void shouldIncludeNumericCountsAsStringsInPlaceholders() {
            stubFullReport(report, USER_ID);
            when(activityLogBackendClient.getSecurityDigestReport()).thenReturn(List.of(report));
            when(authBackendClient.getUserEmailData(USER_ID)).thenReturn(userDataResponse);
            when(userDataResponse.email()).thenReturn(Optional.of(USER_EMAIL));
            when(userDataResponse.username()).thenReturn(Optional.of("john"));

            ArgumentCaptor<Map<String, String>> placeholdersCaptor = captor();
            processor.sendWeeklySecurityDigestEmail();

            verify(emailNotifier).send(eq(ScheduledEmailNotificationType.WEEKLY_SECURITY_DIGEST_REPORT_EMAIL), eq(USER_EMAIL), placeholdersCaptor.capture());
            Map<String, String> placeholders = placeholdersCaptor.getValue();
            assertEquals("5", placeholders.get("successfulLogins"));
            assertEquals("2", placeholders.get("failedLogins"));
            assertEquals("1", placeholders.get("passwordChanges"));
        }

        @Test
        void shouldCallAuthBackendClientOnceForEachReportWhenMultipleReportsExist() {
            stubFullReport(report, USER_ID);
            stubFullReport(otherReport, OTHER_USER_ID);
            when(activityLogBackendClient.getSecurityDigestReport()).thenReturn(List.of(report, otherReport));
            when(authBackendClient.getUserEmailData(USER_ID)).thenReturn(userDataResponse);
            when(userDataResponse.email()).thenReturn(Optional.of(USER_EMAIL));
            when(userDataResponse.username()).thenReturn(Optional.of("john"));
            when(authBackendClient.getUserEmailData(OTHER_USER_ID)).thenReturn(otherUserDataResponse);
            when(otherUserDataResponse.email()).thenReturn(Optional.of("other@finovara.com"));
            when(otherUserDataResponse.username()).thenReturn(Optional.of("jane"));

            processor.sendWeeklySecurityDigestEmail();

            verify(authBackendClient, times(1)).getUserEmailData(USER_ID);
            verify(authBackendClient, times(1)).getUserEmailData(OTHER_USER_ID);
            verify(emailNotifier, times(2)).send(eq(ScheduledEmailNotificationType.WEEKLY_SECURITY_DIGEST_REPORT_EMAIL), anyString(), anyMap());
        }
    }

    @SuppressWarnings("unchecked")
    private ArgumentCaptor<Map<String, String>> captor() {
        return ArgumentCaptor.forClass(Map.class);
    }
}