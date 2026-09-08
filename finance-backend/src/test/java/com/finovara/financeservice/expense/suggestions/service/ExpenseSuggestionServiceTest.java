package com.finovara.financeservice.expense.suggestions.service;

import com.finovara.contracts.model.transaction.ExpenseCategory;
import com.finovara.financeservice.expense.repository.ExpenseRepository;
import com.finovara.financeservice.expense.suggestions.dto.ExpenseSuggestionDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExpenseSuggestionServiceTest {

    private static final Long USER_ID = 1L;
    private static final int PAGE_SIZE = 3;

    @Mock
    private ExpenseRepository expenseRepository;

    @InjectMocks
    private ExpenseSuggestionService expenseSuggestionService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(expenseSuggestionService, "pageSize", PAGE_SIZE);
    }

    @Nested
    class GetExpenseSuggestion {

        @Test
        void shouldReturnSuggestionWhenAllLatestExpensesAreIdentical() {
            ExpenseSuggestionDto suggestion = new ExpenseSuggestionDto(ExpenseCategory.FOOD, BigDecimal.valueOf(50));
            List<ExpenseSuggestionDto> latest = List.of(suggestion, suggestion, suggestion);
            when(expenseRepository.findLatestExpensePairsByUserId(eq(USER_ID), any(PageRequest.class))).thenReturn(latest);

            ExpenseSuggestionDto result = expenseSuggestionService.getExpenseSuggestion(USER_ID);

            assertEquals(suggestion, result);
        }

        @Test
        void shouldReturnNullWhenLatestExpensesCountIsLessThanPageSize() {
            List<ExpenseSuggestionDto> latest = List.of(new ExpenseSuggestionDto(ExpenseCategory.FOOD, BigDecimal.valueOf(50)));
            when(expenseRepository.findLatestExpensePairsByUserId(eq(USER_ID), any(PageRequest.class))).thenReturn(latest);

            ExpenseSuggestionDto result = expenseSuggestionService.getExpenseSuggestion(USER_ID);

            assertNull(result);
        }

        @Test
        void shouldReturnNullWhenLatestExpensesAreNotIdentical() {
            List<ExpenseSuggestionDto> latest = List.of(
                    new ExpenseSuggestionDto(ExpenseCategory.FOOD, BigDecimal.valueOf(50)),
                    new ExpenseSuggestionDto(ExpenseCategory.TRANSPORT, BigDecimal.valueOf(50)),
                    new ExpenseSuggestionDto(ExpenseCategory.FOOD, BigDecimal.valueOf(50))
            );
            when(expenseRepository.findLatestExpensePairsByUserId(eq(USER_ID), any(PageRequest.class))).thenReturn(latest);

            ExpenseSuggestionDto result = expenseSuggestionService.getExpenseSuggestion(USER_ID);

            assertNull(result);
        }

        @Test
        void shouldReturnNullWhenRepositoryReturnsEmptyList() {
            when(expenseRepository.findLatestExpensePairsByUserId(eq(USER_ID), any(PageRequest.class))).thenReturn(Collections.emptyList());

            ExpenseSuggestionDto result = expenseSuggestionService.getExpenseSuggestion(USER_ID);

            assertNull(result);
        }

        @Test
        void shouldReturnSuggestionWhenPageSizeIsOneAndSingleExpenseExists() {
            ReflectionTestUtils.setField(expenseSuggestionService, "pageSize", 1);
            ExpenseSuggestionDto suggestion = new ExpenseSuggestionDto(ExpenseCategory.ENTERTAINMENT, BigDecimal.TEN);
            when(expenseRepository.findLatestExpensePairsByUserId(eq(USER_ID), any(PageRequest.class))).thenReturn(List.of(suggestion));

            ExpenseSuggestionDto result = expenseSuggestionService.getExpenseSuggestion(USER_ID);

            assertEquals(suggestion, result);
        }

        @Test
        void shouldCallRepositoryWithCorrectUserIdAndPageableExactlyOnce() {
            when(expenseRepository.findLatestExpensePairsByUserId(eq(USER_ID), any(PageRequest.class))).thenReturn(Collections.emptyList());

            expenseSuggestionService.getExpenseSuggestion(USER_ID);

            verify(expenseRepository, times(1)).findLatestExpensePairsByUserId(eq(USER_ID), eq(PageRequest.of(0, PAGE_SIZE)));
        }

        @Test
        void shouldThrowExceptionWhenRepositoryThrowsException() {
            when(expenseRepository.findLatestExpensePairsByUserId(eq(USER_ID), any(PageRequest.class))).thenThrow(new RuntimeException("Database error"));

            assertThrows(RuntimeException.class, () -> expenseSuggestionService.getExpenseSuggestion(USER_ID));
        }

        @Test
        void shouldReturnNullWhenUserIdIsNullAndRepositoryReturnsEmptyList() {
            when(expenseRepository.findLatestExpensePairsByUserId(isNull(), any(PageRequest.class))).thenReturn(Collections.emptyList());

            ExpenseSuggestionDto result = expenseSuggestionService.getExpenseSuggestion(null);

            assertNull(result);
        }
    }
}