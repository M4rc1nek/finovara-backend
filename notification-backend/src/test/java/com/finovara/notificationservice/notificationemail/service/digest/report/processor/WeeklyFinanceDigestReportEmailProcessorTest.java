package com.finovara.notificationservice.notificationemail.service.digest.report.processor;

import com.finovara.contracts.authorization.dto.UserDataResponse;
import com.finovara.contracts.notification.email.digest.report.finance.PiggyBankSummaryDto;
import com.finovara.contracts.notification.email.digest.report.finance.WeeklyFinanceDigestReportDto;
import com.finovara.notificationservice.feignclient.AuthBackendClient;
import com.finovara.notificationservice.feignclient.FinanceBackendClient;
import com.finovara.notificationservice.notificationemail.model.ScheduledEmailNotificationType;
import com.finovara.notificationservice.notificationemail.service.EmailNotifier;
import com.finovara.notificationservice.notificationemail.service.digest.report.finance.processor.WeeklyFinanceDigestReportEmailProcessor;
import com.finovara.notificationservice.notificationemail.util.CategoryLabelResolver;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WeeklyFinanceDigestReportEmailProcessorTest {

    private static final Long USER_ID = 1L;

    @Mock
    private AuthBackendClient authBackendClient;

    @Mock
    private FinanceBackendClient financeBackendClient;

    @Mock
    private EmailNotifier emailNotifier;

    @Mock
    private CategoryLabelResolver categoryLabelResolver;

    @InjectMocks
    private WeeklyFinanceDigestReportEmailProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new WeeklyFinanceDigestReportEmailProcessor(authBackendClient, financeBackendClient, emailNotifier, categoryLabelResolver);
    }

    private WeeklyFinanceDigestReportDto fullReport() {
        return new WeeklyFinanceDigestReportDto(
                USER_ID,
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 1, 7),
                new BigDecimal("100.00"),
                null,
                new BigDecimal("200.00"),
                null,
                new BigDecimal("50.00"),
                new BigDecimal("30.00"),
                5,
                new BigDecimal("40.00"),
                null,
                LocalDate.of(2026, 1, 3),
                new BigDecimal("80.00"),
                null,
                LocalDate.of(2026, 1, 5),
                new PiggyBankSummaryDto(2, new BigDecimal("500.00"), new BigDecimal("75.00"), new BigDecimal("125.00"), true)
        );
    }

    private WeeklyFinanceDigestReportDto emptyReport() {
        return new WeeklyFinanceDigestReportDto(
                USER_ID,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                new PiggyBankSummaryDto(0, null, null, null, false)
        );
    }

    @Nested
    class SendDigestEmailReport {

        @Test
        void shouldSendEmailWhenUserHasEmail() {
            WeeklyFinanceDigestReportDto report = fullReport();
            UserDataResponse user = new UserDataResponse(USER_ID, Optional.of("john"), Optional.of("john@example.com"));

            when(financeBackendClient.getWeeklyFinanceDigestReports()).thenReturn(List.of(report));
            when(authBackendClient.getUserData(USER_ID)).thenReturn(user);
            when(categoryLabelResolver.resolveExpenseCategoryName(isNull())).thenReturn("Jedzenie");
            when(categoryLabelResolver.resolveRevenueCategoryName(isNull())).thenReturn("Wynagrodzenie");

            processor.sendWeeklyFinanceDigestEmail();

            verify(emailNotifier).send(eq(ScheduledEmailNotificationType.WEEKLY_FINANCE_DIGEST_REPORT_EMAIL), eq("john@example.com"), any());
        }

        @Test
        void shouldNotSendEmailWhenUserEmailIsEmpty() {
            WeeklyFinanceDigestReportDto report = fullReport();
            UserDataResponse user = new UserDataResponse(USER_ID, Optional.of("john"), Optional.empty());

            when(financeBackendClient.getWeeklyFinanceDigestReports()).thenReturn(List.of(report));
            when(authBackendClient.getUserData(USER_ID)).thenReturn(user);

            processor.sendWeeklyFinanceDigestEmail();

            verify(emailNotifier, never()).send(any(), any(), any());
        }

        @Test
        void shouldNotCallAnythingWhenReportsListIsEmpty() {
            when(financeBackendClient.getWeeklyFinanceDigestReports()).thenReturn(List.of());

            processor.sendWeeklyFinanceDigestEmail();

            verifyNoInteractions(authBackendClient);
            verifyNoInteractions(emailNotifier);
        }

        @Test
        void shouldProcessMultipleReportsIndependently() {
            WeeklyFinanceDigestReportDto reportOne = fullReport();
            WeeklyFinanceDigestReportDto reportTwo = new WeeklyFinanceDigestReportDto(
                    2L, LocalDate.of(2026, 1, 8), LocalDate.of(2026, 1, 14),
                    new BigDecimal("10.00"), null, new BigDecimal("20.00"), null,
                    new BigDecimal("5.00"), new BigDecimal("3.00"), 1,
                    new BigDecimal("4.00"), null, LocalDate.of(2026, 1, 9),
                    new BigDecimal("8.00"), null, LocalDate.of(2026, 1, 10),
                    new PiggyBankSummaryDto(1, new BigDecimal("50.00"), new BigDecimal("10.00"), new BigDecimal("40.00"), false)
            );
            UserDataResponse userOne = new UserDataResponse(USER_ID, Optional.of("john"), Optional.of("john@example.com"));
            UserDataResponse userTwo = new UserDataResponse(2L, Optional.of("anna"), Optional.of("anna@example.com"));

            when(financeBackendClient.getWeeklyFinanceDigestReports()).thenReturn(List.of(reportOne, reportTwo));
            when(authBackendClient.getUserData(USER_ID)).thenReturn(userOne);
            when(authBackendClient.getUserData(2L)).thenReturn(userTwo);
            when(categoryLabelResolver.resolveExpenseCategoryName(isNull())).thenReturn("Jedzenie");
            when(categoryLabelResolver.resolveRevenueCategoryName(isNull())).thenReturn("Wynagrodzenie");

            processor.sendWeeklyFinanceDigestEmail();

            verify(emailNotifier, times(2)).send(any(), any(), any());
        }

        @Test
        void shouldUseDefaultUsernameWhenUsernameIsAbsent() {
            WeeklyFinanceDigestReportDto report = fullReport();
            UserDataResponse user = new UserDataResponse(USER_ID, Optional.empty(), Optional.of("john@example.com"));

            when(financeBackendClient.getWeeklyFinanceDigestReports()).thenReturn(List.of(report));
            when(authBackendClient.getUserData(USER_ID)).thenReturn(user);
            when(categoryLabelResolver.resolveExpenseCategoryName(isNull())).thenReturn("Jedzenie");
            when(categoryLabelResolver.resolveRevenueCategoryName(isNull())).thenReturn("Wynagrodzenie");

            ArgumentCaptor<Map<String, String>> captor = ArgumentCaptor.forClass(Map.class);

            processor.sendWeeklyFinanceDigestEmail();

            verify(emailNotifier).send(any(), any(), captor.capture());
            assertEquals("Użytkowniku", captor.getValue().get("userName"));
        }

        @Test
        void shouldFormatNullValuesAsNotAvailable() {
            WeeklyFinanceDigestReportDto report = emptyReport();
            UserDataResponse user = new UserDataResponse(USER_ID, Optional.of("john"), Optional.of("john@example.com"));

            when(financeBackendClient.getWeeklyFinanceDigestReports()).thenReturn(List.of(report));
            when(authBackendClient.getUserData(USER_ID)).thenReturn(user);
            when(categoryLabelResolver.resolveExpenseCategoryName(isNull())).thenReturn(null);
            when(categoryLabelResolver.resolveRevenueCategoryName(isNull())).thenReturn(null);

            ArgumentCaptor<Map<String, String>> captor = ArgumentCaptor.forClass(Map.class);

            processor.sendWeeklyFinanceDigestEmail();

            verify(emailNotifier).send(any(), any(), captor.capture());
            Map<String, String> placeholders = captor.getValue();
            assertEquals("—", placeholders.get("weekStart"));
            assertEquals("—", placeholders.get("expensesSum"));
            assertEquals("—", placeholders.get("topExpenseCategory"));
            assertEquals("—", placeholders.get("daysWithoutExpense"));
            assertEquals("Nie", placeholders.get("piggyBankGoalCompleted"));
        }

        @Test
        void shouldFormatGoalCompletedAsTakWhenTrue() {
            WeeklyFinanceDigestReportDto report = fullReport();
            UserDataResponse user = new UserDataResponse(USER_ID, Optional.of("john"), Optional.of("john@example.com"));

            when(financeBackendClient.getWeeklyFinanceDigestReports()).thenReturn(List.of(report));
            when(authBackendClient.getUserData(USER_ID)).thenReturn(user);
            when(categoryLabelResolver.resolveExpenseCategoryName(isNull())).thenReturn("Jedzenie");
            when(categoryLabelResolver.resolveRevenueCategoryName(isNull())).thenReturn("Wynagrodzenie");

            ArgumentCaptor<Map<String, String>> captor = ArgumentCaptor.forClass(Map.class);

            processor.sendWeeklyFinanceDigestEmail();

            verify(emailNotifier).send(any(), any(), captor.capture());
            assertEquals("Tak", captor.getValue().get("piggyBankGoalCompleted"));
        }

        @Test
        void shouldFormatDatesUsingDdMmYyyyPattern() {
            WeeklyFinanceDigestReportDto report = fullReport();
            UserDataResponse user = new UserDataResponse(USER_ID, Optional.of("john"), Optional.of("john@example.com"));

            when(financeBackendClient.getWeeklyFinanceDigestReports()).thenReturn(List.of(report));
            when(authBackendClient.getUserData(USER_ID)).thenReturn(user);
            when(categoryLabelResolver.resolveExpenseCategoryName(isNull())).thenReturn("Jedzenie");
            when(categoryLabelResolver.resolveRevenueCategoryName(isNull())).thenReturn("Wynagrodzenie");

            ArgumentCaptor<Map<String, String>> captor = ArgumentCaptor.forClass(Map.class);

            processor.sendWeeklyFinanceDigestEmail();

            verify(emailNotifier).send(any(), any(), captor.capture());
            assertEquals("01.01.2026", captor.getValue().get("weekStart"));
            assertEquals("07.01.2026", captor.getValue().get("weekEnd"));
        }

        @Test
        void shouldIncludePiggyBankQuantityAsPlainNumber() {
            WeeklyFinanceDigestReportDto report = fullReport();
            UserDataResponse user = new UserDataResponse(USER_ID, Optional.of("john"), Optional.of("john@example.com"));

            when(financeBackendClient.getWeeklyFinanceDigestReports()).thenReturn(List.of(report));
            when(authBackendClient.getUserData(USER_ID)).thenReturn(user);
            when(categoryLabelResolver.resolveExpenseCategoryName(isNull())).thenReturn("Jedzenie");
            when(categoryLabelResolver.resolveRevenueCategoryName(isNull())).thenReturn("Wynagrodzenie");

            ArgumentCaptor<Map<String, String>> captor = ArgumentCaptor.forClass(Map.class);

            processor.sendWeeklyFinanceDigestEmail();

            verify(emailNotifier).send(any(), any(), captor.capture());
            assertEquals("2", captor.getValue().get("piggyBankQuantity"));
        }
    }
}
