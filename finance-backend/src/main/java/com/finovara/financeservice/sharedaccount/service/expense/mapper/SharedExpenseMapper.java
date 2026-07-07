package com.finovara.financeservice.sharedaccount.service.expense.mapper;

import com.finovara.financeservice.expense.dto.ExpenseDto;
import com.finovara.financeservice.expense.model.Expense;
import org.springframework.stereotype.Component;

@Component
public class ExpenseMapper {
    public ExpenseDto mapExpenseToDto(Expense expense) {
        return new ExpenseDto(
                expense.getId(),
                expense.getUserId(),
                expense.getAmount(),
                expense.getCategory(),
                expense.getCreatedAt(),
                expense.getDescription()
        );
    }
}
