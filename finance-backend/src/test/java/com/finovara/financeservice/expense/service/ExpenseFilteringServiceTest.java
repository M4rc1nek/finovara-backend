package com.finovara.financeservice.expense.service;

import com.finovara.contracts.util.model.ExpenseCategory;
import com.finovara.financeservice.expense.model.Expense;
import com.finovara.financeservice.expense.repository.ExpenseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExpenseFilteringServiceTest {

    private static final Long USER_ID = 1L;
    private static final int PAGE_SIZE = 10;
    private static final ExpenseCategory CATEGORY = ExpenseCategory.FOOD;
    private static final BigDecimal AMOUNT = new BigDecimal("199.99");
    private static final LocalDate DATE = LocalDate.of(2025, 5, 8);
    private static final String DESCRIPTION = "groceries";

    @Mock
    private ExpenseRepository expenseRepository;

    @Captor
    private ArgumentCaptor<Pageable> pageableCaptor;

    @InjectMocks
    private ExpenseFilteringService service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "pageSize", PAGE_SIZE);
    }

    @Test
    void shouldFilterExpensesUsingRepositoryQuery() {
        Expense expense = Expense.builder()
                .id(99L)
                .userId(USER_ID)
                .category(CATEGORY)
                .amount(AMOUNT)
                .createdAt(DATE)
                .description(DESCRIPTION)
                .build();

        when(expenseRepository.findFilteredExpenses(eq(USER_ID), eq(CATEGORY), eq(AMOUNT), eq(DATE), eq(DESCRIPTION), any(Pageable.class)))
                .thenReturn(List.of(expense));

        List<Expense> result = service.expenseFiltering(USER_ID, CATEGORY, AMOUNT, DATE, DESCRIPTION, 0);

        assertEquals(1, result.size());
        assertSame(expense, result.get(0));

        verify(expenseRepository).findFilteredExpenses(eq(USER_ID), eq(CATEGORY), eq(AMOUNT), eq(DATE), eq(DESCRIPTION), pageableCaptor.capture());

        Pageable pageable = pageableCaptor.getValue();
        assertEquals(0, pageable.getPageNumber());
        assertEquals(PAGE_SIZE, pageable.getPageSize());
        assertEquals(Sort.unsorted(), pageable.getSort());
    }

    @Test
    void shouldUseConfiguredPageSizeForRequestedPage() {
        when(expenseRepository.findFilteredExpenses(eq(USER_ID), eq(CATEGORY), eq(AMOUNT), eq(DATE), eq(DESCRIPTION), any(Pageable.class)))
                .thenReturn(List.of());

        service.expenseFiltering(USER_ID, CATEGORY, AMOUNT, DATE, DESCRIPTION, 2);

        verify(expenseRepository).findFilteredExpenses(eq(USER_ID), eq(CATEGORY), eq(AMOUNT), eq(DATE), eq(DESCRIPTION), pageableCaptor.capture());

        Pageable pageable = pageableCaptor.getValue();
        assertEquals(2, pageable.getPageNumber());
        assertEquals(PAGE_SIZE, pageable.getPageSize());
        assertEquals(Sort.unsorted(), pageable.getSort());
    }
}
