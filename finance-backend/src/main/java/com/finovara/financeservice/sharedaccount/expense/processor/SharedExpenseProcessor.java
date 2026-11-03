package com.finovara.financeservice.sharedaccount.expense.processor;

import com.finovara.financeservice.sharedaccount.expense.repository.SharedExpenseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class SharedExpenseProcessor {

    private final SharedExpenseRepository sharedExpenseRepository;

    @Transactional
    public void deleteAllSharedExpenses() {
        sharedExpenseRepository.deleteAllInBatch();
        log.info("Shared expenses have been deleted.");
    }
}
