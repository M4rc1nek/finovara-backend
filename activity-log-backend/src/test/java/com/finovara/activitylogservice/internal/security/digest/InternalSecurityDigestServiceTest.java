package com.finovara.activitylogservice.internal.security.digest;

import com.finovara.activitylogservice.feignclient.AuthBackendClient;
import com.finovara.activitylogservice.internal.security.digest.dto.AccountChangesDigestSummaryDto;
import com.finovara.activitylogservice.internal.security.digest.dto.LoginDigestSummaryDto;
import com.finovara.activitylogservice.internal.security.digest.mapper.WeeklySecurityDigestReportMapper;
import com.finovara.activitylogservice.internal.security.digest.service.SecuritySummaryService;
import com.finovara.contracts.notification.email.digest.report.security.WeeklySecurityDigestReportDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InternalSecurityDigestServiceTest {

    private static final Long USER_ID_1 = 1L;
    private static final Long USER_ID_2 = 2L;

    @Mock
    private SecuritySummaryService securitySummaryService;

    @Mock
    private WeeklySecurityDigestReportMapper weeklySecurityDigestReportMapper;

    @Mock
    private AuthBackendClient authBackendClient;

    @Mock
    private LoginDigestSummaryDto loginDigestSummaryDto;

    @Mock
    private AccountChangesDigestSummaryDto accountChangesDigestSummaryDto;

    @Mock
    private WeeklySecurityDigestReportDto weeklySecurityDigestReportDto;

    private InternalSecurityDigestService internalSecurityDigestService;

    @BeforeEach
    void setUp() {
        internalSecurityDigestService = new InternalSecurityDigestService(securitySummaryService, weeklySecurityDigestReportMapper, authBackendClient);
    }

    @Nested
    class GetSecurityDigestReport {

        @Test
        void shouldReturnEmptyListWhenNoUsersExist() {
            when(authBackendClient.getAllUserIds()).thenReturn(List.of());

            List<WeeklySecurityDigestReportDto> result = internalSecurityDigestService.getSecurityDigestReport();

            assertTrue(result.isEmpty());
            verifyNoInteractions(securitySummaryService);
            verifyNoInteractions(weeklySecurityDigestReportMapper);
        }

        @Test
        void shouldReturnSingleReportWhenSingleUserExists() {
            when(authBackendClient.getAllUserIds()).thenReturn(List.of(USER_ID_1));
            when(securitySummaryService.calculateLoginSummary(eq(USER_ID_1), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(loginDigestSummaryDto);
            when(securitySummaryService.calculateAccountChangesSummary(eq(USER_ID_1), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(accountChangesDigestSummaryDto);
            when(weeklySecurityDigestReportMapper.toDto(eq(USER_ID_1), any(LocalDateTime.class), any(LocalDateTime.class), eq(loginDigestSummaryDto), eq(accountChangesDigestSummaryDto))).thenReturn(weeklySecurityDigestReportDto);

            List<WeeklySecurityDigestReportDto> result = internalSecurityDigestService.getSecurityDigestReport();

            assertEquals(1, result.size());
            assertEquals(weeklySecurityDigestReportDto, result.getFirst());
        }

        @Test
        void shouldReturnReportForEachUserWhenMultipleUsersExist() {
            when(authBackendClient.getAllUserIds()).thenReturn(List.of(USER_ID_1, USER_ID_2));
            when(securitySummaryService.calculateLoginSummary(any(Long.class), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(loginDigestSummaryDto);
            when(securitySummaryService.calculateAccountChangesSummary(any(Long.class), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(accountChangesDigestSummaryDto);
            when(weeklySecurityDigestReportMapper.toDto(any(Long.class), any(LocalDateTime.class), any(LocalDateTime.class), eq(loginDigestSummaryDto), eq(accountChangesDigestSummaryDto))).thenReturn(weeklySecurityDigestReportDto);

            List<WeeklySecurityDigestReportDto> result = internalSecurityDigestService.getSecurityDigestReport();

            assertEquals(2, result.size());
            verify(securitySummaryService, times(2)).calculateLoginSummary(any(Long.class), any(LocalDateTime.class), any(LocalDateTime.class));
            verify(securitySummaryService, times(2)).calculateAccountChangesSummary(any(Long.class), any(LocalDateTime.class), any(LocalDateTime.class));
            verify(weeklySecurityDigestReportMapper, times(2)).toDto(any(Long.class), any(LocalDateTime.class), any(LocalDateTime.class), eq(loginDigestSummaryDto), eq(accountChangesDigestSummaryDto));
        }

        @Test
        void shouldUseSixDayPeriodEndingTomorrowWhenCalculatingDateRange() {
            when(authBackendClient.getAllUserIds()).thenReturn(List.of(USER_ID_1));
            when(securitySummaryService.calculateLoginSummary(eq(USER_ID_1), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(loginDigestSummaryDto);
            when(securitySummaryService.calculateAccountChangesSummary(eq(USER_ID_1), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(accountChangesDigestSummaryDto);
            when(weeklySecurityDigestReportMapper.toDto(eq(USER_ID_1), any(LocalDateTime.class), any(LocalDateTime.class), eq(loginDigestSummaryDto), eq(accountChangesDigestSummaryDto))).thenReturn(weeklySecurityDigestReportDto);

            internalSecurityDigestService.getSecurityDigestReport();

            ArgumentCaptor<LocalDateTime> fromCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
            ArgumentCaptor<LocalDateTime> toCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
            verify(securitySummaryService).calculateLoginSummary(eq(USER_ID_1), fromCaptor.capture(), toCaptor.capture());

            LocalDate today = LocalDate.now();
            assertEquals(today.minusDays(6).atStartOfDay(), fromCaptor.getValue());
            assertEquals(today.plusDays(1).atStartOfDay(), toCaptor.getValue());
        }

        @Test
        void shouldPassLoginAndAccountChangesSummariesToMapperWhenBuildingReport() {
            when(authBackendClient.getAllUserIds()).thenReturn(List.of(USER_ID_1));
            when(securitySummaryService.calculateLoginSummary(eq(USER_ID_1), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(loginDigestSummaryDto);
            when(securitySummaryService.calculateAccountChangesSummary(eq(USER_ID_1), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(accountChangesDigestSummaryDto);
            when(weeklySecurityDigestReportMapper.toDto(eq(USER_ID_1), any(LocalDateTime.class), any(LocalDateTime.class), eq(loginDigestSummaryDto), eq(accountChangesDigestSummaryDto))).thenReturn(weeklySecurityDigestReportDto);

            internalSecurityDigestService.getSecurityDigestReport();

            verify(weeklySecurityDigestReportMapper).toDto(eq(USER_ID_1), any(LocalDateTime.class), any(LocalDateTime.class), eq(loginDigestSummaryDto), eq(accountChangesDigestSummaryDto));
        }

        @Test
        void shouldPreserveUserIdsOrderWhenBuildingReports() {
            when(authBackendClient.getAllUserIds()).thenReturn(List.of(USER_ID_2, USER_ID_1));
            when(securitySummaryService.calculateLoginSummary(any(Long.class), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(loginDigestSummaryDto);
            when(securitySummaryService.calculateAccountChangesSummary(any(Long.class), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(accountChangesDigestSummaryDto);
            when(weeklySecurityDigestReportMapper.toDto(eq(USER_ID_2), any(LocalDateTime.class), any(LocalDateTime.class), eq(loginDigestSummaryDto), eq(accountChangesDigestSummaryDto))).thenReturn(weeklySecurityDigestReportDto);
            when(weeklySecurityDigestReportMapper.toDto(eq(USER_ID_1), any(LocalDateTime.class), any(LocalDateTime.class), eq(loginDigestSummaryDto), eq(accountChangesDigestSummaryDto))).thenReturn(weeklySecurityDigestReportDto);

            internalSecurityDigestService.getSecurityDigestReport();

            ArgumentCaptor<Long> userIdCaptor = ArgumentCaptor.forClass(Long.class);
            verify(securitySummaryService, times(2)).calculateLoginSummary(userIdCaptor.capture(), any(LocalDateTime.class), any(LocalDateTime.class));
            assertEquals(List.of(USER_ID_2, USER_ID_1), userIdCaptor.getAllValues());
        }
    }
}