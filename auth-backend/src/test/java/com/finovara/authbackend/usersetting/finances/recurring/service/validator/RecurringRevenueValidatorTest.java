package com.finovara.authbackend.usersetting.finances.recurring.service.validator;

import com.finovara.contracts.model.transaction.RevenueCategory;
import com.finovara.contracts.exception.badrequest.InvalidInputException;
import com.finovara.authbackend.user.model.User;
import com.finovara.authbackend.usersetting.finances.recurring.model.RecurringSettings;
import com.finovara.authbackend.usersetting.finances.recurring.service.validator.util.RecurringBasicValidator;
import com.finovara.contracts.model.PeriodType;
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
class RecurringRevenueValidatorTest {

    @Mock
    private RecurringBasicValidator recurringBasicValidator;

    @InjectMocks
    private RecurringRevenueValidator recurringRevenueValidator;

    private RecurringSettings recurringSettings;

    @BeforeEach
    void setUp() {
        recurringSettings = new RecurringSettings();

        recurringSettings.setStartDate(LocalDate.of(2025, 1, 1));
        recurringSettings.setPeriodType(PeriodType.MONTHLY);
        recurringSettings.setRevenueCategory(RevenueCategory.SALARY);

        recurringSettings.setUserId(new User());
    }

    @Test
    void shouldPassValidationWhenAmountIsValid() {
        recurringSettings.setAmount(BigDecimal.valueOf(100));

        recurringRevenueValidator.validate(recurringSettings);

        verify(recurringBasicValidator).validateBasics(recurringSettings, recurringSettings.getRevenueCategory());
    }

    @Test
    void shouldThrowExceptionWhenAmountIsZero() {
        recurringSettings.setAmount(BigDecimal.ZERO);

        InvalidInputException invalidInputException = assertThrows(InvalidInputException.class,
                () -> recurringRevenueValidator.validate(recurringSettings));

        assertEquals("Recurring revenue must be greater than 0", invalidInputException.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenAmountIsNegative() {
        recurringSettings.setAmount(BigDecimal.valueOf(-10));

        InvalidInputException invalidInputException = assertThrows(InvalidInputException.class,
                () -> recurringRevenueValidator.validate(recurringSettings));

        assertEquals("Recurring revenue must be greater than 0", invalidInputException.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenAmountExceedsMaximum() {
        recurringSettings.setAmount(BigDecimal.valueOf(6000000));

        InvalidInputException invalidInputException = assertThrows(InvalidInputException.class,
                () -> recurringRevenueValidator.validate(recurringSettings));

        assertEquals("Recurring revenue cannot be greater than 5000000", invalidInputException.getMessage());
    }
}