package com.finovara.reportservice.report.finances.calculate.highesttransactions.highestrevenue.service;

import com.finovara.contracts.exception.badrequest.InvalidInputException;
import com.finovara.contracts.model.PeriodType;
import com.finovara.contracts.transaction.report.dto.HighestRevenueDto;
import com.finovara.reportservice.feignclient.FinanceBackendReportClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HighestRevenueServiceTest {

    private static final Long USER_ID = 1L;
    private static final int PAGE_SIZE = 5;

    @Mock
    private FinanceBackendReportClient reportClient;

    private Clock clock;
    private HighestRevenueService highestRevenueService;

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(Instant.parse("2026-07-11T00:00:00Z"), ZoneOffset.UTC);
        highestRevenueService = new HighestRevenueService(reportClient, clock);
        ReflectionTestUtils.setField(highestRevenueService, "pageSize", PAGE_SIZE);
    }

    @Nested
    class GetHighestRevenue {

        @ParameterizedTest
        @EnumSource(PeriodType.class)
        void shouldDelegateToClientWithCorrectDateRangeAndReturnResult(PeriodType periodType) {
            LocalDate to = LocalDate.now(clock);
            LocalDate from = periodType.getStartDate(to);
            List<HighestRevenueDto> expected = List.of();

            when(reportClient.highestRevenues(USER_ID, from, to, PAGE_SIZE)).thenReturn(expected);

            List<HighestRevenueDto> result = highestRevenueService.getHighestRevenue(USER_ID, periodType);

            assertThat(result).isSameAs(expected);
            verify(reportClient).highestRevenues(USER_ID, from, to, PAGE_SIZE);
        }

        @Test
        void shouldUseConfiguredPageSizeWhenCallingClient() {
            ReflectionTestUtils.setField(highestRevenueService, "pageSize", 20);
            LocalDate to = LocalDate.now(clock);
            LocalDate from = PeriodType.DAILY.getStartDate(to);

            when(reportClient.highestRevenues(USER_ID, from, to, 20)).thenReturn(List.of());

            highestRevenueService.getHighestRevenue(USER_ID, PeriodType.DAILY);

            verify(reportClient).highestRevenues(USER_ID, from, to, 20);
        }
    }

    @Nested
    class Validation {

        @Test
        void shouldThrowInvalidInputExceptionWhenPeriodTypeIsNull() {
            assertThrows(InvalidInputException.class,
                    () -> highestRevenueService.getHighestRevenue(USER_ID, null));

            verifyNoInteractions(reportClient);
        }
    }
}