package com.finovara.reportservice.sharedaccount.report.finances.calculate.highesttransactions.highestrevenue.service;

import com.finovara.contracts.exception.badrequest.InvalidInputException;
import com.finovara.contracts.model.PeriodType;
import com.finovara.contracts.transaction.report.dto.HighestRevenueDto;
import com.finovara.reportservice.feignclient.FinanceBackendSharedReportClient;
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
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SharedHighestRevenueServiceTest {

    private static final Long OWNER_ID = 1L;
    private static final Long MEMBER_ID = 2L;
    private static final int PAGE_SIZE = 5;

    @Mock
    private FinanceBackendSharedReportClient reportClient;

    private Clock clock;
    private SharedHighestRevenueService sharedHighestRevenueService;

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(Instant.parse("2026-07-11T00:00:00Z"), ZoneOffset.UTC);
        sharedHighestRevenueService = new SharedHighestRevenueService(reportClient, clock);
        ReflectionTestUtils.setField(sharedHighestRevenueService, "pageSize", PAGE_SIZE);
    }

    @Nested
    class GetHighestRevenue {

        @ParameterizedTest
        @EnumSource(PeriodType.class)
        void shouldDelegateToClientWithCorrectDateRangeAndReturnResult(PeriodType periodType) {
            LocalDate to = LocalDate.now(clock);
            LocalDate from = periodType.getStartDate(to);
            List<HighestRevenueDto> expected = List.of();

            when(reportClient.highestRevenues(OWNER_ID, MEMBER_ID, from, to, PAGE_SIZE)).thenReturn(expected);

            List<HighestRevenueDto> result = sharedHighestRevenueService.getHighestRevenue(OWNER_ID, MEMBER_ID, periodType);

            assertThat(result).isSameAs(expected);
            verify(reportClient).highestRevenues(OWNER_ID, MEMBER_ID, from, to, PAGE_SIZE);
        }

        @Test
        void shouldUseConfiguredPageSizeWhenCallingClient() {
            ReflectionTestUtils.setField(sharedHighestRevenueService, "pageSize", 20);
            LocalDate to = LocalDate.now(clock);
            LocalDate from = PeriodType.DAILY.getStartDate(to);

            when(reportClient.highestRevenues(OWNER_ID, MEMBER_ID, from, to, 20)).thenReturn(Collections.emptyList());

            sharedHighestRevenueService.getHighestRevenue(OWNER_ID, MEMBER_ID, PeriodType.DAILY);

            verify(reportClient).highestRevenues(OWNER_ID, MEMBER_ID, from, to, 20);
        }

        @Test
        void shouldReturnEmptyListWhenNoRevenuesFound() {
            LocalDate to = LocalDate.now(clock);
            LocalDate from = PeriodType.WEEKLY.getStartDate(to);

            when(reportClient.highestRevenues(OWNER_ID, MEMBER_ID, from, to, PAGE_SIZE)).thenReturn(Collections.emptyList());

            List<HighestRevenueDto> result = sharedHighestRevenueService.getHighestRevenue(OWNER_ID, MEMBER_ID, PeriodType.WEEKLY);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    class Validation {

        @Test
        void shouldThrowInvalidInputExceptionWhenPeriodTypeIsNull() {
            assertThrows(InvalidInputException.class,
                    () -> sharedHighestRevenueService.getHighestRevenue(OWNER_ID, MEMBER_ID, null));

            verifyNoInteractions(reportClient);
        }
    }
}