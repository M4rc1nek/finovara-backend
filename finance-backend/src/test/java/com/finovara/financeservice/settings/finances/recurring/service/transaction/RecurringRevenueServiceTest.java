package com.finovara.financeservice.settings.finances.recurring.service.transaction;

import com.finovara.contracts.exception.badrequest.InvalidInputException;
import com.finovara.contracts.mainaccount.activity.model.SettingType;
import com.finovara.contracts.user.authorization.additionalcode.resolver.AdditionalAuthorizationCodeResolver;
import com.finovara.contracts.util.PeriodType;
import com.finovara.contracts.util.model.RecurringType;
import com.finovara.financeservice.feignclient.AuthBackendClient;
import com.finovara.financeservice.settings.finances.recurring.dto.RecurringCommonFields;
import com.finovara.financeservice.settings.finances.recurring.dto.RecurringRevenueDto;
import com.finovara.financeservice.settings.finances.recurring.model.RecurringSettings;
import com.finovara.financeservice.settings.finances.recurring.service.support.RecurringSettingsSupport;
import com.finovara.financeservice.settings.finances.recurring.service.validator.RecurringRevenueValidator;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecurringRevenueServiceTest {

    private static final Long USER_ID = 1L;
    private static final BigDecimal AMOUNT = BigDecimal.valueOf(5000);
    private static final PeriodType PERIOD_TYPE = PeriodType.values()[0];
    private static final LocalDate START_DATE = LocalDate.of(2025, 1, 1);
    private static final LocalDate END_DATE = LocalDate.of(2025, 12, 31);
    private static final LocalDate NEXT_EXECUTION_DATE = LocalDate.of(2025, 2, 1);

    @Mock
    private RecurringSettingsSupport recurringSettingsSupport;

    @Mock
    private RecurringRevenueValidator recurringRevenueValidator;

    @Mock
    private AuthBackendClient authBackendClient;

    @Mock
    private AdditionalAuthorizationCodeResolver additionalAuthorizationCodeResolver;

    @Mock
    private RecurringRevenueDto recurringRevenueDto;

    @Mock
    private RecurringSettings settings;

    @InjectMocks
    private RecurringRevenueService service;

    @Nested
    class SaveRevenueSettings {

        @BeforeEach
        void setUp() {
            when(recurringSettingsSupport.getSettingsForUpdate(USER_ID, RecurringType.REVENUE)).thenReturn(settings);
            when(recurringRevenueDto.enable()).thenReturn(true);
            when(recurringRevenueDto.amount()).thenReturn(AMOUNT);
            when(recurringRevenueDto.periodType()).thenReturn(PERIOD_TYPE);
            when(recurringRevenueDto.startDate()).thenReturn(START_DATE);
            when(recurringRevenueDto.endDate()).thenReturn(END_DATE);
        }

        @Test
        void shouldApplyCommonFieldsWhenSettingsAreDisabled() {
            when(settings.isEnable()).thenReturn(false);
            RecurringCommonFields expectedFields = new RecurringCommonFields(true, AMOUNT, PERIOD_TYPE, START_DATE, END_DATE);

            service.saveRevenueSettings(USER_ID, recurringRevenueDto);

            verify(recurringSettingsSupport).applyCommonFields(USER_ID, settings, expectedFields, SettingType.REVENUE_RECURRING);
        }

        @Test
        void shouldNotValidateWhenSettingsAreDisabled() {
            when(settings.isEnable()).thenReturn(false);

            service.saveRevenueSettings(USER_ID, recurringRevenueDto);

            verifyNoInteractions(recurringRevenueValidator);
        }

        @Test
        void shouldSetRevenueCategoryAndResetOtherTargetsWhenSettingsAreSaved() {
            when(settings.isEnable()).thenReturn(false);

            service.saveRevenueSettings(USER_ID, recurringRevenueDto);

            verify(settings).setRevenueCategory(null);
            verify(settings).setExpenseCategory(null);
            verify(settings).setPiggyBankId(null);
        }

        @Test
        void shouldConfirmAuthorizationCodeBeforeLoadingSettingsWhenSavingSettings() {
            when(settings.isEnable()).thenReturn(false);

            service.saveRevenueSettings(USER_ID, recurringRevenueDto);

            InOrder inOrder = inOrder(authBackendClient, recurringSettingsSupport);
            inOrder.verify(authBackendClient).confirmAuthorizationCode(eq(USER_ID), any());
            inOrder.verify(recurringSettingsSupport).getSettingsForUpdate(USER_ID, RecurringType.REVENUE);
        }

        @Test
        void shouldValidateSettingsWhenSettingsAreEnabled() {
            when(settings.isEnable()).thenReturn(true);

            service.saveRevenueSettings(USER_ID, recurringRevenueDto);

            verify(recurringRevenueValidator).validate(settings);
        }

        @Test
        void shouldThrowExceptionWhenValidationFails() {
            when(settings.isEnable()).thenReturn(true);
            doThrow(new InvalidInputException("invalid")).when(recurringRevenueValidator).validate(settings);

            assertThrows(InvalidInputException.class, () -> service.saveRevenueSettings(USER_ID, recurringRevenueDto));
        }
    }

    @Nested
    class SaveRevenueSettingsFailures {

        @Test
        void shouldThrowExceptionWhenAuthorizationCodeConfirmationFails() {
            doThrow(new IllegalStateException("denied")).when(authBackendClient).confirmAuthorizationCode(eq(USER_ID), any());

            assertThrows(IllegalStateException.class, () -> service.saveRevenueSettings(USER_ID, recurringRevenueDto));

            verifyNoInteractions(recurringSettingsSupport, recurringRevenueValidator);
        }

        @Test
        void shouldThrowExceptionWhenSettingsDoNotExist() {
            when(recurringSettingsSupport.getSettingsForUpdate(USER_ID, RecurringType.REVENUE)).thenThrow(new EntityNotFoundException("not found"));

            assertThrows(EntityNotFoundException.class, () -> service.saveRevenueSettings(USER_ID, recurringRevenueDto));

            verify(recurringSettingsSupport, never()).applyCommonFields(any(), any(), any(), any());
            verifyNoInteractions(recurringRevenueValidator);
        }
    }

    @Nested
    class GetRevenueSettings {

        @Test
        void shouldReturnDtoMappedFromSettingsWhenSettingsExist() {
            when(recurringSettingsSupport.getSettings(USER_ID, RecurringType.REVENUE)).thenReturn(settings);
            when(settings.isEnable()).thenReturn(true);
            when(settings.getAmount()).thenReturn(AMOUNT);
            when(settings.getPeriodType()).thenReturn(PERIOD_TYPE);
            when(settings.getStartDate()).thenReturn(START_DATE);
            when(settings.getEndDate()).thenReturn(END_DATE);
            when(settings.getNextExecutionDate()).thenReturn(NEXT_EXECUTION_DATE);

            RecurringRevenueDto result = service.getRevenueSettings(USER_ID);

            assertEquals(new RecurringRevenueDto(true, AMOUNT, null, PERIOD_TYPE, START_DATE, END_DATE, NEXT_EXECUTION_DATE, null), result);
        }

        @Test
        void shouldReturnDisabledDtoWhenSettingsAreDisabled() {
            when(recurringSettingsSupport.getSettings(USER_ID, RecurringType.REVENUE)).thenReturn(settings);
            when(settings.isEnable()).thenReturn(false);

            RecurringRevenueDto result = service.getRevenueSettings(USER_ID);

            assertEquals(new RecurringRevenueDto(false, null, null, null, null, null, null, null), result);
        }

        @Test
        void shouldThrowExceptionWhenSettingsDoNotExist() {
            when(recurringSettingsSupport.getSettings(USER_ID, RecurringType.REVENUE)).thenThrow(new EntityNotFoundException("not found"));

            assertThrows(EntityNotFoundException.class, () -> service.getRevenueSettings(USER_ID));
        }
    }
}