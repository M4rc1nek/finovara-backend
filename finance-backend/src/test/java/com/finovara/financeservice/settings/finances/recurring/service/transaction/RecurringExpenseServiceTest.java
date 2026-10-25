package com.finovara.financeservice.settings.finances.recurring.service.transaction;

import com.finovara.contracts.exception.badrequest.InvalidInputException;
import com.finovara.contracts.mainaccount.activity.model.SettingType;
import com.finovara.contracts.user.authorization.additionalcode.resolver.AdditionalAuthorizationCodeResolver;
import com.finovara.contracts.util.PeriodType;
import com.finovara.contracts.util.model.RecurringType;
import com.finovara.financeservice.feignclient.AuthBackendClient;
import com.finovara.financeservice.limit.model.Limit;
import com.finovara.financeservice.settings.finances.expense.model.ExpenseSettings;
import com.finovara.financeservice.settings.finances.recurring.dto.RecurringCommonFields;
import com.finovara.financeservice.settings.finances.recurring.dto.RecurringExpenseDto;
import com.finovara.financeservice.settings.finances.recurring.model.RecurringSettings;
import com.finovara.financeservice.settings.finances.recurring.service.support.RecurringSettingsSupport;
import com.finovara.financeservice.settings.finances.recurring.service.validator.ExpenseSettingsValidator;
import com.finovara.financeservice.util.limit.manager.LimitManagerService;
import com.finovara.financeservice.util.settings.SettingsManager;
import com.finovara.financeservice.util.wallet.WalletManagerService;
import com.finovara.financeservice.wallet.model.Wallet;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecurringExpenseServiceTest {

    private static final Long USER_ID = 1L;
    private static final BigDecimal AMOUNT = BigDecimal.valueOf(250);
    private static final PeriodType PERIOD_TYPE = PeriodType.values()[0];
    private static final LocalDate START_DATE = LocalDate.of(2025, 1, 1);
    private static final LocalDate END_DATE = LocalDate.of(2025, 12, 31);
    private static final LocalDate NEXT_EXECUTION_DATE = LocalDate.of(2025, 2, 1);

    @Mock
    private RecurringSettingsSupport recurringSettingsSupport;

    @Mock
    private ExpenseSettingsValidator expenseSettingsValidator;

    @Mock
    private SettingsManager settingsManager;

    @Mock
    private WalletManagerService walletManagerService;

    @Mock
    private LimitManagerService limitManagerService;

    @Mock
    private AuthBackendClient authBackendClient;

    @Mock
    private AdditionalAuthorizationCodeResolver additionalAuthorizationCodeResolver;

    @Mock
    private RecurringExpenseDto recurringExpenseDto;

    @Mock
    private RecurringSettings settings;

    @Mock
    private ExpenseSettings expenseSettings;

    @Mock
    private Wallet wallet;

    @Mock
    private Limit limit;

    @InjectMocks
    private RecurringExpenseService service;

    @Nested
    class SaveExpenseSettings {

        @BeforeEach
        void setUp() {
            when(recurringSettingsSupport.getSettingsForUpdate(USER_ID, RecurringType.EXPENSE)).thenReturn(settings);
            when(recurringExpenseDto.enable()).thenReturn(true);
            when(recurringExpenseDto.amount()).thenReturn(AMOUNT);
            when(recurringExpenseDto.periodType()).thenReturn(PERIOD_TYPE);
            when(recurringExpenseDto.startDate()).thenReturn(START_DATE);
            when(recurringExpenseDto.endDate()).thenReturn(END_DATE);
        }

        @Test
        void shouldApplyCommonFieldsWhenSettingsAreDisabled() {
            when(settings.isEnable()).thenReturn(false);
            RecurringCommonFields expectedFields = new RecurringCommonFields(true, AMOUNT, PERIOD_TYPE, START_DATE, END_DATE);

            service.saveExpenseSettings(USER_ID, recurringExpenseDto);

            verify(recurringSettingsSupport).applyCommonFields(USER_ID, settings, expectedFields, SettingType.EXPENSE_RECURRING);
        }

        @Test
        void shouldNotValidateWhenSettingsAreDisabled() {
            when(settings.isEnable()).thenReturn(false);

            service.saveExpenseSettings(USER_ID, recurringExpenseDto);

            verifyNoInteractions(expenseSettingsValidator, settingsManager, walletManagerService, limitManagerService);
        }

        @Test
        void shouldSetExpenseCategoryAndResetOtherTargetsWhenSettingsAreSaved() {
            when(settings.isEnable()).thenReturn(false);

            service.saveExpenseSettings(USER_ID, recurringExpenseDto);

            verify(settings).setExpenseCategory(null);
            verify(settings).setRevenueCategory(null);
            verify(settings).setPiggyBankId(null);
        }

        @Test
        void shouldConfirmAuthorizationCodeBeforeLoadingSettingsWhenSavingSettings() {
            when(settings.isEnable()).thenReturn(false);

            service.saveExpenseSettings(USER_ID, recurringExpenseDto);

            InOrder inOrder = inOrder(authBackendClient, recurringSettingsSupport);
            inOrder.verify(authBackendClient).confirmAuthorizationCode(eq(USER_ID), any());
            inOrder.verify(recurringSettingsSupport).getSettingsForUpdate(USER_ID, RecurringType.EXPENSE);
        }

        @Test
        void shouldValidateWithExpenseSettingsWalletAndLimitsWhenSettingsAreEnabled() {
            List<Limit> limits = List.of(limit);
            when(settings.isEnable()).thenReturn(true);
            when(settingsManager.getExpenseSettingsByUserId(USER_ID)).thenReturn(expenseSettings);
            when(walletManagerService.getWalletByUserIdOrThrow(USER_ID)).thenReturn(wallet);
            when(limitManagerService.getLimitsByUserId(USER_ID)).thenReturn(limits);

            service.saveExpenseSettings(USER_ID, recurringExpenseDto);

            verify(expenseSettingsValidator).validate(settings, expenseSettings, wallet, limits);
        }

        @Test
        void shouldThrowExceptionWhenValidationFails() {
            List<Limit> limits = List.of(limit);
            when(settings.isEnable()).thenReturn(true);
            when(settingsManager.getExpenseSettingsByUserId(USER_ID)).thenReturn(expenseSettings);
            when(walletManagerService.getWalletByUserIdOrThrow(USER_ID)).thenReturn(wallet);
            when(limitManagerService.getLimitsByUserId(USER_ID)).thenReturn(limits);
            doThrow(new InvalidInputException("invalid")).when(expenseSettingsValidator).validate(settings, expenseSettings, wallet, limits);

            assertThrows(InvalidInputException.class, () -> service.saveExpenseSettings(USER_ID, recurringExpenseDto));
        }
    }

    @Nested
    class SaveExpenseSettingsFailures {

        @Test
        void shouldThrowExceptionWhenAuthorizationCodeConfirmationFails() {
            doThrow(new IllegalStateException("denied")).when(authBackendClient).confirmAuthorizationCode(eq(USER_ID), any());

            assertThrows(IllegalStateException.class, () -> service.saveExpenseSettings(USER_ID, recurringExpenseDto));

            verifyNoInteractions(recurringSettingsSupport, expenseSettingsValidator);
        }

        @Test
        void shouldThrowExceptionWhenSettingsDoNotExist() {
            when(recurringSettingsSupport.getSettingsForUpdate(USER_ID, RecurringType.EXPENSE)).thenThrow(new EntityNotFoundException("not found"));

            assertThrows(EntityNotFoundException.class, () -> service.saveExpenseSettings(USER_ID, recurringExpenseDto));

            verify(recurringSettingsSupport, never()).applyCommonFields(any(), any(), any(), any());
            verifyNoInteractions(expenseSettingsValidator);
        }
    }

    @Nested
    class GetExpenseSettings {

        @Test
        void shouldReturnDtoMappedFromSettingsWhenSettingsExist() {
            when(recurringSettingsSupport.getSettings(USER_ID, RecurringType.EXPENSE)).thenReturn(settings);
            when(settings.isEnable()).thenReturn(true);
            when(settings.getAmount()).thenReturn(AMOUNT);
            when(settings.getPeriodType()).thenReturn(PERIOD_TYPE);
            when(settings.getStartDate()).thenReturn(START_DATE);
            when(settings.getEndDate()).thenReturn(END_DATE);
            when(settings.getNextExecutionDate()).thenReturn(NEXT_EXECUTION_DATE);

            RecurringExpenseDto result = service.getExpenseSettings(USER_ID);

            assertEquals(new RecurringExpenseDto(true, AMOUNT, null, PERIOD_TYPE, START_DATE, END_DATE, NEXT_EXECUTION_DATE, null), result);
        }

        @Test
        void shouldReturnDisabledDtoWhenSettingsAreDisabled() {
            when(recurringSettingsSupport.getSettings(USER_ID, RecurringType.EXPENSE)).thenReturn(settings);
            when(settings.isEnable()).thenReturn(false);

            RecurringExpenseDto result = service.getExpenseSettings(USER_ID);

            assertEquals(new RecurringExpenseDto(false, null, null, null, null, null, null, null), result);
        }

        @Test
        void shouldThrowExceptionWhenSettingsDoNotExist() {
            when(recurringSettingsSupport.getSettings(USER_ID, RecurringType.EXPENSE)).thenThrow(new EntityNotFoundException("not found"));

            assertThrows(EntityNotFoundException.class, () -> service.getExpenseSettings(USER_ID));
        }
    }
}