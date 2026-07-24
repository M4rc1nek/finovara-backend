package com.finovara.financeservice.sharedaccount.expense.mapper;

import com.finovara.financeservice.sharedaccount.expense.dto.SharedExpenseDto;
import com.finovara.financeservice.sharedaccount.expense.model.SharedExpense;
import org.springframework.stereotype.Component;

@Component
public class SharedExpenseMapper {

    public SharedExpenseDto mapToDto(SharedExpense expense, String createdByUsername) {
        return new SharedExpenseDto(
                expense.getId(),
                expense.getAmount(),
                expense.getCategory(),
                expense.getCreatedAt(),
                expense.getDescription(),
                expense.getCreatedByUserId(),
                createdByUsername
        );
    }
}