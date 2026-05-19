package com.finovara.finovarabackend.accountactivity.expense.processor;

import com.finovara.finovarabackend.accountactivity.expense.repository.ExpenseActivityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ExpenseActivityProcessor {

    private final ExpenseActivityRepository expenseActivityRepository;

    public void deleteExpenseActivity(){
        expenseActivityRepository.deleteAllInBatch();
    }
}
