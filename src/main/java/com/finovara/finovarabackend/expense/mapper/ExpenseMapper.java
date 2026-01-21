package com.finovara.finovarabackend.expense.mapper;

import com.finovara.finovarabackend.expense.dto.ExpenseDTO;
import com.finovara.finovarabackend.expense.model.Expense;
import org.springframework.stereotype.Component;

@Component
public class ExpenseMapper {
    public ExpenseDTO mapExpenseToDTO(Expense expense) {
        return new ExpenseDTO(
                expense.getId(),
                expense.getUserAssigned().getId(),
                expense.getAmount(),
                expense.getCategory(),
                expense.getCreatedAt(),
                expense.getDescription()
        );
    }
}
