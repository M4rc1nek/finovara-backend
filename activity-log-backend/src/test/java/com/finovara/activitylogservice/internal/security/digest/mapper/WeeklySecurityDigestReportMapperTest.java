package com.finovara.activitylogservice.internal.security.digest.mapper;

import com.finovara.activitylogservice.internal.security.digest.dto.AccountChangesDigestSummaryDto;
import com.finovara.activitylogservice.internal.security.digest.dto.LoginDigestSummaryDto;
import com.finovara.contracts.notification.email.digest.report.security.WeeklySecurityDigestReportDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class WeeklySecurityDigestReportMapperTest {

    private static final Long USER_ID = 1L;
    private static final LocalDateTime WEEK_START = LocalDateTime.of(2026, 8, 10, 0, 0);
    private static final LocalDateTime WEEK_END = LocalDateTime.of(2026, 8, 17, 0, 0);

    private WeeklySecurityDigestReportMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new WeeklySecurityDigestReportMapper();
    }

    @Nested
    class ToDto {

        @Test
        void shouldMapAllFieldsWhenSummariesAreFullyPopulated() {
            LoginDigestSummaryDto loginSummary = new LoginDigestSummaryDto(5L, 2L, List.of("1.1.1.1"), List.of("Warsaw"), List.of("Chrome"));
            AccountChangesDigestSummaryDto accountChangesSummary = new AccountChangesDigestSummaryDto(
                    1L, LocalDateTime.of(2026, 8, 11, 10, 0),
                    2L, LocalDateTime.of(2026, 8, 12, 10, 0),
                    3L, LocalDateTime.of(2026, 8, 13, 10, 0),
                    4L, LocalDateTime.of(2026, 8, 14, 10, 0),
                    5L, LocalDateTime.of(2026, 8, 15, 10, 0)
            );

            WeeklySecurityDigestReportDto result = mapper.toDto(USER_ID, WEEK_START, WEEK_END, loginSummary, accountChangesSummary);

            assertEquals(USER_ID, result.userId());
            assertEquals(WEEK_START, result.weekStart());
            assertEquals(WEEK_END, result.weekEnd());
            assertEquals(5L, result.successfulLogins());
            assertEquals(2L, result.failedLogins());
            assertEquals(List.of("1.1.1.1"), result.ipAddresses());
            assertEquals(List.of("Warsaw"), result.locations());
            assertEquals(List.of("Chrome"), result.browsers());
            assertEquals(1L, result.passwordChanges());
            assertEquals(LocalDateTime.of(2026, 8, 11, 10, 0), result.lastPasswordChangeDate());
            assertEquals(2L, result.emailChanges());
            assertEquals(LocalDateTime.of(2026, 8, 12, 10, 0), result.lastEmailChangeDate());
            assertEquals(3L, result.usernameChanges());
            assertEquals(LocalDateTime.of(2026, 8, 13, 10, 0), result.lastUsernameChangeDate());
            assertEquals(4L, result.profileImageChanges());
            assertEquals(LocalDateTime.of(2026, 8, 14, 10, 0), result.lastProfileImageChangeDate());
            assertEquals(5L, result.profileImageDeleted());
            assertEquals(LocalDateTime.of(2026, 8, 15, 10, 0), result.lastProfileImageDeleteDate());
        }

        @Test
        void shouldMapEmptyListsAndNullDatesWhenSummariesHaveNoActivity() {
            LoginDigestSummaryDto loginSummary = new LoginDigestSummaryDto(0L, 0L, List.of(), List.of(), List.of());
            AccountChangesDigestSummaryDto accountChangesSummary = new AccountChangesDigestSummaryDto(
                    0L, null, 0L, null, 0L, null, 0L, null, 0L, null
            );

            WeeklySecurityDigestReportDto result = mapper.toDto(USER_ID, WEEK_START, WEEK_END, loginSummary, accountChangesSummary);

            assertEquals(List.of(), result.ipAddresses());
            assertEquals(List.of(), result.locations());
            assertEquals(List.of(), result.browsers());
            assertNull(result.lastPasswordChangeDate());
            assertNull(result.lastEmailChangeDate());
            assertNull(result.lastUsernameChangeDate());
            assertNull(result.lastProfileImageChangeDate());
            assertNull(result.lastProfileImageDeleteDate());
        }

        @Test
        void shouldMapNullUserIdWhenUserIdIsNull() {
            LoginDigestSummaryDto loginSummary = new LoginDigestSummaryDto(0L, 0L, List.of(), List.of(), List.of());
            AccountChangesDigestSummaryDto accountChangesSummary = new AccountChangesDigestSummaryDto(
                    0L, null, 0L, null, 0L, null, 0L, null, 0L, null
            );

            WeeklySecurityDigestReportDto result = mapper.toDto(null, WEEK_START, WEEK_END, loginSummary, accountChangesSummary);

            assertNull(result.userId());
        }

        @Test
        void shouldPreserveWeekStartAndWeekEndWhenMapping() {
            LoginDigestSummaryDto loginSummary = new LoginDigestSummaryDto(0L, 0L, List.of(), List.of(), List.of());
            AccountChangesDigestSummaryDto accountChangesSummary = new AccountChangesDigestSummaryDto(
                    0L, null, 0L, null, 0L, null, 0L, null, 0L, null
            );

            WeeklySecurityDigestReportDto result = mapper.toDto(USER_ID, WEEK_START, WEEK_END, loginSummary, accountChangesSummary);

            assertEquals(WEEK_START, result.weekStart());
            assertEquals(WEEK_END, result.weekEnd());
        }
    }
}