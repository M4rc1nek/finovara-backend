package com.finovara.reportservice.report.smartreport.service.loader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ExtendWith(MockitoExtension.class)
class SmartReportTemplateServiceTest {

    private static final String MONTH_SPENDING_FILE = "smartreport/answer/MonthSpending";
    private static final String AVERAGE_DAY_SPENDING_FILE = "smartreport/answer/AverageDaySpending";
    private static final String EXPENSE_RATE_FILE = "smartreport/answer/ExpenseRate";
    private static final String SAVINGS_RATE_FILE = "smartreport/answer/SavingsRate";
    private static final String FALLBACK_MESSAGE = "Responses are null or empty";

    @Mock
    private SmartReportResourceLoader resourceLoader;

    @InjectMocks
    private SmartReportTemplateService service;

    @Nested
    class Render {

        private Map<String, List<String>> files;

        @BeforeEach
        void setUp() {
            files = new HashMap<>();
            when(resourceLoader.readLines(any(String.class)))
                    .thenAnswer(invocation -> files.getOrDefault(invocation.getArgument(0), Collections.emptyList()));
        }

        @Test
        void shouldReplacePlaceholderWithFormattedAmountWhenTemplateContainsPlaceholder() {
            files.put(MONTH_SPENDING_FILE, List.of("Wydałeś {amount} zł"));
            service.init();

            String result = service.render(SmartReportType.MONTH_SPENDING, new BigDecimal("123.456"));

            assertEquals("Wydałeś 123.46 zł", result);
        }

        @ParameterizedTest
        @CsvSource({
                "123.456,123.46",
                "1.005,1.01",
                "0,0.00",
                "10,10.00",
                "-5.555,-5.56",
                "0.004,0.00",
                "1000000.1,1000000.10",
                "1E+3,1000.00"
        })
        void shouldFormatAmountWithTwoDecimalPlacesWhenValueIsGiven(BigDecimal value, String expected) {
            files.put(MONTH_SPENDING_FILE, List.of("{amount}"));
            service.init();

            String result = service.render(SmartReportType.MONTH_SPENDING, value);

            assertEquals(expected, result);
        }

        @Test
        void shouldReplaceEveryPlaceholderWhenTemplateContainsMultiplePlaceholders() {
            files.put(MONTH_SPENDING_FILE, List.of("{amount} i jeszcze {amount}"));
            service.init();

            String result = service.render(SmartReportType.MONTH_SPENDING, BigDecimal.TEN);

            assertEquals("10.00 i jeszcze 10.00", result);
        }

        @Test
        void shouldReturnTemplateUnchangedWhenTemplateHasNoPlaceholder() {
            files.put(MONTH_SPENDING_FILE, List.of("Brak danych"));
            service.init();

            String result = service.render(SmartReportType.MONTH_SPENDING, BigDecimal.TEN);

            assertEquals("Brak danych", result);
        }

        @Test
        void shouldReturnOneOfTemplatesWhenManyTemplatesExist() {
            files.put(MONTH_SPENDING_FILE, List.of("A {amount}", "B {amount}", "C {amount}"));
            service.init();

            String result = service.render(SmartReportType.MONTH_SPENDING, BigDecimal.ONE);

            assertTrue(List.of("A 1.00", "B 1.00", "C 1.00").contains(result));
        }

        @Test
        void shouldUseTemplatesFromMatchingFileWhenTypeIsGiven() {
            files.put(MONTH_SPENDING_FILE, List.of("M {amount}"));
            files.put(AVERAGE_DAY_SPENDING_FILE, List.of("D {amount}"));
            files.put(EXPENSE_RATE_FILE, List.of("E {amount}"));
            files.put(SAVINGS_RATE_FILE, List.of("S {amount}"));
            service.init();

            assertEquals("M 1.00", service.render(SmartReportType.MONTH_SPENDING, BigDecimal.ONE));
            assertEquals("D 1.00", service.render(SmartReportType.AVERAGE_DAY_SPENDING, BigDecimal.ONE));
            assertEquals("E 1.00", service.render(SmartReportType.EXPENSE_RATE, BigDecimal.ONE));
            assertEquals("S 1.00", service.render(SmartReportType.SAVINGS_RATE, BigDecimal.ONE));
        }

        @Test
        void shouldReturnFallbackMessageWhenTemplateFileIsEmpty() {
            service.init();

            String result = service.render(SmartReportType.MONTH_SPENDING, BigDecimal.ONE);

            assertEquals(FALLBACK_MESSAGE, result);
        }

        @Test
        void shouldReturnFallbackMessageWhenTypeIsNull() {
            files.put(MONTH_SPENDING_FILE, List.of("M {amount}"));
            service.init();

            String result = service.render(null, BigDecimal.ONE);

            assertEquals(FALLBACK_MESSAGE, result);
        }

        @Test
        void shouldThrowExceptionWhenValueIsNull() {
            files.put(MONTH_SPENDING_FILE, List.of("M {amount}"));
            service.init();

            assertThrows(NullPointerException.class, () -> service.render(SmartReportType.MONTH_SPENDING, null));
        }

        @Test
        void shouldReadEveryTemplateFileExactlyOnceWhenInitIsCalled() {
            service.init();

            verify(resourceLoader, times(1)).readLines(MONTH_SPENDING_FILE);
            verify(resourceLoader, times(1)).readLines(AVERAGE_DAY_SPENDING_FILE);
            verify(resourceLoader, times(1)).readLines(EXPENSE_RATE_FILE);
            verify(resourceLoader, times(1)).readLines(SAVINGS_RATE_FILE);
            verifyNoMoreInteractions(resourceLoader);
        }
    }

    @Nested
    class RenderWithoutInit {

        @Test
        void shouldReturnFallbackMessageWhenInitWasNotCalled() {
            String result = service.render(SmartReportType.MONTH_SPENDING, BigDecimal.ONE);

            assertEquals(FALLBACK_MESSAGE, result);
            verifyNoInteractions(resourceLoader);
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
}