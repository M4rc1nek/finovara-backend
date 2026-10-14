package com.finovara.financeservice.settings.factory;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.util.ReflectionTestUtils.getField;

import com.finovara.contracts.util.PeriodType;
import com.finovara.contracts.util.model.RecurringType;
import com.finovara.financeservice.settings.finances.expense.model.ExpenseSettings;
import com.finovara.financeservice.settings.finances.expense.repository.ExpenseSettingsRepository;
import com.finovara.financeservice.settings.finances.recurring.model.RecurringSettings;
import com.finovara.financeservice.settings.finances.recurring.repository.RecurringSettingsRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@ExtendWith(MockitoExtension.class)
class FinanceSettingsFactoryTest {

    private static final Long USER_ID = 1L;
    private static final int RECURRING_TYPES_COUNT = RecurringType.values().length;
    private static final RecurringType FIRST_TYPE = RecurringType.values()[0];
    private static final RecurringType LAST_TYPE = RecurringType.values()[RECURRING_TYPES_COUNT - 1];

    @Mock
    private ExpenseSettingsRepository expenseSettingsRepository;

    @Mock
    private RecurringSettingsRepository recurringSettingsRepository;

    @Captor
    private ArgumentCaptor<ExpenseSettings> expenseSettingsCaptor;

    @Captor
    private ArgumentCaptor<RecurringSettings> recurringSettingsCaptor;

    @InjectMocks
    private FinanceSettingsFactory factory;

    @Nested
    class CreateDefaultSettingsIfNotExist {

        @Test
        void shouldSaveExpenseSettingsWithDefaultValuesWhenExpenseSettingsDoNotExist() {
            when(expenseSettingsRepository.existsByUserId(USER_ID)).thenReturn(false);
            when(recurringSettingsRepository.existsByUserIdAndType(eq(USER_ID), any(RecurringType.class))).thenReturn(true);

            factory.createDefaultSettingsIfNotExist(USER_ID);

            verify(expenseSettingsRepository).save(expenseSettingsCaptor.capture());
            ExpenseSettings saved = expenseSettingsCaptor.getValue();
            assertEquals(USER_ID, getField(saved, "userId"));
            assertEquals(false, getField(saved, "amountThresholdEnabled"));
            assertEquals(BigDecimal.ZERO, getField(saved, "blockedAmount"));
            assertEquals(false, getField(saved, "smartScanEnabled"));
            assertEquals(false, getField(saved, "countQuantityLimitEnabled"));
            assertEquals(1, getField(saved, "numberOfQuantityLimit"));
            assertEquals(PeriodType.DAILY, getField(saved, "periodType"));
            assertEquals(false, getField(saved, "quantityLimitEmergencyModeEnabled"));
        }

        @Test
        void shouldNotSaveExpenseSettingsWhenExpenseSettingsAlreadyExist() {
            when(expenseSettingsRepository.existsByUserId(USER_ID)).thenReturn(true);
            when(recurringSettingsRepository.existsByUserIdAndType(eq(USER_ID), any(RecurringType.class))).thenReturn(true);

            factory.createDefaultSettingsIfNotExist(USER_ID);

            verify(expenseSettingsRepository, never()).save(any(ExpenseSettings.class));
            verify(expenseSettingsRepository).existsByUserId(USER_ID);
            verifyNoMoreInteractions(expenseSettingsRepository);
        }

        @Test
        void shouldSaveRecurringSettingsForEveryTypeWhenNoneExist() {
            when(expenseSettingsRepository.existsByUserId(USER_ID)).thenReturn(true);
            when(recurringSettingsRepository.existsByUserIdAndType(eq(USER_ID), any(RecurringType.class))).thenReturn(false);

            factory.createDefaultSettingsIfNotExist(USER_ID);

            verify(recurringSettingsRepository, times(RECURRING_TYPES_COUNT)).save(recurringSettingsCaptor.capture());
            List<Object> savedTypes = recurringSettingsCaptor.getAllValues().stream()
                    .map(settings -> getField(settings, "type"))
                    .toList();
            assertEquals(asList(RecurringType.values()), savedTypes);
        }

        @Test
        void shouldSaveRecurringSettingsWithDefaultValuesWhenRecurringSettingsDoNotExist() {
            when(expenseSettingsRepository.existsByUserId(USER_ID)).thenReturn(true);
            when(recurringSettingsRepository.existsByUserIdAndType(eq(USER_ID), any(RecurringType.class))).thenReturn(false);

            factory.createDefaultSettingsIfNotExist(USER_ID);

            verify(recurringSettingsRepository, times(RECURRING_TYPES_COUNT)).save(recurringSettingsCaptor.capture());
            recurringSettingsCaptor.getAllValues().forEach(saved -> {
                assertEquals(USER_ID, getField(saved, "userId"));
                assertEquals(false, getField(saved, "enable"));
                assertEquals(BigDecimal.ONE, getField(saved, "amount"));
                assertNull(getField(saved, "revenueCategory"));
                assertNull(getField(saved, "expenseCategory"));
                assertNull(getField(saved, "piggyBankId"));
                assertEquals(PeriodType.MONTHLY, getField(saved, "periodType"));
                assertNull(getField(saved, "startDate"));
                assertNull(getField(saved, "nextExecutionDate"));
                assertEquals(LocalDate.now(), getField(saved, "createdAt"));
            });
        }

