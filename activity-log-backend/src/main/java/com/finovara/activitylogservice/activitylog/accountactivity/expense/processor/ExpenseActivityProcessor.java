package com.finovara.activitylogservice.activitylog.accountactivity.expense.processor;

import com.finovara.activitylogservice.activitylog.accountactivity.expense.repository.ExpenseActivityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Slf4j
@RequiredArgsConstructor
public class ExpenseActivityProcessor {

    private final ExpenseActivityRepository expenseActivityRepository;

    @Transactional
    public void deleteExpenseActivity(){
        expenseActivityRepository.deleteAllInBatch();
        log.info("Expense Activity has been deleted.");
    }
}
