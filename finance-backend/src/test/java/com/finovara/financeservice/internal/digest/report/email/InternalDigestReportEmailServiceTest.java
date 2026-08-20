package com.finovara.financeservice.internal.digest.report.email;

import com.finovara.contracts.notification.email.digest.report.PiggyBankSummaryDto;
import com.finovara.contracts.notification.email.digest.report.WeeklyFinanceDigestReportDto;
import com.finovara.financeservice.feignclient.AuthBackendClient;
import com.finovara.financeservice.internal.digest.report.email.dto.ExpenseSummary;
import com.finovara.financeservice.internal.digest.report.email.dto.RevenueSummary;
import com.finovara.financeservice.internal.digest.report.email.mapper.WeeklyFinanceDigestReportMapper;
import com.finovara.financeservice.internal.digest.report.email.service.ExpenseDigestService;
import com.finovara.financeservice.internal.digest.report.email.service.PiggyBankDigestService;
import com.finovara.financeservice.internal.digest.report.email.service.RevenueDigestService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InternalDigestReportEmailServiceTest {

    @Mock
    private ExpenseDigestService expenseDigestService;

    @Mock
    private RevenueDigestService revenueDigestService;

    @Mock
    private PiggyBankDigestService piggyBankDigestService;

    @Mock
    private WeeklyFinanceDigestReportMapper weeklyFinanceDigestReportMapper;

    @Mock
    private AuthBackendClient authBackendClient;

    private InternalDigestReportEmailService service;

    @BeforeEach
    void setUp() {
        service = new InternalDigestReportEmailService(expenseDigestService, revenueDigestService, piggyBankDigestService, weeklyFinanceDigestReportMapper, authBackendClient);
    }

    private ExpenseSummary expenseSummary() {
        return new ExpenseSummary(new BigDecimal("100"), "FOOD", new BigDecimal("50"), "FOOD", LocalDate.now(), 2, new BigDecimal("40"));
    }

    private RevenueSummary revenueSummary() {
        return new RevenueSummary(new BigDecimal("300"), "SALARY", new BigDecimal("300"), "SALARY", LocalDate.now());
    }

    private PiggyBankSummaryDto piggyBankSummaryDto() {
        return new PiggyBankSummaryDto(1L, new BigDecimal("10"), new BigDecimal("50"), new BigDecimal("10"), false);
    }

    @Nested
    class GetWeeklyFinanceDigestReports {

        @Test
        void shouldReturnEmptyListWhenNoUserIdsExist() {
            when(authBackendClient.getAllUserIds()).thenReturn(List.of());

            List<WeeklyFinanceDigestReportDto> result = service.getWeeklyFinanceDigestReports();

            assertTrue(result.isEmpty());
            verify(expenseDigestService, never()).calculateSummary(any(), any(), any());
        }

        @Test
        void shouldBuildReportForSingleUser() {
            Long userId = 1L;
            WeeklyFinanceDigestReportDto dto = mock(WeeklyFinanceDigestReportDto.class);

            when(authBackendClient.getAllUserIds()).thenReturn(List.of(userId));
            when(expenseDigestService.calculateSummary(eq(userId), any(LocalDate.class), any(LocalDate.class))).thenReturn(expenseSummary());
            when(revenueDigestService.calculateSummary(eq(userId), any(LocalDate.class), any(LocalDate.class))).thenReturn(revenueSummary());
            when(piggyBankDigestService.calculateSummary(eq(userId), any(LocalDate.class), any(LocalDate.class))).thenReturn(piggyBankSummaryDto());
            when(weeklyFinanceDigestReportMapper.toDto(eq(userId), any(LocalDate.class), any(LocalDate.class), any(ExpenseSummary.class), any(RevenueSummary.class), any(PiggyBankSummaryDto.class))).thenReturn(dto);

            List<WeeklyFinanceDigestReportDto> result = service.getWeeklyFinanceDigestReports();

            assertEquals(1, result.size());
            assertEquals(dto, result.get(0));
        }

        @Test
        void shouldBuildReportsForMultipleUsers() {
            when(authBackendClient.getAllUserIds()).thenReturn(List.of(1L, 2L, 3L));
            when(expenseDigestService.calculateSummary(any(), any(LocalDate.class), any(LocalDate.class))).thenReturn(expenseSummary());
            when(revenueDigestService.calculateSummary(any(), any(LocalDate.class), any(LocalDate.class))).thenReturn(revenueSummary());
            when(piggyBankDigestService.calculateSummary(any(), any(LocalDate.class), any(LocalDate.class))).thenReturn(piggyBankSummaryDto());
            when(weeklyFinanceDigestReportMapper.toDto(any(), any(LocalDate.class), any(LocalDate.class), any(ExpenseSummary.class), any(RevenueSummary.class), any(PiggyBankSummaryDto.class))).thenReturn(mock(WeeklyFinanceDigestReportDto.class));

            List<WeeklyFinanceDigestReportDto> result = service.getWeeklyFinanceDigestReports();

            assertEquals(3, result.size());
            verify(expenseDigestService, times(3)).calculateSummary(any(), any(LocalDate.class), any(LocalDate.class));
        }

        @Test
        void shouldUseMondayAsWeekStartAndSundayAsWeekEnd() {
            Long userId = 1L;
            ArgumentCaptor<LocalDate> fromCaptor = ArgumentCaptor.forClass(LocalDate.class);
            ArgumentCaptor<LocalDate> toCaptor = ArgumentCaptor.forClass(LocalDate.class);

            when(authBackendClient.getAllUserIds()).thenReturn(List.of(userId));
            when(expenseDigestService.calculateSummary(eq(userId), fromCaptor.capture(), toCaptor.capture())).thenReturn(expenseSummary());
            when(revenueDigestService.calculateSummary(eq(userId), any(LocalDate.class), any(LocalDate.class))).thenReturn(revenueSummary());
            when(piggyBankDigestService.calculateSummary(eq(userId), any(LocalDate.class), any(LocalDate.class))).thenReturn(piggyBankSummaryDto());
            when(weeklyFinanceDigestReportMapper.toDto(eq(userId), any(LocalDate.class), any(LocalDate.class), any(ExpenseSummary.class), any(RevenueSummary.class), any(PiggyBankSummaryDto.class))).thenReturn(mock(WeeklyFinanceDigestReportDto.class));

            service.getWeeklyFinanceDigestReports();

            assertEquals(DayOfWeek.MONDAY, fromCaptor.getValue().getDayOfWeek());
            assertEquals(fromCaptor.getValue().plusDays(6), toCaptor.getValue());
        }

        @Test
        void shouldPassSameDateRangeToAllDigestServices() {
            Long userId = 1L;
            ArgumentCaptor<LocalDate> expenseFrom = ArgumentCaptor.forClass(LocalDate.class);
            ArgumentCaptor<LocalDate> revenueFrom = ArgumentCaptor.forClass(LocalDate.class);
            ArgumentCaptor<LocalDate> piggyFrom = ArgumentCaptor.forClass(LocalDate.class);

            when(authBackendClient.getAllUserIds()).thenReturn(List.of(userId));
            when(expenseDigestService.calculateSummary(eq(userId), expenseFrom.capture(), any(LocalDate.class))).thenReturn(expenseSummary());
            when(revenueDigestService.calculateSummary(eq(userId), revenueFrom.capture(), any(LocalDate.class))).thenReturn(revenueSummary());
            when(piggyBankDigestService.calculateSummary(eq(userId), piggyFrom.capture(), any(LocalDate.class))).thenReturn(piggyBankSummaryDto());
            when(weeklyFinanceDigestReportMapper.toDto(eq(userId), any(LocalDate.class), any(LocalDate.class), any(ExpenseSummary.class), any(RevenueSummary.class), any(PiggyBankSummaryDto.class))).thenReturn(mock(WeeklyFinanceDigestReportDto.class));

            service.getWeeklyFinanceDigestReports();

            assertEquals(expenseFrom.getValue(), revenueFrom.getValue());
            assertEquals(expenseFrom.getValue(), piggyFrom.getValue());
        }

        @Test
        void shouldThrowExceptionWhenExpenseDigestServiceFails() {
            when(authBackendClient.getAllUserIds()).thenReturn(List.of(1L));
            when(expenseDigestService.calculateSummary(any(), any(LocalDate.class), any(LocalDate.class))).thenThrow(new RuntimeException("expense failure"));

            assertThrows(RuntimeException.class, () -> service.getWeeklyFinanceDigestReports());
        }

        @Test
        void shouldThrowExceptionWhenAuthBackendClientFails() {
            when(authBackendClient.getAllUserIds()).thenThrow(new RuntimeException("auth failure"));

            assertThrows(RuntimeException.class, () -> service.getWeeklyFinanceDigestReports());
        }

        @Test
        void shouldThrowExceptionWhenMapperFails() {
            when(authBackendClient.getAllUserIds()).thenReturn(List.of(1L));
            when(expenseDigestService.calculateSummary(any(), any(LocalDate.class), any(LocalDate.class))).thenReturn(expenseSummary());
            when(revenueDigestService.calculateSummary(any(), any(LocalDate.class), any(LocalDate.class))).thenReturn(revenueSummary());
            when(piggyBankDigestService.calculateSummary(any(), any(LocalDate.class), any(LocalDate.class))).thenReturn(piggyBankSummaryDto());
            when(weeklyFinanceDigestReportMapper.toDto(any(), any(LocalDate.class), any(LocalDate.class), any(ExpenseSummary.class), any(RevenueSummary.class), any(PiggyBankSummaryDto.class))).thenThrow(new RuntimeException("mapping failure"));

            assertThrows(RuntimeException.class, () -> service.getWeeklyFinanceDigestReports());
        }
    }
}