package com.finovara.financeservice.settings.finances.recurring.service.validator.util;

import com.finovara.contracts.exception.badrequest.InvalidInputException;
import com.finovara.contracts.util.PeriodType;
import com.finovara.financeservice.settings.finances.recurring.model.RecurringSettings;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecurringBasicValidatorTest {

    private static final Long USER_ID = 1L;
    private static final String CATEGORY = "CATEGORY";
    private static final PeriodType PERIOD_TYPE = PeriodType.values()[0];
    private static final LocalDate START_DATE = LocalDate.of(2025, 1, 1);
    private static final LocalDate END_DATE = LocalDate.of(2025, 12, 31);

    @Mock
    private RecurringSettings settings;

    @InjectMocks
    private RecurringBasicValidator validator;

    @Nested
    class ValidateBasics {

        @Test
        void shouldNotThrowWhenAllFieldsAreProvided() {
            when(settings.getUserId()).thenReturn(USER_ID);
            when(settings.getStartDate()).thenReturn(START_DATE);
            when(settings.getEndDate()).thenReturn(END_DATE);
            when(settings.getPeriodType()).thenReturn(PERIOD_TYPE);
            when(settings.getAmount()).thenReturn(BigDecimal.TEN);

            assertDoesNotThrow(() -> validator.validateBasics(settings, CATEGORY));
        }

        @Test
        void shouldThrowExceptionWhenUserIsNull() {
            when(settings.getUserId()).thenReturn(null);

            InvalidInputException exception = assertThrows(InvalidInputException.class, () -> validator.validateBasics(settings, CATEGORY));

            assertEquals("User is required", exception.getMessage());
        }

        @Test
        void shouldThrowExceptionWhenStartDateIsNull() {
            when(settings.getUserId()).thenReturn(USER_ID);
            when(settings.getStartDate()).thenReturn(null);

            InvalidInputException exception = assertThrows(InvalidInputException.class, () -> validator.validateBasics(settings, CATEGORY));

            assertEquals("Start date is required", exception.getMessage());
        }

        @Test
        void shouldThrowExceptionWhenEndDateIsNull() {
            when(settings.getUserId()).thenReturn(USER_ID);
            when(settings.getStartDate()).thenReturn(START_DATE);
            when(settings.getEndDate()).thenReturn(null);

            InvalidInputException exception = assertThrows(InvalidInputException.class, () -> validator.validateBasics(settings, CATEGORY));

            assertEquals("End date is required", exception.getMessage());
        }

        @Test
        void shouldThrowExceptionWhenCategoryIsNull() {
            when(settings.getUserId()).thenReturn(USER_ID);
            when(settings.getStartDate()).thenReturn(START_DATE);
            when(settings.getEndDate()).thenReturn(END_DATE);

            InvalidInputException exception = assertThrows(InvalidInputException.class, () -> validator.validateBasics(settings, null));

            assertEquals("Category is required", exception.getMessage());
        }

        @Test
        void shouldThrowExceptionWhenPeriodTypeIsNull() {
            when(settings.getUserId()).thenReturn(USER_ID);
            when(settings.getStartDate()).thenReturn(START_DATE);
            when(settings.getEndDate()).thenReturn(END_DATE);
            when(settings.getPeriodType()).thenReturn(null);

            InvalidInputException exception = assertThrows(InvalidInputException.class, () -> validator.validateBasics(settings, CATEGORY));

            assertEquals("Frequency is required", exception.getMessage());
        }

        @Test
        void shouldThrowExceptionWhenAmountIsNull() {
            when(settings.getUserId()).thenReturn(USER_ID);
            when(settings.getStartDate()).thenReturn(START_DATE);
            when(settings.getEndDate()).thenReturn(END_DATE);
            when(settings.getPeriodType()).thenReturn(PERIOD_TYPE);
            when(settings.getAmount()).thenReturn(null);

            InvalidInputException exception = assertThrows(InvalidInputException.class, () -> validator.validateBasics(settings, CATEGORY));

            assertEquals("Amount is required", exception.getMessage());
        }
    }

    @Nested
    class ValidateBasicsWithoutCategory {

        @Test
        void shouldNotThrowWhenAllFieldsAreProvided() {
            when(settings.getUserId()).thenReturn(USER_ID);
            when(settings.getStartDate()).thenReturn(START_DATE);
            when(settings.getEndDate()).thenReturn(END_DATE);
            when(settings.getPeriodType()).thenReturn(PERIOD_TYPE);
            when(settings.getAmount()).thenReturn(BigDecimal.TEN);

            assertDoesNotThrow(() -> validator.validateBasicsWithoutCategory(settings));
        }

        @Test
        void shouldThrowExceptionWhenUserIsNull() {
            when(settings.getUserId()).thenReturn(null);

            InvalidInputException exception = assertThrows(InvalidInputException.class, () -> validator.validateBasicsWithoutCategory(settings));

            assertEquals("User is required", exception.getMessage());
        }

        @Test
        void shouldThrowExceptionWhenStartDateIsNull() {
            when(settings.getUserId()).thenReturn(USER_ID);
            when(settings.getStartDate()).thenReturn(null);

            InvalidInputException exception = assertThrows(InvalidInputException.class, () -> validator.validateBasicsWithoutCategory(settings));

            assertEquals("Start date is required", exception.getMessage());
        }

        @Test
        void shouldThrowExceptionWhenEndDateIsNull() {
            when(settings.getUserId()).thenReturn(USER_ID);
            when(settings.getStartDate()).thenReturn(START_DATE);
            when(settings.getEndDate()).thenReturn(null);

            InvalidInputException exception = assertThrows(InvalidInputException.class, () -> validator.validateBasicsWithoutCategory(settings));

            assertEquals("End date is required", exception.getMessage());
        }

        @Test
        void shouldThrowExceptionWhenPeriodTypeIsNull() {
            when(settings.getUserId()).thenReturn(USER_ID);
            when(settings.getStartDate()).thenReturn(START_DATE);
            when(settings.getEndDate()).thenReturn(END_DATE);
            when(settings.getPeriodType()).thenReturn(null);

            InvalidInputException exception = assertThrows(InvalidInputException.class, () -> validator.validateBasicsWithoutCategory(settings));

            assertEquals("Frequency is required", exception.getMessage());
        }

        @Test
        void shouldThrowExceptionWhenAmountIsNull() {
            when(settings.getUserId()).thenReturn(USER_ID);
            when(settings.getStartDate()).thenReturn(START_DATE);
            when(settings.getEndDate()).thenReturn(END_DATE);
            when(settings.getPeriodType()).thenReturn(PERIOD_TYPE);
            when(settings.getAmount()).thenReturn(null);

            InvalidInputException exception = assertThrows(InvalidInputException.class, () -> validator.validateBasicsWithoutCategory(settings));

            assertEquals("Amount is required", exception.getMessage());
        }
    }
}