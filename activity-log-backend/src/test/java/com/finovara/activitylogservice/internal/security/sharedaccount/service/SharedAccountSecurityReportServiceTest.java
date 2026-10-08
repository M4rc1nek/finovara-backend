package com.finovara.activitylogservice.internal.security.sharedaccount.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.finovara.activitylogservice.activitylog.sharedaccount.repository.SharedAccountActivityLogRepository;
import com.finovara.activitylogservice.feignclient.AuthBackendClient;
import com.finovara.activitylogservice.internal.security.util.clientinfo.ClientInfoResolver;
import com.finovara.activitylogservice.internal.security.util.clientinfo.dto.ClientInfoDto;
import com.finovara.contracts.sharedaccount.SharedAccountActivityLogType;
import com.finovara.contracts.sharedaccount.SharedAccountMemberInfoDto;
import com.finovara.contracts.sharedaccount.report.security.dto.SharedAccountSecurityOverviewDto;
import com.finovara.contracts.sharedaccount.report.security.dto.SharedAccountSecurityReportDto;
import com.finovara.contracts.util.PeriodType;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SharedAccountSecurityReportServiceTest {

    private static final Long CALLER_ID = 1L;
    private static final Long OTHER_ID = 2L;
    private static final Long THIRD_ID = 3L;

    @Mock
    private ClientInfoResolver clientInfoResolver;

    @Mock
    private AuthBackendClient authBackendClient;

    @Mock
    private SharedAccountActivityLogRepository sharedAccountActivityLogRepository;

    @Mock
    private ClientInfoDto clientInfo;

    @InjectMocks
    private SharedAccountSecurityReportService service;

    private PeriodType periodType;

    @BeforeEach
    void setUp() {
        periodType = PeriodType.values()[0];
    }

    private SharedAccountMemberInfoDto createMember(Long userId) {
        SharedAccountMemberInfoDto member = mock(SharedAccountMemberInfoDto.class);
        when(member.userId()).thenReturn(userId);
        return member;
    }

    @Nested
    class GetSecurityOverview {

        @Test
        void shouldReturnEmptyOverviewWhenNoMembersExist() {
            when(authBackendClient.getSharedAccountMembers(CALLER_ID)).thenReturn(List.of());

            SharedAccountSecurityOverviewDto result = service.getSecurityOverview(CALLER_ID, periodType);

            assertTrue(result.members().isEmpty());
            verifyNoInteractions(clientInfoResolver, sharedAccountActivityLogRepository);
        }

        @Test
        void shouldReturnReportWithMemberDataWhenSingleMemberExists() {
            SharedAccountMemberInfoDto member = createMember(CALLER_ID);
            when(member.username()).thenReturn("john");
            when(authBackendClient.getSharedAccountMembers(CALLER_ID)).thenReturn(List.of(member));
            when(clientInfoResolver.getClientContext(eq(CALLER_ID), any(LocalDateTime.class), any(LocalDateTime.class)))
                    .thenReturn(clientInfo);

            SharedAccountSecurityOverviewDto result = service.getSecurityOverview(CALLER_ID, periodType);

            assertEquals(1, result.members().size());
            SharedAccountSecurityReportDto report = result.members().get(0);
            assertEquals(CALLER_ID, report.userId());
            assertEquals("john", report.username());
            assertEquals(member.role(), report.role());
            assertEquals(clientInfo.locationShares(), report.locationShares());
            assertEquals(clientInfo.browserShares(), report.browserShares());
        }

        @Test
        void shouldReturnActivityCountsAndDatesWhenMemberHasActivities() {
            LocalDateTime expenseDate = LocalDateTime.of(2026, 1, 10, 10, 0);
            LocalDateTime revenueDate = LocalDateTime.of(2026, 1, 11, 11, 0);
            LocalDateTime piggyBankDate = LocalDateTime.of(2026, 1, 12, 12, 0);
            LocalDateTime lastActiveAt = LocalDateTime.of(2026, 1, 13, 13, 0);
            SharedAccountMemberInfoDto member = createMember(CALLER_ID);
            when(authBackendClient.getSharedAccountMembers(CALLER_ID)).thenReturn(List.of(member));
            when(clientInfoResolver.getClientContext(eq(CALLER_ID), any(LocalDateTime.class), any(LocalDateTime.class)))
                    .thenReturn(clientInfo);
            when(sharedAccountActivityLogRepository.countByUserIdAndActivityType(
                    eq(CALLER_ID), eq(SharedAccountActivityLogType.EXPENSE_CREATED), any(LocalDateTime.class), any(LocalDateTime.class)))
                    .thenReturn(3);
            when(sharedAccountActivityLogRepository.countByUserIdAndActivityType(
                    eq(CALLER_ID), eq(SharedAccountActivityLogType.REVENUE_CREATED), any(LocalDateTime.class), any(LocalDateTime.class)))
                    .thenReturn(5);
            when(sharedAccountActivityLogRepository.countByUserIdAndActivityType(
                    eq(CALLER_ID), eq(SharedAccountActivityLogType.PIGGY_BANK_DEPOSIT), any(LocalDateTime.class), any(LocalDateTime.class)))
                    .thenReturn(7);
            when(sharedAccountActivityLogRepository.findLastActivityDateByUserIdAndActivityType(
                    CALLER_ID, SharedAccountActivityLogType.EXPENSE_CREATED)).thenReturn(expenseDate);
            when(sharedAccountActivityLogRepository.findLastActivityDateByUserIdAndActivityType(
                    CALLER_ID, SharedAccountActivityLogType.REVENUE_CREATED)).thenReturn(revenueDate);
            when(sharedAccountActivityLogRepository.findLastActivityDateByUserIdAndActivityType(
                    CALLER_ID, SharedAccountActivityLogType.PIGGY_BANK_DEPOSIT)).thenReturn(piggyBankDate);
            when(sharedAccountActivityLogRepository.findLastActivityDateByUserId(CALLER_ID)).thenReturn(lastActiveAt);

            SharedAccountSecurityReportDto report = service.getSecurityOverview(CALLER_ID, periodType).members().get(0);

            assertEquals(3, report.expensesCount());
            assertEquals(expenseDate, report.lastExpenseDate());
            assertEquals(5, report.revenuesCount());
            assertEquals(revenueDate, report.lastRevenueDate());
            assertEquals(7, report.piggyBankDepositsCount());
            assertEquals(piggyBankDate, report.lastPiggyBankDepositDate());
            assertEquals(lastActiveAt, report.lastActiveAt());
        }

        @Test
        void shouldReturnZeroCountsAndNullDatesWhenMemberHasNoActivities() {
            SharedAccountMemberInfoDto member = createMember(CALLER_ID);
            when(authBackendClient.getSharedAccountMembers(CALLER_ID)).thenReturn(List.of(member));
            when(clientInfoResolver.getClientContext(eq(CALLER_ID), any(LocalDateTime.class), any(LocalDateTime.class)))
                    .thenReturn(clientInfo);

            SharedAccountSecurityReportDto report = service.getSecurityOverview(CALLER_ID, periodType).members().get(0);

            assertEquals(0, report.expensesCount());
            assertEquals(0, report.revenuesCount());
            assertEquals(0, report.piggyBankDepositsCount());
            assertNull(report.lastExpenseDate());
            assertNull(report.lastRevenueDate());
            assertNull(report.lastPiggyBankDepositDate());
            assertNull(report.lastActiveAt());
        }

        @Test
        void shouldPlaceCallerFirstWhenCallerIsNotFirstMember() {
            SharedAccountMemberInfoDto other = createMember(OTHER_ID);
            SharedAccountMemberInfoDto caller = createMember(CALLER_ID);
            when(authBackendClient.getSharedAccountMembers(CALLER_ID)).thenReturn(List.of(other, caller));
            when(clientInfoResolver.getClientContext(any(Long.class), any(LocalDateTime.class), any(LocalDateTime.class)))
                    .thenReturn(clientInfo);

            List<SharedAccountSecurityReportDto> result = service.getSecurityOverview(CALLER_ID, periodType).members();

            assertEquals(List.of(CALLER_ID, OTHER_ID), result.stream().map(SharedAccountSecurityReportDto::userId).toList());
        }

        @Test
        void shouldKeepOrderOfOtherMembersWhenCallerIsPlacedFirst() {
            SharedAccountMemberInfoDto third = createMember(THIRD_ID);
            SharedAccountMemberInfoDto caller = createMember(CALLER_ID);
            SharedAccountMemberInfoDto other = createMember(OTHER_ID);
            when(authBackendClient.getSharedAccountMembers(CALLER_ID)).thenReturn(List.of(third, caller, other));
            when(clientInfoResolver.getClientContext(any(Long.class), any(LocalDateTime.class), any(LocalDateTime.class)))
                    .thenReturn(clientInfo);

            List<SharedAccountSecurityReportDto> result = service.getSecurityOverview(CALLER_ID, periodType).members();

            assertEquals(List.of(CALLER_ID, THIRD_ID, OTHER_ID), result.stream().map(SharedAccountSecurityReportDto::userId).toList());
        }

        @Test
        void shouldKeepOriginalOrderWhenCallerIsNotAmongMembers() {
            SharedAccountMemberInfoDto other = createMember(OTHER_ID);
            SharedAccountMemberInfoDto third = createMember(THIRD_ID);
            when(authBackendClient.getSharedAccountMembers(CALLER_ID)).thenReturn(List.of(other, third));
            when(clientInfoResolver.getClientContext(any(Long.class), any(LocalDateTime.class), any(LocalDateTime.class)))
                    .thenReturn(clientInfo);

            List<SharedAccountSecurityReportDto> result = service.getSecurityOverview(CALLER_ID, periodType).members();

            assertEquals(List.of(OTHER_ID, THIRD_ID), result.stream().map(SharedAccountSecurityReportDto::userId).toList());
        }

        @Test
        void shouldResolveClientContextForEachMemberWhenMultipleMembersExist() {
            SharedAccountMemberInfoDto caller = createMember(CALLER_ID);
            SharedAccountMemberInfoDto other = createMember(OTHER_ID);
            when(authBackendClient.getSharedAccountMembers(CALLER_ID)).thenReturn(List.of(caller, other));
            when(clientInfoResolver.getClientContext(any(Long.class), any(LocalDateTime.class), any(LocalDateTime.class)))
                    .thenReturn(clientInfo);

            service.getSecurityOverview(CALLER_ID, periodType);

            verify(clientInfoResolver).getClientContext(eq(CALLER_ID), any(LocalDateTime.class), any(LocalDateTime.class));
            verify(clientInfoResolver).getClientContext(eq(OTHER_ID), any(LocalDateTime.class), any(LocalDateTime.class));
            verify(sharedAccountActivityLogRepository).findLastActivityDateByUserId(CALLER_ID);
            verify(sharedAccountActivityLogRepository).findLastActivityDateByUserId(OTHER_ID);
        }

        @Test
        void shouldRequestLastActivityDatesWithoutPeriodWhenBuildingReport() {
            SharedAccountMemberInfoDto member = createMember(CALLER_ID);
            when(authBackendClient.getSharedAccountMembers(CALLER_ID)).thenReturn(List.of(member));
            when(clientInfoResolver.getClientContext(eq(CALLER_ID), any(LocalDateTime.class), any(LocalDateTime.class)))
                    .thenReturn(clientInfo);

            service.getSecurityOverview(CALLER_ID, periodType);

            verify(sharedAccountActivityLogRepository).findLastActivityDateByUserIdAndActivityType(
                    CALLER_ID, SharedAccountActivityLogType.EXPENSE_CREATED);
            verify(sharedAccountActivityLogRepository).findLastActivityDateByUserIdAndActivityType(
                    CALLER_ID, SharedAccountActivityLogType.REVENUE_CREATED);
            verify(sharedAccountActivityLogRepository).findLastActivityDateByUserIdAndActivityType(
                    CALLER_ID, SharedAccountActivityLogType.PIGGY_BANK_DEPOSIT);
            verify(sharedAccountActivityLogRepository).findLastActivityDateByUserId(CALLER_ID);
        }

        @Test
        void shouldNotQueryActivitiesOfOtherUsersWhenSingleMemberExists() {
            SharedAccountMemberInfoDto member = createMember(CALLER_ID);
            when(authBackendClient.getSharedAccountMembers(CALLER_ID)).thenReturn(List.of(member));
            when(clientInfoResolver.getClientContext(eq(CALLER_ID), any(LocalDateTime.class), any(LocalDateTime.class)))
                    .thenReturn(clientInfo);

            service.getSecurityOverview(CALLER_ID, periodType);

            verify(sharedAccountActivityLogRepository, never()).findLastActivityDateByUserId(OTHER_ID);
            verify(clientInfoResolver, never()).getClientContext(eq(OTHER_ID), any(LocalDateTime.class), any(LocalDateTime.class));
        }

        @ParameterizedTest
        @EnumSource(PeriodType.class)
        void shouldUseRangeFromPeriodStartToNowWhenPeriodTypeIsGiven(PeriodType type) {
            SharedAccountMemberInfoDto member = createMember(CALLER_ID);
            when(authBackendClient.getSharedAccountMembers(CALLER_ID)).thenReturn(List.of(member));
            when(clientInfoResolver.getClientContext(eq(CALLER_ID), any(LocalDateTime.class), any(LocalDateTime.class)))
                    .thenReturn(clientInfo);
            ArgumentCaptor<LocalDateTime> fromCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
            ArgumentCaptor<LocalDateTime> toCaptor = ArgumentCaptor.forClass(LocalDateTime.class);

            service.getSecurityOverview(CALLER_ID, type);

            verify(clientInfoResolver).getClientContext(eq(CALLER_ID), fromCaptor.capture(), toCaptor.capture());
            LocalDateTime expectedFrom = type.getStartDate(toCaptor.getValue().toLocalDate()).atStartOfDay();
            assertEquals(expectedFrom, fromCaptor.getValue());
            assertFalse(toCaptor.getValue().isAfter(LocalDateTime.now()));
        }

        @Test
        void shouldUseSamePeriodForClientContextAndCountsWhenBuildingReport() {
            SharedAccountMemberInfoDto member = createMember(CALLER_ID);
            when(authBackendClient.getSharedAccountMembers(CALLER_ID)).thenReturn(List.of(member));
            when(clientInfoResolver.getClientContext(eq(CALLER_ID), any(LocalDateTime.class), any(LocalDateTime.class)))
                    .thenReturn(clientInfo);
            ArgumentCaptor<LocalDateTime> contextFromCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
            ArgumentCaptor<LocalDateTime> contextToCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
            ArgumentCaptor<LocalDateTime> countFromCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
            ArgumentCaptor<LocalDateTime> countToCaptor = ArgumentCaptor.forClass(LocalDateTime.class);

            service.getSecurityOverview(CALLER_ID, periodType);

            verify(clientInfoResolver).getClientContext(eq(CALLER_ID), contextFromCaptor.capture(), contextToCaptor.capture());
            verify(sharedAccountActivityLogRepository).countByUserIdAndActivityType(
                    eq(CALLER_ID), eq(SharedAccountActivityLogType.EXPENSE_CREATED), countFromCaptor.capture(), countToCaptor.capture());
            assertEquals(contextFromCaptor.getValue(), countFromCaptor.getValue());
            assertEquals(contextToCaptor.getValue(), countToCaptor.getValue());
        }

        @Test
        void shouldThrowExceptionWhenPeriodTypeIsNull() {
            assertThrows(NullPointerException.class, () -> service.getSecurityOverview(CALLER_ID, null));

            verifyNoInteractions(authBackendClient, clientInfoResolver, sharedAccountActivityLogRepository);
        }

        @Test
        void shouldThrowExceptionWhenAuthBackendClientFails() {
            when(authBackendClient.getSharedAccountMembers(CALLER_ID)).thenThrow(new IllegalStateException("auth down"));

            assertThrows(IllegalStateException.class, () -> service.getSecurityOverview(CALLER_ID, periodType));

            verifyNoInteractions(clientInfoResolver, sharedAccountActivityLogRepository);
        }

        @Test
        void shouldThrowExceptionWhenClientInfoResolverFails() {
            SharedAccountMemberInfoDto member = createMember(CALLER_ID);
            when(authBackendClient.getSharedAccountMembers(CALLER_ID)).thenReturn(List.of(member));
            when(clientInfoResolver.getClientContext(eq(CALLER_ID), any(LocalDateTime.class), any(LocalDateTime.class)))
                    .thenThrow(new IllegalStateException("resolver failed"));

            assertThrows(IllegalStateException.class, () -> service.getSecurityOverview(CALLER_ID, periodType));

            verifyNoInteractions(sharedAccountActivityLogRepository);
        }

        @Test
        void shouldThrowExceptionWhenRepositoryCountFails() {
            SharedAccountMemberInfoDto member = createMember(CALLER_ID);
            when(authBackendClient.getSharedAccountMembers(CALLER_ID)).thenReturn(List.of(member));
            when(clientInfoResolver.getClientContext(eq(CALLER_ID), any(LocalDateTime.class), any(LocalDateTime.class)))
                    .thenReturn(clientInfo);
            when(sharedAccountActivityLogRepository.countByUserIdAndActivityType(
                    eq(CALLER_ID), eq(SharedAccountActivityLogType.EXPENSE_CREATED), any(LocalDateTime.class), any(LocalDateTime.class)))
                    .thenThrow(new IllegalStateException("db failed"));

            assertThrows(IllegalStateException.class, () -> service.getSecurityOverview(CALLER_ID, periodType));
        }

        @Test
        void shouldThrowExceptionWhenRepositoryLastActivityFails() {
            SharedAccountMemberInfoDto member = createMember(CALLER_ID);
            when(authBackendClient.getSharedAccountMembers(CALLER_ID)).thenReturn(List.of(member));
            when(clientInfoResolver.getClientContext(eq(CALLER_ID), any(LocalDateTime.class), any(LocalDateTime.class)))
                    .thenReturn(clientInfo);
            when(sharedAccountActivityLogRepository.findLastActivityDateByUserId(CALLER_ID))
                    .thenThrow(new IllegalStateException("db failed"));

            assertThrows(IllegalStateException.class, () -> service.getSecurityOverview(CALLER_ID, periodType));
        }
    }
}