        @Test
        void shouldSkipSavingOnlyForExistingRecurringTypesWhenSomeTypesExist() {
            when(expenseSettingsRepository.existsByUserId(USER_ID)).thenReturn(true);
            when(recurringSettingsRepository.existsByUserIdAndType(eq(USER_ID), any(RecurringType.class)))
                    .thenAnswer(invocation -> invocation.getArgument(1) == FIRST_TYPE);

            factory.createDefaultSettingsIfNotExist(USER_ID);

            verify(recurringSettingsRepository, times(RECURRING_TYPES_COUNT - 1)).save(recurringSettingsCaptor.capture());
            List<Object> savedTypes = recurringSettingsCaptor.getAllValues().stream()
                    .map(settings -> getField(settings, "type"))
                    .toList();
            assertEquals(asList(RecurringType.values()).subList(1, RECURRING_TYPES_COUNT), savedTypes);
        }

        @Test
        void shouldNotSaveRecurringSettingsWhenAllRecurringSettingsAlreadyExist() {
            when(expenseSettingsRepository.existsByUserId(USER_ID)).thenReturn(true);
            when(recurringSettingsRepository.existsByUserIdAndType(eq(USER_ID), any(RecurringType.class))).thenReturn(true);

            factory.createDefaultSettingsIfNotExist(USER_ID);

            verify(recurringSettingsRepository, never()).save(any(RecurringSettings.class));
            verify(recurringSettingsRepository, times(RECURRING_TYPES_COUNT)).existsByUserIdAndType(eq(USER_ID), any(RecurringType.class));
            verifyNoMoreInteractions(recurringSettingsRepository);
        }

        @Test
        void shouldCreateExpenseAndRecurringSettingsWhenNothingExists() {
            when(expenseSettingsRepository.existsByUserId(USER_ID)).thenReturn(false);
            when(recurringSettingsRepository.existsByUserIdAndType(eq(USER_ID), any(RecurringType.class))).thenReturn(false);

            factory.createDefaultSettingsIfNotExist(USER_ID);

            verify(expenseSettingsRepository, times(1)).save(any(ExpenseSettings.class));
            verify(recurringSettingsRepository, times(RECURRING_TYPES_COUNT)).save(any(RecurringSettings.class));
        }

        @Test
        void shouldCheckExistenceForEachRecurringTypeWhenCreatingSettings() {
            when(expenseSettingsRepository.existsByUserId(USER_ID)).thenReturn(true);
            when(recurringSettingsRepository.existsByUserIdAndType(eq(USER_ID), any(RecurringType.class))).thenReturn(true);

            factory.createDefaultSettingsIfNotExist(USER_ID);

            asList(RecurringType.values()).forEach(type ->
                    verify(recurringSettingsRepository).existsByUserIdAndType(USER_ID, type));
        }

        @Test
        void shouldSaveExpenseSettingsWithNullUserIdWhenUserIdIsNull() {
            factory.createDefaultSettingsIfNotExist(null);

            verify(expenseSettingsRepository).save(expenseSettingsCaptor.capture());
            assertNull(getField(expenseSettingsCaptor.getValue(), "userId"));
        }

        @Test
        void shouldThrowExceptionWhenExpenseExistenceCheckFails() {
            when(expenseSettingsRepository.existsByUserId(USER_ID)).thenThrow(new IllegalStateException("db error"));

            assertThrows(IllegalStateException.class, () -> factory.createDefaultSettingsIfNotExist(USER_ID));

            verifyNoInteractions(recurringSettingsRepository);
        }

        @Test
        void shouldThrowExceptionWhenExpenseSettingsSaveFails() {
            when(expenseSettingsRepository.existsByUserId(USER_ID)).thenReturn(false);
            when(expenseSettingsRepository.save(any(ExpenseSettings.class))).thenThrow(new IllegalStateException("save error"));

            assertThrows(IllegalStateException.class, () -> factory.createDefaultSettingsIfNotExist(USER_ID));

            verifyNoInteractions(recurringSettingsRepository);
        }

        @Test
        void shouldThrowExceptionWhenRecurringSettingsSaveFails() {
            when(expenseSettingsRepository.existsByUserId(USER_ID)).thenReturn(true);
            when(recurringSettingsRepository.existsByUserIdAndType(eq(USER_ID), any(RecurringType.class))).thenReturn(false);
            when(recurringSettingsRepository.save(any(RecurringSettings.class))).thenThrow(new IllegalStateException("save error"));

            assertThrows(IllegalStateException.class, () -> factory.createDefaultSettingsIfNotExist(USER_ID));

            verify(recurringSettingsRepository, times(1)).save(any(RecurringSettings.class));
        }
    }

    @Nested
    class DeleteSettingsIfExist {

