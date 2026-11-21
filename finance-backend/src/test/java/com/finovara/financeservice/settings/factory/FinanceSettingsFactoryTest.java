package com.finovara.financeservice.settings.factory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.finovara.contracts.exception.notfound.RequestedEntityNotFoundException;
import com.finovara.contracts.util.PeriodType;
import com.finovara.contracts.util.model.RecurringType;
import com.finovara.financeservice.settings.finances.expense.model.ExpenseSettings;
import com.finovara.financeservice.settings.finances.expense.repository.ExpenseSettingsRepository;
import com.finovara.financeservice.settings.finances.recurring.model.RecurringSettings;
import com.finovara.financeservice.settings.finances.recurring.repository.RecurringSettingsRepository;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FinanceSettingsFactoryTest {

    private static final Long USER_ID = 1L;
    private static final RecurringType FIRST_TYPE = RecurringType.values()[0];

    @Mock
    private ExpenseSettingsRepository expenseSettingsRepository;
    @Mock
    private RecurringSettingsRepository recurringSettingsRepository;
    @Mock
    private ExpenseSettings expenseSettings;
    @Mock
    private RecurringSettings firstRecurringSettings;
    @Mock
    private RecurringSettings secondRecurringSettings;

    @InjectMocks
    private FinanceSettingsFactory factory;

    @Nested
    class CreateDefaultSettingsIfNotExist {

        @Nested
        class WhenNoSettingsExist {

            @BeforeEach
            void setUp() {
                when(expenseSettingsRepository.existsByUserId(USER_ID)).thenReturn(false);
                when(recurringSettingsRepository.findAllByUserId(USER_ID)).thenReturn(List.of());
            }

            @Test
            void shouldSaveExpenseSettingsWithDefaultValuesWhenNoSettingsExist() {
                ArgumentCaptor<ExpenseSettings> captor = ArgumentCaptor.forClass(ExpenseSettings.class);

                factory.createDefaultSettingsIfNotExist(USER_ID);

                verify(expenseSettingsRepository).save(captor.capture());
                ExpenseSettings captured = captor.getValue();
                assertEquals(USER_ID, captured.getUserId());
                assertFalse(captured.isAmountThresholdEnabled());
                assertEquals(0, BigDecimal.ZERO.compareTo(captured.getBlockedAmount()));
                assertFalse(captured.isSmartScanEnabled());
                assertFalse(captured.isCountQuantityLimitEnabled());
                assertEquals(1, captured.getNumberOfQuantityLimit());
                assertEquals(PeriodType.DAILY, captured.getPeriodType());
                assertFalse(captured.isQuantityLimitEmergencyModeEnabled());
            }

            @Test
            void shouldSaveRecurringSettingsForEveryTypeWhenNoSettingsExist() {
                ArgumentCaptor<List<RecurringSettings>> captor = ArgumentCaptor.forClass(List.class);

                factory.createDefaultSettingsIfNotExist(USER_ID);

                verify(recurringSettingsRepository).saveAll(captor.capture());
                Set<RecurringType> savedTypes = captor.getValue().stream()
                        .map(RecurringSettings::getType)
                        .collect(Collectors.toSet());
                assertEquals(Set.copyOf(Arrays.asList(RecurringType.values())), savedTypes);
            }

            @Test
            void shouldSaveRecurringSettingsWithDefaultValuesWhenNoSettingsExist() {
                ArgumentCaptor<List<RecurringSettings>> captor = ArgumentCaptor.forClass(List.class);

                factory.createDefaultSettingsIfNotExist(USER_ID);

                verify(recurringSettingsRepository).saveAll(captor.capture());
                captor.getValue().forEach(saved -> {
                    assertEquals(USER_ID, saved.getUserId());
                    assertFalse(saved.isEnable());
                    assertEquals(0, BigDecimal.ONE.compareTo(saved.getAmount()));
                    assertNull(saved.getRevenueCategory());
                    assertNull(saved.getExpenseCategory());
                    assertNull(saved.getPiggyBankId());
                    assertEquals(PeriodType.MONTHLY, saved.getPeriodType());
                    assertNull(saved.getStartDate());
                    assertNull(saved.getNextExecutionDate());
                    assertNotNull(saved.getCreatedAt());
                });
            }
        }

        @Nested
        class WhenExpenseSettingsAlreadyExist {

            @BeforeEach
            void setUp() {
                when(expenseSettingsRepository.existsByUserId(USER_ID)).thenReturn(true);
                when(recurringSettingsRepository.findAllByUserId(USER_ID)).thenReturn(List.of());
            }

            @Test
            void shouldNotSaveExpenseSettingsWhenExpenseSettingsAlreadyExist() {
                factory.createDefaultSettingsIfNotExist(USER_ID);

                verify(expenseSettingsRepository, never()).save(any(ExpenseSettings.class));
            }

            @Test
            void shouldStillSaveRecurringSettingsWhenExpenseSettingsAlreadyExist() {
                factory.createDefaultSettingsIfNotExist(USER_ID);

                verify(recurringSettingsRepository).saveAll(any());
            }
        }

        @Nested
        class WhenRecurringSettingsAlreadyExist {

            @BeforeEach
            void setUp() {
                when(expenseSettingsRepository.existsByUserId(USER_ID)).thenReturn(false);
            }

            @Test
            void shouldNotSaveRecurringSettingsWhenAllTypesAlreadyExist() {
                when(recurringSettingsRepository.findAllByUserId(USER_ID)).thenReturn(allRecurringSettings());

                factory.createDefaultSettingsIfNotExist(USER_ID);

                verify(recurringSettingsRepository, never()).saveAll(any());
            }

            @Test
            void shouldStillSaveExpenseSettingsWhenAllRecurringTypesAlreadyExist() {
                when(recurringSettingsRepository.findAllByUserId(USER_ID)).thenReturn(allRecurringSettings());

                factory.createDefaultSettingsIfNotExist(USER_ID);

                verify(expenseSettingsRepository).save(any(ExpenseSettings.class));
            }

            @Test
            void shouldSaveOnlyMissingRecurringTypeWhenOnlyOneTypeIsMissing() {
                ArgumentCaptor<List<RecurringSettings>> captor = ArgumentCaptor.forClass(List.class);
                when(recurringSettingsRepository.findAllByUserId(USER_ID)).thenReturn(
                        allRecurringSettings().stream()
                                .filter(settings -> settings.getType() != FIRST_TYPE)
                                .toList());

                factory.createDefaultSettingsIfNotExist(USER_ID);

                verify(recurringSettingsRepository).saveAll(captor.capture());
                assertEquals(List.of(FIRST_TYPE), captor.getValue().stream().map(RecurringSettings::getType).toList());
            }

            @Test
            void shouldSkipOnlyExistingRecurringTypeWhenOnlyOneTypeExists() {
                ArgumentCaptor<List<RecurringSettings>> captor = ArgumentCaptor.forClass(List.class);
                when(recurringSettingsRepository.findAllByUserId(USER_ID)).thenReturn(
                        List.of(RecurringSettings.builder().type(FIRST_TYPE).build()));

                factory.createDefaultSettingsIfNotExist(USER_ID);

                verify(recurringSettingsRepository).saveAll(captor.capture());
                assertEquals(RecurringType.values().length - 1, captor.getValue().size());
                assertFalse(captor.getValue().stream().anyMatch(saved -> saved.getType() == FIRST_TYPE));
            }
        }

        @Test
        void shouldNotSaveAnythingWhenAllSettingsAlreadyExist() {
            when(expenseSettingsRepository.existsByUserId(USER_ID)).thenReturn(true);
            when(recurringSettingsRepository.findAllByUserId(USER_ID)).thenReturn(allRecurringSettings());

            factory.createDefaultSettingsIfNotExist(USER_ID);

            verify(expenseSettingsRepository, never()).save(any(ExpenseSettings.class));
            verify(recurringSettingsRepository, never()).saveAll(any());
        }
    }

    @Nested
    class DeleteSettingsIfExist {

        @Nested
        class WhenBothSettingsExist {

            @BeforeEach
            void setUp() {
                when(recurringSettingsRepository.findAllByUserId(USER_ID)).thenReturn(List.of(firstRecurringSettings));
                when(expenseSettingsRepository.existsByUserId(USER_ID)).thenReturn(true);
                when(expenseSettingsRepository.findByUserId(USER_ID)).thenReturn(Optional.of(expenseSettings));
            }

            @Test
            void shouldDeleteAllRecurringSettingsWhenBothSettingsExist() {
                List<RecurringSettings> recurringSettings = List.of(firstRecurringSettings, secondRecurringSettings);
                when(recurringSettingsRepository.findAllByUserId(USER_ID)).thenReturn(recurringSettings);

                factory.deleteSettingsIfExist(USER_ID);

                verify(recurringSettingsRepository).deleteAll(recurringSettings);
            }

            @Test
            void shouldDeleteExpenseSettingsWhenBothSettingsExist() {
                factory.deleteSettingsIfExist(USER_ID);

                verify(expenseSettingsRepository).delete(expenseSettings);
            }

        }

        @Test
        void shouldNotDeleteAnythingWhenRecurringSettingsDoNotExist() {
            when(recurringSettingsRepository.findAllByUserId(USER_ID)).thenReturn(List.of());

            factory.deleteSettingsIfExist(USER_ID);

            verify(recurringSettingsRepository, never()).deleteAll(any());
            verify(expenseSettingsRepository, never()).delete(any(ExpenseSettings.class));
            verifyNoMoreInteractions(expenseSettingsRepository);
        }

        @Test
        void shouldNotDeleteAnythingWhenExpenseSettingsDoNotExist() {
            when(recurringSettingsRepository.findAllByUserId(USER_ID)).thenReturn(List.of(firstRecurringSettings));
            when(expenseSettingsRepository.existsByUserId(USER_ID)).thenReturn(false);

            factory.deleteSettingsIfExist(USER_ID);

            verify(recurringSettingsRepository, never()).deleteAll(any());
            verify(expenseSettingsRepository, never()).delete(any(ExpenseSettings.class));
        }

        @Test
        void shouldDeleteWhenOnlyLastRecurringTypeExists() {
            when(recurringSettingsRepository.findAllByUserId(USER_ID)).thenReturn(List.of(firstRecurringSettings));
            when(expenseSettingsRepository.existsByUserId(USER_ID)).thenReturn(true);
            when(expenseSettingsRepository.findByUserId(USER_ID)).thenReturn(Optional.of(expenseSettings));

            factory.deleteSettingsIfExist(USER_ID);

            verify(recurringSettingsRepository).deleteAll(List.of(firstRecurringSettings));
            verify(expenseSettingsRepository).delete(expenseSettings);
        }

        @Test
        void shouldThrowExceptionWhenExpenseSettingsExistButCannotBeFound() {
            when(recurringSettingsRepository.findAllByUserId(USER_ID)).thenReturn(List.of(firstRecurringSettings));
            when(expenseSettingsRepository.existsByUserId(USER_ID)).thenReturn(true);
            when(expenseSettingsRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

            assertThrows(RequestedEntityNotFoundException.class, () -> factory.deleteSettingsIfExist(USER_ID));

            verify(recurringSettingsRepository, never()).deleteAll(any());
            verify(expenseSettingsRepository, never()).delete(any(ExpenseSettings.class));
        }
    }

    private List<RecurringSettings> allRecurringSettings() {
        return Arrays.stream(RecurringType.values())
                .map(type -> RecurringSettings.builder().type(type).build())
                .toList();
    }
}