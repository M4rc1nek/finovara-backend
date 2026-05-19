package com.finovara.finovarabackend.usersetting.finances.recurring.service.validator.util;

import com.finovara.finovarabackend.exception.badrequest.InvalidInputException;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.usersetting.finances.recurring.model.RecurringSettings;
import com.finovara.finovarabackend.util.model.PeriodType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class RecurringBasicValidatorTest {

    private RecurringBasicValidator recurringBasicValidator;
    private RecurringSettings recurringSettings;

    @BeforeEach
    void setUp() {
        recurringBasicValidator = new RecurringBasicValidator();

        recurringSettings = new RecurringSettings();
        recurringSettings.setAmount(BigDecimal.valueOf(100));
        recurringSettings.setStartDate(LocalDate.of(2025, 1, 1));
        recurringSettings.setPeriodType(PeriodType.MONTHLY);
    }

    @Nested
    class ValidateBasics {

        @Test
        void shouldPassValidationWhenAllFieldsAreValid() {
            recurringSettings.setUserAssigned(new User());

            assertDoesNotThrow(() -> recurringBasicValidator.validateBasics(recurringSettings, "CATEGORY"));
        }

        @Test
        void shouldThrowExceptionWhenUserIsNull() {
            recurringSettings.setUserAssigned(null);

            InvalidInputException invalidInputException = assertThrows(InvalidInputException.class,
                    () -> recurringBasicValidator.validateBasics(recurringSettings, "CATEGORY"));

            assertEquals("User is required", invalidInputException.getMessage());
        }

        @Test
        void shouldThrowExceptionWhenStartDateIsNull() {
            recurringSettings.setUserAssigned(new User());
            recurringSettings.setStartDate(null);

            InvalidInputException invalidInputException = assertThrows(InvalidInputException.class,
                    () -> recurringBasicValidator.validateBasics(recurringSettings, "CATEGORY"));

            assertEquals("Start date is required", invalidInputException.getMessage());
        }

        @Test
        void shouldThrowExceptionWhenCategoryIsNull() {
            recurringSettings.setUserAssigned(new User());

            InvalidInputException invalidInputException = assertThrows(InvalidInputException.class,
                    () -> recurringBasicValidator.validateBasics(recurringSettings, null));

            assertEquals("Category is required", invalidInputException.getMessage());
        }

        @Test
        void shouldThrowExceptionWhenPeriodTypeIsNull() {
            recurringSettings.setUserAssigned(new User());
            recurringSettings.setPeriodType(null);

            InvalidInputException invalidInputException = assertThrows(InvalidInputException.class,
                    () -> recurringBasicValidator.validateBasics(recurringSettings, "CATEGORY"));

            assertEquals("Frequency is required", invalidInputException.getMessage());
        }

        @Test
        void shouldThrowExceptionWhenAmountIsNull() {
            recurringSettings.setUserAssigned(new User());
            recurringSettings.setAmount(null);

            InvalidInputException invalidInputException = assertThrows(InvalidInputException.class,
                    () -> recurringBasicValidator.validateBasics(recurringSettings, "CATEGORY"));

            assertEquals("Amount is required", invalidInputException.getMessage());
        }
    }

    @Nested
    class ValidateBasicsWithoutCategory {

        @Test
        void shouldPassValidationWhenAllFieldsAreValid() {
            recurringSettings.setUserAssigned(new User());

            assertDoesNotThrow(() -> recurringBasicValidator.validateBasicsWithoutCategory(recurringSettings));
        }

        @Test
        void shouldThrowExceptionWhenUserIsNull() {
            recurringSettings.setUserAssigned(null);

            InvalidInputException invalidInputException = assertThrows(InvalidInputException.class,
                    () -> recurringBasicValidator.validateBasicsWithoutCategory(recurringSettings));

            assertEquals("User is required", invalidInputException.getMessage());
        }

        @Test
        void shouldThrowExceptionWhenStartDateIsNull() {
            recurringSettings.setUserAssigned(new User());
            recurringSettings.setStartDate(null);

            InvalidInputException invalidInputException = assertThrows(InvalidInputException.class,
                    () -> recurringBasicValidator.validateBasicsWithoutCategory(recurringSettings));

            assertEquals("Start date is required", invalidInputException.getMessage());
        }

        @Test
        void shouldThrowExceptionWhenPeriodTypeIsNull() {
            recurringSettings.setUserAssigned(new User());
            recurringSettings.setPeriodType(null);

            InvalidInputException invalidInputException = assertThrows(InvalidInputException.class,
                    () -> recurringBasicValidator.validateBasicsWithoutCategory(recurringSettings));

            assertEquals("Frequency is required", invalidInputException.getMessage());
        }

        @Test
        void shouldThrowExceptionWhenAmountIsNull() {
            recurringSettings.setUserAssigned(new User());
            recurringSettings.setAmount(null);

            InvalidInputException invalidInputException = assertThrows(InvalidInputException.class,
                    () -> recurringBasicValidator.validateBasicsWithoutCategory(recurringSettings));

            assertEquals("Amount is required", invalidInputException.getMessage());
        }
    }
}