        @Test
        void shouldDeleteRecurringAndExpenseSettingsWhenBothExist() {
            when(recurringSettingsRepository.existsByUserIdAndType(eq(USER_ID), any(RecurringType.class))).thenReturn(true);
            when(expenseSettingsRepository.existsByUserId(USER_ID)).thenReturn(true);

            factory.deleteSettingsIfExist(USER_ID);

            verify(recurringSettingsRepository).deleteAllByUserId(USER_ID);
            verify(expenseSettingsRepository).deleteByUserId(USER_ID);
        }

        @Test
        void shouldDeleteRecurringSettingsBeforeExpenseSettingsWhenBothExist() {
            when(recurringSettingsRepository.existsByUserIdAndType(eq(USER_ID), any(RecurringType.class))).thenReturn(true);
            when(expenseSettingsRepository.existsByUserId(USER_ID)).thenReturn(true);
            InOrder inOrder = inOrder(recurringSettingsRepository, expenseSettingsRepository);

            factory.deleteSettingsIfExist(USER_ID);

            inOrder.verify(recurringSettingsRepository).deleteAllByUserId(USER_ID);
            inOrder.verify(expenseSettingsRepository).deleteByUserId(USER_ID);
        }

        @Test
        void shouldNotDeleteAnythingWhenRecurringSettingsDoNotExist() {
            factory.deleteSettingsIfExist(USER_ID);

            verify(recurringSettingsRepository, times(RECURRING_TYPES_COUNT)).existsByUserIdAndType(eq(USER_ID), any(RecurringType.class));
            verify(recurringSettingsRepository, never()).deleteAllByUserId(USER_ID);
            verifyNoInteractions(expenseSettingsRepository);
        }

        @Test
        void shouldNotDeleteAnythingWhenExpenseSettingsDoNotExist() {
            when(recurringSettingsRepository.existsByUserIdAndType(eq(USER_ID), any(RecurringType.class))).thenReturn(true);
            when(expenseSettingsRepository.existsByUserId(USER_ID)).thenReturn(false);

            factory.deleteSettingsIfExist(USER_ID);

            verify(recurringSettingsRepository, never()).deleteAllByUserId(USER_ID);
            verify(expenseSettingsRepository, never()).deleteByUserId(USER_ID);
        }

        @Test
        void shouldStopCheckingRecurringTypesWhenFirstTypeExists() {
            when(recurringSettingsRepository.existsByUserIdAndType(USER_ID, FIRST_TYPE)).thenReturn(true);
            when(expenseSettingsRepository.existsByUserId(USER_ID)).thenReturn(true);

            factory.deleteSettingsIfExist(USER_ID);

            verify(recurringSettingsRepository, times(1)).existsByUserIdAndType(eq(USER_ID), any(RecurringType.class));
            verify(recurringSettingsRepository).deleteAllByUserId(USER_ID);
            verify(expenseSettingsRepository).deleteByUserId(USER_ID);
        }

        @Test
        void shouldDeleteSettingsWhenOnlyLastRecurringTypeExists() {
            when(recurringSettingsRepository.existsByUserIdAndType(eq(USER_ID), any(RecurringType.class)))
                    .thenAnswer(invocation -> invocation.getArgument(1) == LAST_TYPE);
            when(expenseSettingsRepository.existsByUserId(USER_ID)).thenReturn(true);

            factory.deleteSettingsIfExist(USER_ID);

            verify(recurringSettingsRepository, times(RECURRING_TYPES_COUNT)).existsByUserIdAndType(eq(USER_ID), any(RecurringType.class));
            verify(recurringSettingsRepository).deleteAllByUserId(USER_ID);
            verify(expenseSettingsRepository).deleteByUserId(USER_ID);
        }

        @Test
        void shouldNotDeleteAnythingWhenUserIdIsNullAndNoSettingsExist() {
            factory.deleteSettingsIfExist(null);

            verify(recurringSettingsRepository, never()).deleteAllByUserId(any());
            verifyNoInteractions(expenseSettingsRepository);
        }

        @Test
        void shouldThrowExceptionWhenRecurringSettingsDeletionFails() {
            when(recurringSettingsRepository.existsByUserIdAndType(eq(USER_ID), any(RecurringType.class))).thenReturn(true);
            when(expenseSettingsRepository.existsByUserId(USER_ID)).thenReturn(true);
            doThrow(new IllegalStateException("delete error")).when(recurringSettingsRepository).deleteAllByUserId(USER_ID);

            assertThrows(IllegalStateException.class, () -> factory.deleteSettingsIfExist(USER_ID));

            verify(expenseSettingsRepository, never()).deleteByUserId(USER_ID);
        }

        @Test
        void shouldThrowExceptionWhenExpenseSettingsDeletionFails() {
            when(recurringSettingsRepository.existsByUserIdAndType(eq(USER_ID), any(RecurringType.class))).thenReturn(true);
            when(expenseSettingsRepository.existsByUserId(USER_ID)).thenReturn(true);
            doThrow(new IllegalStateException("delete error")).when(expenseSettingsRepository).deleteByUserId(USER_ID);

            assertThrows(IllegalStateException.class, () -> factory.deleteSettingsIfExist(USER_ID));

            verify(recurringSettingsRepository).deleteAllByUserId(USER_ID);
        }
    }
}