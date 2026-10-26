package com.finovara.financeservice.expense.service;

import com.finovara.contracts.util.model.ExpenseCategory;
import com.finovara.financeservice.expense.model.Expense;
import com.finovara.financeservice.expense.repository.ExpenseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExpenseFilteringService {

    private final ExpenseRepository expenseRepository;

    @Value("${finance.pagination.page-size}")
    private int pageSize;

    public List<Expense> expenseFiltering(Long userId, ExpenseCategory category, BigDecimal amount, LocalDate date, String description, int page) {
        Pageable pageable = PageRequest.of(page, pageSize, Sort.unsorted());

        return expenseRepository.findFilteredExpenses(userId, category, amount, date, description, pageable);
    }
}