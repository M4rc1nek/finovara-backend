package com.finovara.financeservice.expense.suggestions.service;

import com.finovara.financeservice.expense.repository.ExpenseRepository;
import com.finovara.financeservice.expense.suggestions.dto.ExpenseSuggestionDto;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ExpenseSuggestionService {

    private final ExpenseRepository expenseRepository;

    @Value("${expense.suggestion.history-size}")
    private int pageSize;

    @Cacheable(value = "expense:suggestion", key = "#userId", unless = "#result == null")
    public ExpenseSuggestionDto getExpenseSuggestion(Long userId) {
        List<ExpenseSuggestionDto> latest =
                expenseRepository.findLatestExpensePairsByUserId(userId, PageRequest.of(0, pageSize));

        if (latest.size() < pageSize) {
            return null;
        }

        boolean allIdentical = latest.stream().distinct().count() == 1;
        return allIdentical ? latest.getFirst() : null;
    }
}
