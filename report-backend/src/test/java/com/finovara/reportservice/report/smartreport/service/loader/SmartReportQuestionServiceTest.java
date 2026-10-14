package com.finovara.reportservice.report.smartreport.service.loader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.finovara.contracts.exception.serviceunavailable.ServiceUnavailableException;
import com.finovara.reportservice.report.smartreport.model.SmartReportType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ExtendWith(MockitoExtension.class)
class SmartReportQuestionServiceTest {

    private static final String MONTH_SPENDING_FILE = "smartreport/question/MonthSpendingQuestion";
    private static final String AVERAGE_DAY_SPENDING_FILE = "smartreport/question/AverageDaySpendingQuestion";
    private static final String EXPENSE_RATE_FILE = "smartreport/question/ExpenseRateQuestion";
    private static final String SAVINGS_RATE_FILE = "smartreport/question/SavingsRateQuestion";
    private static final String MONTH_SPENDING_LINE = "MONTH_SPENDING|Ile wydałem w tym miesiącu?";

    @Mock
    private SmartReportResourceLoader resourceLoader;

    @InjectMocks
    private SmartReportQuestionService service;

    @Nested
    class Init {

        private Map<String, List<String>> files;

        @BeforeEach
        void setUp() {
            files = new HashMap<>();
            when(resourceLoader.readLines(any(String.class)))
                    .thenAnswer(invocation -> files.getOrDefault(invocation.getArgument(0), Collections.emptyList()));
        }

        @Test
        void shouldReturnTypeWhenQuestionMatchesRegisteredLine() {
            files.put(MONTH_SPENDING_FILE, List.of(MONTH_SPENDING_LINE));
            service.init();

            SmartReportType result = service.getTypeFromQuestion("Ile wydałem w tym miesiącu?");

            assertEquals(SmartReportType.MONTH_SPENDING, result);
        }

        @ParameterizedTest
        @CsvSource({
                "Ile wydałem w tym miesiącu?,MONTH_SPENDING",
                "Ile średnio wydaję dziennie?,AVERAGE_DAY_SPENDING",
                "Jaki jest mój wskaźnik wydatków?,EXPENSE_RATE",
                "Jaki jest mój wskaźnik oszczędności?,SAVINGS_RATE"
        })
        void shouldReturnTypeFromEachQuestionFileWhenQuestionIsRegistered(String question, SmartReportType expectedType) {
            files.put(MONTH_SPENDING_FILE, List.of("MONTH_SPENDING|Ile wydałem w tym miesiącu?"));
            files.put(AVERAGE_DAY_SPENDING_FILE, List.of("AVERAGE_DAY_SPENDING|Ile średnio wydaję dziennie?"));
            files.put(EXPENSE_RATE_FILE, List.of("EXPENSE_RATE|Jaki jest mój wskaźnik wydatków?"));
            files.put(SAVINGS_RATE_FILE, List.of("SAVINGS_RATE|Jaki jest mój wskaźnik oszczędności?"));
            service.init();

            SmartReportType result = service.getTypeFromQuestion(question);

            assertEquals(expectedType, result);
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "ILE WYDAŁEM W TYM MIESIĄCU",
                "ile wydalem w tym miesiacu",
                "  Ile wydałem w tym miesiącu?  ",
                "ile wydałem w tym miesiącu ?",
                "Ile wydałem w tym miesiącu??"
        })
        void shouldReturnTypeWhenQuestionDiffersByCaseDiacriticsWhitespaceOrQuestionMarks(String question) {
            files.put(MONTH_SPENDING_FILE, List.of(MONTH_SPENDING_LINE));
            service.init();

            SmartReportType result = service.getTypeFromQuestion(question);

            assertEquals(SmartReportType.MONTH_SPENDING, result);
        }

        @Test
        void shouldReturnNullWhenQuestionIsUnknown() {
            files.put(MONTH_SPENDING_FILE, List.of(MONTH_SPENDING_LINE));
            service.init();

            SmartReportType result = service.getTypeFromQuestion("Zupełnie inne pytanie");

            assertNull(result);
        }

        @Test
        void shouldReturnNullWhenQuestionIsNull() {
            files.put(MONTH_SPENDING_FILE, List.of(MONTH_SPENDING_LINE));
            service.init();

            SmartReportType result = service.getTypeFromQuestion(null);

            assertNull(result);
        }

        @ParameterizedTest
        @ValueSource(strings = {"MONTH_SPENDING", "MONTH_SPENDING|Pytanie|dodatkowe", ""})
        void shouldIgnoreLineWhenLineDoesNotHaveExactlyTwoParts(String line) {
            files.put(MONTH_SPENDING_FILE, List.of(line));
            service.init();

            SmartReportType result = service.getTypeFromQuestion("Pytanie");

            assertNull(result);
        }

        @Test
        void shouldTrimTypeNameWhenTypeNameHasWhitespace() {
            files.put(MONTH_SPENDING_FILE, List.of(" MONTH_SPENDING |Pytanie testowe"));
            service.init();

            SmartReportType result = service.getTypeFromQuestion("Pytanie testowe");

            assertEquals(SmartReportType.MONTH_SPENDING, result);
        }

        @Test
        void shouldOverwritePreviousTypeWhenQuestionIsDuplicated() {
            files.put(MONTH_SPENDING_FILE, List.of("MONTH_SPENDING|Pytanie", "SAVINGS_RATE|pytanie?"));
            service.init();

            SmartReportType result = service.getTypeFromQuestion("Pytanie");

            assertEquals(SmartReportType.SAVINGS_RATE, result);
        }

        @Test
        void shouldReadEveryQuestionFileExactlyOnceWhenInitIsCalled() {
            service.init();

            verify(resourceLoader, times(1)).readLines(MONTH_SPENDING_FILE);
            verify(resourceLoader, times(1)).readLines(AVERAGE_DAY_SPENDING_FILE);
            verify(resourceLoader, times(1)).readLines(EXPENSE_RATE_FILE);
            verify(resourceLoader, times(1)).readLines(SAVINGS_RATE_FILE);
            verifyNoMoreInteractions(resourceLoader);
        }

        @Test
        void shouldThrowExceptionWhenTypeNameIsUnknown() {
            files.put(MONTH_SPENDING_FILE, List.of("UNKNOWN_TYPE|Pytanie"));

            assertThrows(IllegalArgumentException.class, () -> service.init());
        }
    }

    @Nested
    class InitWithLoaderFailure {

        @Test
        void shouldThrowExceptionWhenResourceLoaderFails() {
            when(resourceLoader.readLines(MONTH_SPENDING_FILE))
                    .thenThrow(new ServiceUnavailableException("Failed to load resource", new IllegalStateException("missing")));

            assertThrows(ServiceUnavailableException.class, () -> service.init());
        }
    }

    @Nested
    class GetTypeFromQuestionWithoutInit {

        @Test
        void shouldReturnNullWhenInitWasNotCalled() {
            SmartReportType result = service.getTypeFromQuestion("Ile wydałem w tym miesiącu?");

            assertNull(result);
            verifyNoInteractions(resourceLoader);
        }
    }
}