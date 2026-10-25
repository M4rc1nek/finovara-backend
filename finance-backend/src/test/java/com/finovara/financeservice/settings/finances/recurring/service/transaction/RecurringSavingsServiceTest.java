package com.finovara.financeservice.settings.finances.recurring.service.transaction;

import com.finovara.contracts.exception.badrequest.InvalidInputException;
import com.finovara.contracts.mainaccount.activity.model.SettingType;
import com.finovara.contracts.user.authorization.additionalcode.resolver.AdditionalAuthorizationCodeResolver;
import com.finovara.contracts.util.PeriodType;
import com.finovara.contracts.util.model.RecurringType;
import com.finovara.financeservice.feignclient.AuthBackendClient;
import com.finovara.financeservice.settings.finances.recurring.dto.RecurringCommonFields;
import com.finovara.financeservice.settings.finances.recurring.dto.RecurringSavingsDto;
import com.finovara.financeservice.settings.finances.recurring.model.RecurringSettings;
import com.finovara.financeservice.settings.finances.recurring.service.support.RecurringSettingsSupport;
import com.finovara.financeservice.settings.finances.recurring.service.validator.RecurringSavingsValidator;
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
class RecurringSavingsServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long PIGGY_BANK_ID = 7L;
    private static final BigDecimal AMOUNT = BigDecimal.valueOf(300);
    private static final PeriodType PERIOD_TYPE = PeriodType.values()[0];
    private static final LocalDate START_DATE = LocalDate.of(2025, 1, 1);
    private static final LocalDate END_DATE = LocalDate.of(2025, 12, 31);
    private static final LocalDate NEXT_EXECUTION_DATE = LocalDate.of(2025, 2, 1);

    @Mock
    private RecurringSettingsSupport recurringSettingsSupport;

    @Mock
    private RecurringSavingsValidator recurringSavingsValidator;

    @Mock
    private WalletManagerService walletManagerService;

    @Mock
    private AuthBackendClient authBackendClient;

    @Mock
    private AdditionalAuthorizationCodeResolver additionalAuthorizationCodeResolver;

    @Mock
    private RecurringSavingsDto recurringSavingsDto;

    @Mock
    private RecurringSettings settings;

    @Mock
    private Wallet wallet;

    @InjectMocks
    private RecurringSavingsService service;

    @Nested
    class SaveSavingsSettings {

        @BeforeEach
        void setUp() {
            when(recurringSettingsSupport.getSettingsForUpdate(USER_ID, RecurringType.SAVINGS)).thenReturn(settings);
            when(recurringSavingsDto.piggyBankId()).thenReturn(PIGGY_BANK_ID);
            when(recurringSavingsDto.enable()).thenReturn(true);
            when(recurringSavingsDto.amount()).thenReturn(AMOUNT);
            when(recurringSavingsDto.periodType()).thenReturn(PERIOD_TYPE);
            when(recurringSavingsDto.startDate()).thenReturn(START_DATE);
            when(recurringSavingsDto.endDate()).thenReturn(END_DATE);
        }

        @Test
        void shouldApplyCommonFieldsWhenSettingsAreDisabled() {
            when(settings.isEnable()).thenReturn(false);
            RecurringCommonFields expectedFields = new RecurringCommonFields(true, AMOUNT, PERIOD_TYPE, START_DATE, END_DATE);

            service.saveSavingsSettings(USER_ID, recurringSavingsDto);

            verify(recurringSettingsSupport).applyCommonFields(USER_ID, settings, expectedFields, SettingType.SAVINGS_RECURRING);
        }

        @Test
        void shouldNotValidateWhenSettingsAreDisabled() {
            when(settings.isEnable()).thenReturn(false);

            service.saveSavingsSettings(USER_ID, recurringSavingsDto);

            verifyNoInteractions(recurringSavingsValidator, walletManagerService);
        }

        @Test
        void shouldSetPiggyBankAndResetCategoriesWhenSettingsAreSaved() {
            when(settings.isEnable()).thenReturn(false);

            service.saveSavingsSettings(USER_ID, recurringSavingsDto);

            verify(settings).setPiggyBankId(PIGGY_BANK_ID);
            verify(settings).setRevenueCategory(null);
            verify(settings).setExpenseCategory(null);
        }

        @Test
        void shouldConfirmAuthorizationCodeBeforeLoadingSettingsWhenSavingSettings() {
            when(settings.isEnable()).thenReturn(false);

            service.saveSavingsSettings(USER_ID, recurringSavingsDto);

            InOrder inOrder = inOrder(authBackendClient, recurringSettingsSupport);
            inOrder.verify(authBackendClient).confirmAuthorizationCode(eq(USER_ID), any());
            inOrder.verify(recurringSettingsSupport).getSettingsForUpdate(USER_ID, RecurringType.SAVINGS);
        }

        @Test
        void shouldValidateWithWalletWhenSettingsAreEnabled() {
            when(settings.isEnable()).thenReturn(true);
            when(walletManagerService.getWalletByUserIdOrThrow(USER_ID)).thenReturn(wallet);

            service.saveSavingsSettings(USER_ID, recurringSavingsDto);

            verify(recurringSavingsValidator).validate(settings, wallet);
        }

        @Test
        void shouldThrowExceptionWhenValidationFails() {
            when(settings.isEnable()).thenReturn(true);
            when(walletManagerService.getWalletByUserIdOrThrow(USER_ID)).thenReturn(wallet);
            doThrow(new InvalidInputException("invalid")).when(recurringSavingsValidator).validate(settings, wallet);

            assertThrows(InvalidInputException.class, () -> service.saveSavingsSettings(USER_ID, recurringSavingsDto));
        }
    }

    @Nested
    class SaveSavingsSettingsFailures {

        @Test
        void shouldThrowExceptionWhenAuthorizationCodeConfirmationFails() {
            doThrow(new IllegalStateException("denied")).when(authBackendClient).confirmAuthorizationCode(eq(USER_ID), any());

            assertThrows(IllegalStateException.class, () -> service.saveSavingsSettings(USER_ID, recurringSavingsDto));

            verifyNoInteractions(recurringSettingsSupport, recurringSavingsValidator, walletManagerService);
        }

        @Test
        void shouldThrowExceptionWhenSettingsDoNotExist() {
            when(recurringSettingsSupport.getSettingsForUpdate(USER_ID, RecurringType.SAVINGS)).thenThrow(new EntityNotFoundException("not found"));

            assertThrows(EntityNotFoundException.class, () -> service.saveSavingsSettings(USER_ID, recurringSavingsDto));

            verify(recurringSettingsSupport, never()).applyCommonFields(any(), any(), any(), any());
            verifyNoInteractions(recurringSavingsValidator, walletManagerService);
        }
    }

    @Nested
    class GetSavingsSettings {

        @Test
        void shouldReturnDtoMappedFromSettingsWhenSettingsExist() {
            when(recurringSettingsSupport.getSettings(USER_ID, RecurringType.SAVINGS)).thenReturn(settings);
            when(settings.isEnable()).thenReturn(true);
            when(settings.getAmount()).thenReturn(AMOUNT);
            when(settings.getPiggyBankId()).thenReturn(PIGGY_BANK_ID);
            when(settings.getPeriodType()).thenReturn(PERIOD_TYPE);
            when(settings.getStartDate()).thenReturn(START_DATE);
            when(settings.getEndDate()).thenReturn(END_DATE);
            when(settings.getNextExecutionDate()).thenReturn(NEXT_EXECUTION_DATE);

            RecurringSavingsDto result = service.getSavingsSettings(USER_ID);

            assertEquals(new RecurringSavingsDto(true, AMOUNT, PIGGY_BANK_ID, PERIOD_TYPE, START_DATE, END_DATE, NEXT_EXECUTION_DATE, null), result);
        }

        @Test
        void shouldReturnDisabledDtoWhenSettingsAreDisabled() {
            when(recurringSettingsSupport.getSettings(USER_ID, RecurringType.SAVINGS)).thenReturn(settings);
            when(settings.isEnable()).thenReturn(false);
            when(settings.getPiggyBankId()).thenReturn(null);

            RecurringSavingsDto result = service.getSavingsSettings(USER_ID);

            assertEquals(new RecurringSavingsDto(false, null, null, null, null, null, null, null), result);
        }

        @Test
        void shouldThrowExceptionWhenSettingsDoNotExist() {
            when(recurringSettingsSupport.getSettings(USER_ID, RecurringType.SAVINGS)).thenThrow(new EntityNotFoundException("not found"));

            assertThrows(EntityNotFoundException.class, () -> service.getSavingsSettings(USER_ID));
        }
    }
}