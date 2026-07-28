package com.finovara.financeservice.settings.finances.recurring.service.validator.util;

import com.finovara.contracts.exception.badrequest.InvalidInputException;
import com.finovara.contracts.model.PeriodType;
import com.finovara.financeservice.settings.finances.recurring.model.RecurringSettings;
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
        recurringSettings.setEndDate(LocalDate.of(2026, 1, 1));
        recurringSettings.setPeriodType(PeriodType.MONTHLY);
    }

    @Nested
    class ValidateBasics {

        @Test
        void shouldPassValidationWhenAllFieldsAreValid() {
            recurringSettings.setUserId(1L);

            assertDoesNotThrow(() -> recurringBasicValidator.validateBasics(recurringSettings, "CATEGORY"));
        }

        @Test
        void shouldThrowExceptionWhenUserIsNull() {
            recurringSettings.setUserId(null);

            InvalidInputException invalidInputException = assertThrows(InvalidInputException.class,
                    () -> recurringBasicValidator.validateBasics(recurringSettings, "CATEGORY"));

            assertEquals("User is required", invalidInputException.getMessage());
        }

        @Test
        void shouldThrowExceptionWhenStartDateIsNull() {
            recurringSettings.setUserId(1L);
            recurringSettings.setStartDate(null);

            InvalidInputException invalidInputException = assertThrows(InvalidInputException.class,
                    () -> recurringBasicValidator.validateBasics(recurringSettings, "CATEGORY"));

            assertEquals("Start date is required", invalidInputException.getMessage());
        }

        @Test
        void shouldThrowExceptionWhenEndDateIsNull() {
            recurringSettings.setUserId(1L);
            recurringSettings.setEndDate(null);

            InvalidInputException invalidInputException = assertThrows(InvalidInputException.class,
                    () -> recurringBasicValidator.validateBasics(recurringSettings, "CATEGORY"));

            assertEquals("End date is required", invalidInputException.getMessage());
        }

        @Test
        void shouldThrowExceptionWhenCategoryIsNull() {
            recurringSettings.setUserId(1L);

            InvalidInputException invalidInputException = assertThrows(InvalidInputException.class,
                    () -> recurringBasicValidator.validateBasics(recurringSettings, null));

            assertEquals("Category is required", invalidInputException.getMessage());
        }

        @Test
        void shouldThrowExceptionWhenPeriodTypeIsNull() {
            recurringSettings.setUserId(1L);
            recurringSettings.setPeriodType(null);

            InvalidInputException invalidInputException = assertThrows(InvalidInputException.class,
                    () -> recurringBasicValidator.validateBasics(recurringSettings, "CATEGORY"));

            assertEquals("Frequency is required", invalidInputException.getMessage());
        }

        @Test
        void shouldThrowExceptionWhenAmountIsNull() {
            recurringSettings.setUserId(1L);
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
            recurringSettings.setUserId(1L);

            assertDoesNotThrow(() -> recurringBasicValidator.validateBasicsWithoutCategory(recurringSettings));
        }

        @Test
        void shouldThrowExceptionWhenUserIsNull() {
            recurringSettings.setUserId(null);

            InvalidInputException invalidInputException = assertThrows(InvalidInputException.class,
                    () -> recurringBasicValidator.validateBasicsWithoutCategory(recurringSettings));

            assertEquals("User is required", invalidInputException.getMessage());
        }

        @Test
        void shouldThrowExceptionWhenStartDateIsNull() {
            recurringSettings.setUserId(1L);
            recurringSettings.setStartDate(null);

            InvalidInputException invalidInputException = assertThrows(InvalidInputException.class,
                    () -> recurringBasicValidator.validateBasicsWithoutCategory(recurringSettings));

            assertEquals("Start date is required", invalidInputException.getMessage());
        }

        @Test
        void shouldThrowExceptionWhenEndDateIsNull() {
            recurringSettings.setUserId(1L);
            recurringSettings.setEndDate(null);

            InvalidInputException invalidInputException = assertThrows(InvalidInputException.class,
                    () -> recurringBasicValidator.validateBasics(recurringSettings, "CATEGORY"));

            assertEquals("End date is required", invalidInputException.getMessage());
        }

        @Test
        void shouldThrowExceptionWhenPeriodTypeIsNull() {
            recurringSettings.setUserId(1L);
            recurringSettings.setPeriodType(null);

            InvalidInputException invalidInputException = assertThrows(InvalidInputException.class,
                    () -> recurringBasicValidator.validateBasicsWithoutCategory(recurringSettings));

            assertEquals("Frequency is required", invalidInputException.getMessage());
        }

        @Test
        void shouldThrowExceptionWhenAmountIsNull() {
            recurringSettings.setUserId(1L);
            recurringSettings.setAmount(null);

            InvalidInputException invalidInputException = assertThrows(InvalidInputException.class,
                    () -> recurringBasicValidator.validateBasicsWithoutCategory(recurringSettings));

            assertEquals("Amount is required", invalidInputException.getMessage());
        }
    }
}