package com.finovara.financeservice.settings.finances.recurring.service.occurrence;

import com.finovara.contracts.exception.badrequest.InvalidInputException;
import com.finovara.contracts.util.PeriodType;
import com.finovara.contracts.util.model.RecurringType;
import com.finovara.financeservice.settings.finances.recurring.dto.RecurringOccurrenceDto;
import com.finovara.financeservice.settings.finances.recurring.model.RecurringSettings;
import com.finovara.financeservice.settings.finances.recurring.repository.RecurringSettingsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecurringOccurrenceServiceTest {

    private static final Long USER_ID = 1L;
    private static final long MAX_RANGE_DAYS = 100000L;
    private static final PeriodType PERIOD_TYPE = PeriodType.values()[0];
    private static final LocalDate DATE_1 = LocalDate.of(2025, 1, 1);
    private static final LocalDate DATE_2 = PERIOD_TYPE.addPeriod(DATE_1);
    private static final LocalDate DATE_3 = PERIOD_TYPE.addPeriod(DATE_2);
    private static final LocalDate DATE_4 = PERIOD_TYPE.addPeriod(DATE_3);

    @Mock
    private RecurringSettingsRepository recurringSettingsRepository;

    @Mock
    private RecurringSettings rule;

    @Mock
    private RecurringSettings otherRule;

    @InjectMocks
    private RecurringOccurrenceService service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "maxRangeDay", MAX_RANGE_DAYS);
    }

    @Nested
    class GetUpcomingOccurrences {

        @Test
        void shouldReturnEmptyListWhenNoEnabledRulesExist() {
            when(recurringSettingsRepository.findAllEnabledByUserId(USER_ID)).thenReturn(List.of());

            List<RecurringOccurrenceDto> result = service.getUpcomingOccurrences(USER_ID, DATE_1, DATE_3);

            assertTrue(result.isEmpty());
            verify(recurringSettingsRepository).findAllEnabledByUserId(USER_ID);
        }

        @Test
        void shouldReturnOccurrenceMappedFromRuleWhenRuleFallsInRange() {
            when(recurringSettingsRepository.findAllEnabledByUserId(USER_ID)).thenReturn(List.of(rule));
            when(rule.getNextExecutionDate()).thenReturn(DATE_1);
            when(rule.getPeriodType()).thenReturn(PERIOD_TYPE);
            when(rule.getEndDate()).thenReturn(null);
            when(rule.getType()).thenReturn(RecurringType.EXPENSE);
            when(rule.getId()).thenReturn(10L);
            when(rule.getPiggyBankId()).thenReturn(20L);
            when(rule.getAmount()).thenReturn(BigDecimal.TEN);

            List<RecurringOccurrenceDto> result = service.getUpcomingOccurrences(USER_ID, DATE_1, DATE_1);

            assertEquals(List.of(new RecurringOccurrenceDto(DATE_1, RecurringType.EXPENSE, BigDecimal.TEN, null, null, 10L, 20L)), result);
        }

        @Test
        void shouldReturnOnlyOccurrencesFromRangeStartWhenNextExecutionIsBeforeFrom() {
            when(recurringSettingsRepository.findAllEnabledByUserId(USER_ID)).thenReturn(List.of(rule));
            when(rule.getNextExecutionDate()).thenReturn(DATE_1);
            when(rule.getPeriodType()).thenReturn(PERIOD_TYPE);
            when(rule.getEndDate()).thenReturn(null);

            List<RecurringOccurrenceDto> result = service.getUpcomingOccurrences(USER_ID, DATE_2, DATE_3);

            assertEquals(List.of(DATE_2, DATE_3), result.stream().map(RecurringOccurrenceDto::date).toList());
        }

        @Test
        void shouldStopGeneratingOccurrencesWhenEndDateIsReached() {
            when(recurringSettingsRepository.findAllEnabledByUserId(USER_ID)).thenReturn(List.of(rule));
            when(rule.getNextExecutionDate()).thenReturn(DATE_1);
            when(rule.getPeriodType()).thenReturn(PERIOD_TYPE);
            when(rule.getEndDate()).thenReturn(DATE_3);

            List<RecurringOccurrenceDto> result = service.getUpcomingOccurrences(USER_ID, DATE_1, DATE_4);

            assertEquals(List.of(DATE_1, DATE_2, DATE_3), result.stream().map(RecurringOccurrenceDto::date).toList());
        }

        @Test
        void shouldReturnSingleOccurrenceWhenEndDateEqualsNextExecutionDate() {
            when(recurringSettingsRepository.findAllEnabledByUserId(USER_ID)).thenReturn(List.of(rule));
            when(rule.getNextExecutionDate()).thenReturn(DATE_1);
            when(rule.getPeriodType()).thenReturn(PERIOD_TYPE);
            when(rule.getEndDate()).thenReturn(DATE_1);

            List<RecurringOccurrenceDto> result = service.getUpcomingOccurrences(USER_ID, DATE_1, DATE_3);

            assertEquals(List.of(DATE_1), result.stream().map(RecurringOccurrenceDto::date).toList());
        }

        @Test
        void shouldReturnEmptyListWhenNextExecutionIsAfterRangeEnd() {
            when(recurringSettingsRepository.findAllEnabledByUserId(USER_ID)).thenReturn(List.of(rule));
            when(rule.getNextExecutionDate()).thenReturn(DATE_3);
            when(rule.getPeriodType()).thenReturn(PERIOD_TYPE);

            List<RecurringOccurrenceDto> result = service.getUpcomingOccurrences(USER_ID, DATE_1, DATE_2);

            assertTrue(result.isEmpty());
        }

        @Test
        void shouldReturnEmptyListWhenAllOccurrencesAreBeforeRangeStart() {
            when(recurringSettingsRepository.findAllEnabledByUserId(USER_ID)).thenReturn(List.of(rule));
            when(rule.getNextExecutionDate()).thenReturn(DATE_1);
            when(rule.getPeriodType()).thenReturn(PERIOD_TYPE);
            when(rule.getEndDate()).thenReturn(DATE_1);

            List<RecurringOccurrenceDto> result = service.getUpcomingOccurrences(USER_ID, DATE_2, DATE_3);

            assertTrue(result.isEmpty());
        }

        @Test
        void shouldSkipRuleWhenNextExecutionDateIsNull() {
            when(recurringSettingsRepository.findAllEnabledByUserId(USER_ID)).thenReturn(List.of(rule));
            when(rule.getNextExecutionDate()).thenReturn(null);

            List<RecurringOccurrenceDto> result = service.getUpcomingOccurrences(USER_ID, DATE_1, DATE_3);

            assertTrue(result.isEmpty());
        }

        @Test
        void shouldSkipRuleWhenPeriodTypeIsNull() {
            when(recurringSettingsRepository.findAllEnabledByUserId(USER_ID)).thenReturn(List.of(rule));
            when(rule.getNextExecutionDate()).thenReturn(DATE_1);
            when(rule.getPeriodType()).thenReturn(null);

            List<RecurringOccurrenceDto> result = service.getUpcomingOccurrences(USER_ID, DATE_1, DATE_3);

            assertTrue(result.isEmpty());
        }

        @Test
        void shouldReturnOccurrencesSortedByDateWhenMultipleRulesExist() {
            when(recurringSettingsRepository.findAllEnabledByUserId(USER_ID)).thenReturn(List.of(rule, otherRule));
            when(rule.getNextExecutionDate()).thenReturn(DATE_2);
            when(rule.getPeriodType()).thenReturn(PERIOD_TYPE);
            when(rule.getEndDate()).thenReturn(null);
            when(otherRule.getNextExecutionDate()).thenReturn(DATE_1);
            when(otherRule.getPeriodType()).thenReturn(PERIOD_TYPE);
            when(otherRule.getEndDate()).thenReturn(null);

            List<RecurringOccurrenceDto> result = service.getUpcomingOccurrences(USER_ID, DATE_1, DATE_3);

            assertEquals(
                    List.of(DATE_1, DATE_2, DATE_2, DATE_3, DATE_3),
                    result.stream().map(RecurringOccurrenceDto::date).toList()
            );
        }

        @Test
        void shouldIgnoreInvalidRuleAndProcessValidRuleWhenRulesAreMixed() {
            when(recurringSettingsRepository.findAllEnabledByUserId(USER_ID)).thenReturn(List.of(rule, otherRule));
            when(rule.getNextExecutionDate()).thenReturn(null);
            when(otherRule.getNextExecutionDate()).thenReturn(DATE_1);
            when(otherRule.getPeriodType()).thenReturn(PERIOD_TYPE);
            when(otherRule.getEndDate()).thenReturn(null);

            List<RecurringOccurrenceDto> result = service.getUpcomingOccurrences(USER_ID, DATE_1, DATE_2);

            assertEquals(List.of(DATE_1, DATE_2), result.stream().map(RecurringOccurrenceDto::date).toList());
        }

        @Test
        void shouldLimitOccurrencesToMaximumPerRuleWhenRangeIsVeryLarge() {
            ReflectionTestUtils.setField(service, "maxRangeDay", Long.MAX_VALUE);
            when(recurringSettingsRepository.findAllEnabledByUserId(USER_ID)).thenReturn(List.of(rule));
            when(rule.getNextExecutionDate()).thenReturn(DATE_1);
            when(rule.getPeriodType()).thenReturn(PERIOD_TYPE);
            when(rule.getEndDate()).thenReturn(null);

            List<RecurringOccurrenceDto> result = service.getUpcomingOccurrences(USER_ID, DATE_1, LocalDate.of(9999, 12, 31));

            assertEquals(1000, result.size());
        }

        @Test
        void shouldAcceptRangeWhenFromEqualsTo() {
            when(recurringSettingsRepository.findAllEnabledByUserId(USER_ID)).thenReturn(List.of());

            List<RecurringOccurrenceDto> result = service.getUpcomingOccurrences(USER_ID, DATE_1, DATE_1);

            assertTrue(result.isEmpty());
        }

        @Test
        void shouldAcceptRangeWhenRangeEqualsMaxRangeDays() {
            when(recurringSettingsRepository.findAllEnabledByUserId(USER_ID)).thenReturn(List.of());

            List<RecurringOccurrenceDto> result = service.getUpcomingOccurrences(USER_ID, DATE_1, DATE_1.plusDays(MAX_RANGE_DAYS));

            assertTrue(result.isEmpty());
        }

        @Test
        void shouldThrowExceptionWhenFromIsAfterTo() {
            assertThrows(InvalidInputException.class, () -> service.getUpcomingOccurrences(USER_ID, DATE_1.plusDays(1), DATE_1));

            verifyNoInteractions(recurringSettingsRepository);
        }

        @Test
        void shouldThrowExceptionWhenRangeExceedsMaxRangeDays() {
            assertThrows(InvalidInputException.class, () -> service.getUpcomingOccurrences(USER_ID, DATE_1, DATE_1.plusDays(MAX_RANGE_DAYS + 1)));

            verifyNoInteractions(recurringSettingsRepository);
        }
    }
}