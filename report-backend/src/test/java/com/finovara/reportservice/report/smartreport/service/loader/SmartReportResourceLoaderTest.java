package com.finovara.reportservice.report.smartreport.service.loader;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.finovara.contracts.exception.serviceunavailable.ServiceUnavailableException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

@ExtendWith(MockitoExtension.class)
class SmartReportResourceLoaderTest {

    @InjectMocks
    private SmartReportResourceLoader loader;

    @Nested
    class ReadLines {

        @ParameterizedTest
        @ValueSource(strings = {
                "smartreport/question/MonthSpendingQuestion",
                "smartreport/question/AverageDaySpendingQuestion",
                "smartreport/question/ExpenseRateQuestion",
                "smartreport/question/SavingsRateQuestion",
                "smartreport/answer/MonthSpending",
                "smartreport/answer/AverageDaySpending",
                "smartreport/answer/ExpenseRate",
                "smartreport/answer/SavingsRate"
        })
        void shouldReturnNonEmptyListWhenResourceExists(String path) {
            List<String> result = loader.readLines(path);

            assertFalse(result.isEmpty());
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "smartreport/question/MonthSpendingQuestion",
                "smartreport/answer/MonthSpending"
        })
        void shouldReturnOnlyNonBlankLinesWhenResourceExists(String path) {
            List<String> result = loader.readLines(path);

            assertTrue(result.stream().noneMatch(String::isBlank));
        }
    }

    @Nested
    class ReadLinesWithInvalidInput {

        @Test
        void shouldThrowExceptionWhenResourceDoesNotExist() {
            assertThrows(ServiceUnavailableException.class, () -> loader.readLines("smartreport/missing/NonExistingFile"));
        }

        @Test
        void shouldThrowExceptionWhenPathIsNull() {
            assertThrows(ServiceUnavailableException.class, () -> loader.readLines(null));
        }
    }
}