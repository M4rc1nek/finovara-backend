package com.finovara.financeservice.internal.digest.report.email.service;

import com.finovara.contracts.model.transaction.RevenueCategory;
import com.finovara.financeservice.internal.digest.report.email.dto.RevenueSummary;
import com.finovara.financeservice.revenue.model.Revenue;
import com.finovara.financeservice.revenue.repository.RevenueRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RevenueDigestServiceTest {

    private static final Long USER_ID = 1L;
    private static final LocalDate FROM = LocalDate.of(2026, 8, 10);
    private static final LocalDate TO = LocalDate.of(2026, 8, 16);

    @Mock
    private RevenueRepository revenueRepository;

    private RevenueDigestService service;

    @BeforeEach
    void setUp() {
        service = new RevenueDigestService(revenueRepository);
    }

    @Nested
    class CalculateSummary {

        @Test
        void shouldReturnCompleteSummaryWhenDataExists() {
            Revenue highestRevenue = mock(Revenue.class);
            when(highestRevenue.getAmount()).thenReturn(new BigDecimal("500"));
            when(highestRevenue.getCategory()).thenReturn(RevenueCategory.SALARY);
            when(highestRevenue.getCreatedAt()).thenReturn(LocalDate.of(2026, 8, 12));

            when(revenueRepository.sumRevenuesByUserAndDateRange(USER_ID, FROM, TO)).thenReturn(Optional.of(new BigDecimal("700")));
            when(revenueRepository.findTopRevenueCategoriesByUserIdAndPeriod(eq(USER_ID), eq(FROM), eq(TO), eq(PageRequest.of(0, 1)))).thenReturn(List.of(RevenueCategory.SALARY));
            when(revenueRepository.findTopRevenuesByUserIdAndPeriod(eq(USER_ID), eq(FROM), eq(TO), eq(PageRequest.of(0, 1)))).thenReturn(List.of(highestRevenue));

            RevenueSummary result = service.calculateSummary(USER_ID, FROM, TO);

            assertEquals(new BigDecimal("700"), result.sum());
            assertEquals("SALARY", result.topCategory());
            assertEquals(new BigDecimal("500"), result.highestAmount());
            assertEquals("SALARY", result.highestCategory());
            assertEquals(LocalDate.of(2026, 8, 12), result.highestDate());
        }

        @Test
        void shouldReturnZeroSumWhenNoRevenuesFound() {
            when(revenueRepository.sumRevenuesByUserAndDateRange(USER_ID, FROM, TO)).thenReturn(Optional.empty());
            when(revenueRepository.findTopRevenueCategoriesByUserIdAndPeriod(eq(USER_ID), eq(FROM), eq(TO), eq(PageRequest.of(0, 1)))).thenReturn(List.of());
            when(revenueRepository.findTopRevenuesByUserIdAndPeriod(eq(USER_ID), eq(FROM), eq(TO), eq(PageRequest.of(0, 1)))).thenReturn(List.of());

            RevenueSummary result = service.calculateSummary(USER_ID, FROM, TO);

            assertEquals(BigDecimal.ZERO, result.sum());
        }

        @Test
        void shouldReturnNullTopCategoryWhenNoCategoriesFound() {
            when(revenueRepository.sumRevenuesByUserAndDateRange(USER_ID, FROM, TO)).thenReturn(Optional.of(BigDecimal.TEN));
            when(revenueRepository.findTopRevenueCategoriesByUserIdAndPeriod(eq(USER_ID), eq(FROM), eq(TO), eq(PageRequest.of(0, 1)))).thenReturn(List.of());
            when(revenueRepository.findTopRevenuesByUserIdAndPeriod(eq(USER_ID), eq(FROM), eq(TO), eq(PageRequest.of(0, 1)))).thenReturn(List.of());

            RevenueSummary result = service.calculateSummary(USER_ID, FROM, TO);

            assertNull(result.topCategory());
        }

        @Test
        void shouldReturnZeroHighestAmountAndNullFieldsWhenNoRevenueFound() {
            when(revenueRepository.sumRevenuesByUserAndDateRange(USER_ID, FROM, TO)).thenReturn(Optional.of(BigDecimal.ZERO));
            when(revenueRepository.findTopRevenueCategoriesByUserIdAndPeriod(eq(USER_ID), eq(FROM), eq(TO), eq(PageRequest.of(0, 1)))).thenReturn(List.of());
            when(revenueRepository.findTopRevenuesByUserIdAndPeriod(eq(USER_ID), eq(FROM), eq(TO), eq(PageRequest.of(0, 1)))).thenReturn(List.of());

            RevenueSummary result = service.calculateSummary(USER_ID, FROM, TO);

            assertEquals(BigDecimal.ZERO, result.highestAmount());
            assertNull(result.highestCategory());
            assertNull(result.highestDate());
        }

        @Test
        void shouldQueryRepositoryWithCorrectPageRequestForTopCategory() {
            when(revenueRepository.sumRevenuesByUserAndDateRange(USER_ID, FROM, TO)).thenReturn(Optional.of(BigDecimal.ZERO));
            when(revenueRepository.findTopRevenueCategoriesByUserIdAndPeriod(eq(USER_ID), eq(FROM), eq(TO), eq(PageRequest.of(0, 1)))).thenReturn(List.of());
            when(revenueRepository.findTopRevenuesByUserIdAndPeriod(eq(USER_ID), eq(FROM), eq(TO), eq(PageRequest.of(0, 1)))).thenReturn(List.of());

            service.calculateSummary(USER_ID, FROM, TO);

            ArgumentCaptor<PageRequest> pageRequestCaptor = ArgumentCaptor.forClass(PageRequest.class);
            verify(revenueRepository).findTopRevenueCategoriesByUserIdAndPeriod(eq(USER_ID), eq(FROM), eq(TO), pageRequestCaptor.capture());
            assertEquals(1, pageRequestCaptor.getValue().getPageSize());
        }

        @Test
        void shouldQueryRepositoryWithCorrectDateRange() {
            when(revenueRepository.sumRevenuesByUserAndDateRange(USER_ID, FROM, TO)).thenReturn(Optional.of(BigDecimal.ZERO));
            when(revenueRepository.findTopRevenueCategoriesByUserIdAndPeriod(eq(USER_ID), eq(FROM), eq(TO), eq(PageRequest.of(0, 1)))).thenReturn(List.of());
            when(revenueRepository.findTopRevenuesByUserIdAndPeriod(eq(USER_ID), eq(FROM), eq(TO), eq(PageRequest.of(0, 1)))).thenReturn(List.of());

            service.calculateSummary(USER_ID, FROM, TO);

            verify(revenueRepository).sumRevenuesByUserAndDateRange(USER_ID, FROM, TO);
        }
    }
}