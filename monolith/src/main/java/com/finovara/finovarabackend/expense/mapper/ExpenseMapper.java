package com.finovara.finovarabackend.expense.mapper;

import com.finovara.finovarabackend.expense.dto.ExpenseDto;
import com.finovara.finovarabackend.expense.model.Expense;
import org.springframework.stereotype.Component;

@Component
public class ExpenseMapper {
    public ExpenseDto mapExpenseToDto(Expense expense) {
        return new ExpenseDto(
                expense.getId(),
                expense.getUserAssigned().getId(),
                expense.getAmount(),
                expense.getCategory(),
                expense.getCreatedAt(),
                expense.getDescription()
        );
    }
}
