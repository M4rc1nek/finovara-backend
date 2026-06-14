package com.finovara.authbackend.expense.mapper;

import com.finovara.authbackend.expense.dto.ExpenseDto;
import com.finovara.authbackend.expense.model.Expense;
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
