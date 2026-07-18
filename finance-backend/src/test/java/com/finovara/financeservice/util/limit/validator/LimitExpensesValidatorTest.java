package com.finovara.financeservice.util.limit.validator;

import com.finovara.contracts.exception.badrequest.InvalidInputException;
import com.finovara.contracts.model.PeriodType;
import com.finovara.contracts.model.transaction.ExpenseCategory;
import com.finovara.financeservice.limit.dto.LimitDto;
import com.finovara.financeservice.limit.model.LimitStatus;
import com.finovara.financeservice.sharedaccount.limit.dto.SharedLimitDto;
import com.finovara.financeservice.util.periodbalance.FinancialPeriodService;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LimitExpensesValidatorTest {

    private static final Long USER_ID = 1L;

    @Mock
    private FinancialPeriodService financialPeriodService;

    @InjectMocks
    private LimitExpensesValidator limitExpensesValidator;

    @Nested
    class ValidateCurrentExpensesDoNotExceedLimit {

        @Test
        void shouldNotThrowWhenSpentIsLessThanLimitAmount() {
            LimitDto limitDto = new LimitDto(USER_ID, null, PeriodType.MONTHLY, ExpenseCategory.FOOD, LimitStatus.LOW, BigDecimal.valueOf(500), true);

            when(financialPeriodService.getExpensesSum(USER_ID, PeriodType.MONTHLY, ExpenseCategory.FOOD)).thenReturn(BigDecimal.valueOf(300));

            assertDoesNotThrow(() -> limitExpensesValidator.validateCurrentExpensesDoNotExceedLimit(USER_ID, limitDto));
        }

        @Test
        void shouldNotThrowWhenSpentEqualsLimitAmount() {
            LimitDto limitDto = new LimitDto(USER_ID, null, PeriodType.MONTHLY, ExpenseCategory.FOOD, LimitStatus.LOW, BigDecimal.valueOf(500), true);

            when(financialPeriodService.getExpensesSum(USER_ID, PeriodType.MONTHLY, ExpenseCategory.FOOD)).thenReturn(BigDecimal.valueOf(500));

            assertDoesNotThrow(() -> limitExpensesValidator.validateCurrentExpensesDoNotExceedLimit(USER_ID, limitDto));
        }

        @Test
        void shouldThrowInvalidInputExceptionWhenSpentExceedsLimitAmount() {
            LimitDto limitDto = new LimitDto(USER_ID, null, PeriodType.MONTHLY, ExpenseCategory.FOOD, LimitStatus.LOW, BigDecimal.valueOf(500), true);

            when(financialPeriodService.getExpensesSum(USER_ID, PeriodType.MONTHLY, ExpenseCategory.FOOD)).thenReturn(BigDecimal.valueOf(600));

            assertThrows(InvalidInputException.class, () -> limitExpensesValidator.validateCurrentExpensesDoNotExceedLimit(USER_ID, limitDto));
        }

        @Test
        void shouldQueryFinancialPeriodServiceWithNullCategoryWhenGeneralLimit() {
            LimitDto limitDto = new LimitDto(USER_ID, null, PeriodType.MONTHLY, null, LimitStatus.LOW, BigDecimal.valueOf(1000), true);

            when(financialPeriodService.getExpensesSum(USER_ID, PeriodType.MONTHLY, null)).thenReturn(BigDecimal.valueOf(200));

            assertDoesNotThrow(() -> limitExpensesValidator.validateCurrentExpensesDoNotExceedLimit(USER_ID, limitDto));
        }
    }

    @Nested
    class ValidateCurrentSharedExpensesDoNotExceedLimit {

        @Test
        void shouldNotThrowWhenSpentIsLessThanLimitAmount() {
            SharedLimitDto sharedLimitDto = mock(SharedLimitDto.class);
            when(sharedLimitDto.periodType()).thenReturn(PeriodType.MONTHLY);
            when(sharedLimitDto.category()).thenReturn(ExpenseCategory.FOOD);
            when(sharedLimitDto.amount()).thenReturn(BigDecimal.valueOf(500));

            when(financialPeriodService.getExpensesSum(USER_ID, PeriodType.MONTHLY, ExpenseCategory.FOOD)).thenReturn(BigDecimal.valueOf(300));

            assertDoesNotThrow(() -> limitExpensesValidator.validateCurrentSharedExpensesDoNotExceedLimit(USER_ID, sharedLimitDto));
        }

        @Test
        void shouldNotThrowWhenSpentEqualsLimitAmount() {
            SharedLimitDto sharedLimitDto = mock(SharedLimitDto.class);
            when(sharedLimitDto.periodType()).thenReturn(PeriodType.MONTHLY);
            when(sharedLimitDto.category()).thenReturn(ExpenseCategory.FOOD);
            when(sharedLimitDto.amount()).thenReturn(BigDecimal.valueOf(500));

            when(financialPeriodService.getExpensesSum(USER_ID, PeriodType.MONTHLY, ExpenseCategory.FOOD)).thenReturn(BigDecimal.valueOf(500));

            assertDoesNotThrow(() -> limitExpensesValidator.validateCurrentSharedExpensesDoNotExceedLimit(USER_ID, sharedLimitDto));
        }

        @Test
        void shouldThrowInvalidInputExceptionWhenSpentExceedsLimitAmount() {
            SharedLimitDto sharedLimitDto = mock(SharedLimitDto.class);
            when(sharedLimitDto.periodType()).thenReturn(PeriodType.MONTHLY);
            when(sharedLimitDto.category()).thenReturn(ExpenseCategory.FOOD);
            when(sharedLimitDto.amount()).thenReturn(BigDecimal.valueOf(500));

            when(financialPeriodService.getExpensesSum(USER_ID, PeriodType.MONTHLY, ExpenseCategory.FOOD)).thenReturn(BigDecimal.valueOf(600));

            assertThrows(InvalidInputException.class, () -> limitExpensesValidator.validateCurrentSharedExpensesDoNotExceedLimit(USER_ID, sharedLimitDto));
        }

        @Test
        void shouldQueryFinancialPeriodServiceWithNullCategoryWhenGeneralSharedLimit() {
            SharedLimitDto sharedLimitDto = mock(SharedLimitDto.class);
            when(sharedLimitDto.periodType()).thenReturn(PeriodType.WEEKLY);
            when(sharedLimitDto.category()).thenReturn(null);
            when(sharedLimitDto.amount()).thenReturn(BigDecimal.valueOf(1000));

            when(financialPeriodService.getExpensesSum(USER_ID, PeriodType.WEEKLY, null)).thenReturn(BigDecimal.valueOf(100));

            assertDoesNotThrow(() -> limitExpensesValidator.validateCurrentSharedExpensesDoNotExceedLimit(USER_ID, sharedLimitDto));
        }
    }

    private static SharedLimitDto mock(Class<SharedLimitDto> clazz) {
        return org.mockito.Mockito.mock(clazz);
    }
}