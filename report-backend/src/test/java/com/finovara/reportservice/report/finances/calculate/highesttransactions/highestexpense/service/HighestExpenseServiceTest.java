package com.finovara.reportservice.report.finances.highesttransactions.highestexpense.service;

import com.finovara.contracts.exception.badrequest.InvalidInputException;
import com.finovara.contracts.model.PeriodType;
import com.finovara.contracts.transaction.report.dto.HighestExpenseDto;
import com.finovara.reportservice.feignclient.FinanceBackendReportClient;
import com.finovara.reportservice.report.finances.calculate.highesttransactions.highestexpense.service.HighestExpenseService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HighestExpenseServiceTest {

    private static final Long USER_ID = 1L;
    private static final int PAGE_SIZE = 5;

    @Mock
    private FinanceBackendReportClient reportClient;

    @InjectMocks
    private HighestExpenseService highestExpenseService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(highestExpenseService, "pageSize", PAGE_SIZE);
    }

    @Nested
    class GetHighestExpense {

        @ParameterizedTest
        @EnumSource(PeriodType.class)
        void shouldDelegateToClientWithCorrectDateRangeAndReturnResult(PeriodType periodType) {
            LocalDate to = LocalDate.now();
            LocalDate from = periodType.getStartDate(to);
            List<HighestExpenseDto> expected = List.of();

            when(reportClient.highestExpenses(USER_ID, from, to, PAGE_SIZE)).thenReturn(expected);

            List<HighestExpenseDto> result = highestExpenseService.getHighestExpense(USER_ID, periodType);

            assertThat(result).isSameAs(expected);
            verify(reportClient).highestExpenses(USER_ID, from, to, PAGE_SIZE);
        }
    }

    @Nested
    class Validation {

        @Test
        void shouldThrowInvalidInputExceptionWhenPeriodTypeIsNull() {
            assertThrows(InvalidInputException.class,
                    () -> highestExpenseService.getHighestExpense(USER_ID, null));

            verifyNoInteractions(reportClient);
        }
    }
}
