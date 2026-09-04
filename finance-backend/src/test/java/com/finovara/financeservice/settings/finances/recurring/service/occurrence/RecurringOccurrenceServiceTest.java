package com.finovara.financeservice.settings.finances.recurring.service.occurrence;

import com.finovara.contracts.exception.badrequest.InvalidInputException;
import com.finovara.contracts.model.PeriodType;
import com.finovara.contracts.model.transaction.ExpenseCategory;
import com.finovara.contracts.model.transaction.RevenueCategory;
import com.finovara.financeservice.settings.finances.recurring.dto.RecurringOccurrenceDto;
import com.finovara.financeservice.settings.finances.recurring.model.RecurringSettings;
import com.finovara.contracts.model.RecurringType;
import com.finovara.financeservice.settings.finances.recurring.repository.RecurringSettingsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecurringOccurrenceServiceTest {

    private static final Long USER_ID = 1L;
    private static final long MAX_RANGE_DAYS = 180;

    @Mock
    private RecurringSettingsRepository recurringSettingsRepository;

    private RecurringOccurrenceService recurringOccurrenceService;

    @BeforeEach
    void setUp() {
        recurringOccurrenceService = new RecurringOccurrenceService(recurringSettingsRepository);
        ReflectionTestUtils.setField(recurringOccurrenceService, "maxRangeDay", MAX_RANGE_DAYS);
    }

    private RecurringSettings buildExpenseRule(LocalDate nextExecutionDate, LocalDate endDate, PeriodType periodType) {
        RecurringSettings settings = new RecurringSettings();
        settings.setId(1L);
        settings.setUserId(USER_ID);
        settings.setType(RecurringType.EXPENSE);
        settings.setEnable(true);
        settings.setAmount(BigDecimal.valueOf(250));
        settings.setExpenseCategory(ExpenseCategory.HOUSING);
        settings.setPeriodType(periodType);
        settings.setNextExecutionDate(nextExecutionDate);
        settings.setEndDate(endDate);
        return settings;
    }

    @Nested
    class ValidateDateRange {

        @Test
        void shouldThrowWhenFromIsAfterTo() {
            LocalDate from = LocalDate.of(2026, 8, 10);
            LocalDate to = LocalDate.of(2026, 8, 1);

            assertThrows(InvalidInputException.class,
                    () -> recurringOccurrenceService.getUpcomingOccurrences(USER_ID, from, to));
        }

        @Test
        void shouldThrowWhenRangeExceedsMaxDays() {
            LocalDate from = LocalDate.of(2026, 1, 1);
            LocalDate to = from.plusDays(MAX_RANGE_DAYS + 1);

            assertThrows(InvalidInputException.class,
                    () -> recurringOccurrenceService.getUpcomingOccurrences(USER_ID, from, to));
        }

        @Test
        void shouldNotThrowWhenRangeIsExactlyMaxDays() {
            LocalDate from = LocalDate.of(2026, 1, 1);
            LocalDate to = from.plusDays(MAX_RANGE_DAYS);

            when(recurringSettingsRepository.findAllEnabledByUserId(USER_ID)).thenReturn(List.of());

            assertThat(recurringOccurrenceService.getUpcomingOccurrences(USER_ID, from, to)).isEmpty();
        }
    }

    @Nested
    class GenerateOccurrences {

        @Test
        void shouldGenerateMonthlyOccurrencesWithinRange() {
            RecurringSettings rule = buildExpenseRule(
                    LocalDate.of(2026, 8, 5), null, PeriodType.MONTHLY);

            when(recurringSettingsRepository.findAllEnabledByUserId(USER_ID)).thenReturn(List.of(rule));

            List<RecurringOccurrenceDto> result = recurringOccurrenceService.getUpcomingOccurrences(
                    USER_ID, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 10, 31));

            assertThat(result).extracting(RecurringOccurrenceDto::date)
                    .containsExactly(
                            LocalDate.of(2026, 8, 5),
                            LocalDate.of(2026, 9, 5),
                            LocalDate.of(2026, 10, 5)
                    );
        }

        @Test
        void shouldExcludeOccurrencesBeforeFromDate() {
            RecurringSettings rule = buildExpenseRule(
                    LocalDate.of(2026, 7, 5), null, PeriodType.MONTHLY);

            when(recurringSettingsRepository.findAllEnabledByUserId(USER_ID)).thenReturn(List.of(rule));

            List<RecurringOccurrenceDto> result = recurringOccurrenceService.getUpcomingOccurrences(
                    USER_ID, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 9, 30));

            assertThat(result).extracting(RecurringOccurrenceDto::date)
                    .containsExactly(LocalDate.of(2026, 8, 5), LocalDate.of(2026, 9, 5));
        }

        @Test
        void shouldStopGeneratingAfterEndDate() {
            RecurringSettings rule = buildExpenseRule(
                    LocalDate.of(2026, 8, 5), LocalDate.of(2026, 9, 5), PeriodType.MONTHLY);

            when(recurringSettingsRepository.findAllEnabledByUserId(USER_ID)).thenReturn(List.of(rule));

            List<RecurringOccurrenceDto> result = recurringOccurrenceService.getUpcomingOccurrences(
                    USER_ID, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 12, 31));

            assertThat(result).extracting(RecurringOccurrenceDto::date)
                    .containsExactly(LocalDate.of(2026, 8, 5), LocalDate.of(2026, 9, 5));
        }

        @Test
        void shouldReturnEmptyListWhenEndDateIsBeforeFrom() {
            RecurringSettings rule = buildExpenseRule(
                    LocalDate.of(2026, 8, 5), LocalDate.of(2026, 8, 5), PeriodType.MONTHLY);

            when(recurringSettingsRepository.findAllEnabledByUserId(USER_ID)).thenReturn(List.of(rule));

            List<RecurringOccurrenceDto> result = recurringOccurrenceService.getUpcomingOccurrences(
                    USER_ID, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 10, 31));

            assertThat(result).isEmpty();
        }

        @Test
        void shouldMergeAndSortOccurrencesFromMultipleRules() {
            RecurringSettings rent = buildExpenseRule(LocalDate.of(2026, 8, 5), null, PeriodType.MONTHLY);

            RecurringSettings salary = new RecurringSettings();
            salary.setId(2L);
            salary.setUserId(USER_ID);
            salary.setType(RecurringType.REVENUE);
            salary.setEnable(true);
            salary.setAmount(BigDecimal.valueOf(3000));
            salary.setRevenueCategory(RevenueCategory.SALARY);
            salary.setPeriodType(PeriodType.MONTHLY);
            salary.setNextExecutionDate(LocalDate.of(2026, 8, 10));
            salary.setEndDate(null);

            when(recurringSettingsRepository.findAllEnabledByUserId(USER_ID)).thenReturn(List.of(rent, salary));

            List<RecurringOccurrenceDto> result = recurringOccurrenceService.getUpcomingOccurrences(
                    USER_ID, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31));

            assertThat(result).extracting(RecurringOccurrenceDto::date)
                    .containsExactly(LocalDate.of(2026, 8, 5), LocalDate.of(2026, 8, 10));
        }

        @Test
        void shouldMapAllFieldsToOccurrenceDto() {
            RecurringSettings rule = buildExpenseRule(LocalDate.of(2026, 8, 5), null, PeriodType.MONTHLY);
            rule.setPiggyBankId(99L);

            when(recurringSettingsRepository.findAllEnabledByUserId(USER_ID)).thenReturn(List.of(rule));

            List<RecurringOccurrenceDto> result = recurringOccurrenceService.getUpcomingOccurrences(
                    USER_ID, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31));

            assertThat(result).hasSize(1);
            RecurringOccurrenceDto occurrence = result.getFirst();
            assertThat(occurrence.date()).isEqualTo(LocalDate.of(2026, 8, 5));
            assertThat(occurrence.type()).isEqualTo(RecurringType.EXPENSE);
            assertThat(occurrence.amount()).isEqualByComparingTo(BigDecimal.valueOf(250));
            assertThat(occurrence.expenseCategory()).isEqualTo(ExpenseCategory.HOUSING);
            assertThat(occurrence.revenueCategory()).isNull();
            assertThat(occurrence.recurringSettingsId()).isEqualTo(1L);
            assertThat(occurrence.piggyBankId()).isEqualTo(99L);
        }

        @Test
        void shouldReturnEmptyListWhenNoActiveRulesExist() {
            when(recurringSettingsRepository.findAllEnabledByUserId(USER_ID)).thenReturn(List.of());

            List<RecurringOccurrenceDto> result = recurringOccurrenceService.getUpcomingOccurrences(
                    USER_ID, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31));

            assertThat(result).isEmpty();
        }
    }
}