package com.finovara.financeservice.sharedaccount.mapper.expense;

import com.finovara.financeservice.sharedaccount.dto.expense.SharedExpenseDto;
import com.finovara.financeservice.sharedaccount.model.expense.SharedExpense;
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