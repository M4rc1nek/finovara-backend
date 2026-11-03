package com.finovara.financeservice.expense.processor;

import com.finovara.financeservice.expense.repository.ExpenseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class ExpenseProcessor {

    private final ExpenseRepository expenseRepository;

    @Transactional
    public void deleteAllExpenses(){
        expenseRepository.deleteAllInBatch();
        log.info("Expenses have been deleted.");
    }
}
