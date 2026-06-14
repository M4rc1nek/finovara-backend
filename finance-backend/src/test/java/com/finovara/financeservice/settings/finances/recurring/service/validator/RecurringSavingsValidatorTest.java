package com.finovara.authbackend.settings.finances.recurring.service.validator;

import com.finovara.contracts.exception.badrequest.InvalidInputException;
import com.finovara.authbackend.settings.finances.recurring.model.RecurringSettings;
import com.finovara.authbackend.settings.finances.recurring.service.validator.util.RecurringBasicValidator;
import com.finovara.contracts.model.PeriodType;
import com.finovara.authbackend.wallet.model.Wallet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RecurringSavingsValidatorTest {

    @Mock
    private RecurringBasicValidator recurringBasicValidator;

    @InjectMocks
    private RecurringSavingsValidator recurringSavingsValidator;

    private RecurringSettings recurringSettings;
    private Wallet wallet;

    @BeforeEach
    void setUp() {
        recurringSettings = new RecurringSettings();
        recurringSettings.setAmount(BigDecimal.valueOf(100));
        recurringSettings.setStartDate(LocalDate.of(2025, 1, 1));
        recurringSettings.setPeriodType(PeriodType.MONTHLY);

        recurringSettings.setUserId(1L);

        wallet = Wallet.create(1L);
        wallet.deposit(BigDecimal.valueOf(1000));
    }

    @Test
    void shouldPassValidationWhenAllConditionsAreValid() {
        recurringSettings.setPiggyBankId(10L);

        recurringSavingsValidator.validate(recurringSettings, wallet);

        verify(recurringBasicValidator).validateBasicsWithoutCategory(recurringSettings);
    }

    @Test
    void shouldThrowExceptionWhenAmountExceedsWalletBalance() {
        recurringSettings.setPiggyBankId(10L);
        recurringSettings.setAmount(BigDecimal.valueOf(2000));

        wallet.withdraw(BigDecimal.valueOf(900));
        InvalidInputException invalidInputException = assertThrows(InvalidInputException.class,
                () -> recurringSavingsValidator.validate(recurringSettings, wallet));

        assertEquals("Insufficient funds", invalidInputException.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenPiggyBankIsNull() {
        recurringSettings.setPiggyBankId(null);

        InvalidInputException invalidInputException = assertThrows(InvalidInputException.class,
                () -> recurringSavingsValidator.validate(recurringSettings, wallet));

        assertEquals("Piggy bank is required", invalidInputException.getMessage());
    }
}