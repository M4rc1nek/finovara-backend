package com.finovara.activityservice.activity_log.accountactivity.expense.processor;

import com.finovara.activityservice.activity_log.accountactivity.expense.repository.ExpenseActivityRepository;
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
