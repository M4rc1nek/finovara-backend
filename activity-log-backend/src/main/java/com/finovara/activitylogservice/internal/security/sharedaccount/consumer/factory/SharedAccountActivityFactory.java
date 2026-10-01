package com.finovara.activitylogservice.internal.security.sharedaccount.consumer.factory;

import com.finovara.activitylogservice.internal.security.sharedaccount.model.SharedAccountFinanceActivity;
import com.finovara.activitylogservice.internal.security.sharedaccount.repository.SharedAccountFinanceActivityRepository;
import com.finovara.contracts.activity.event.sharedaccount.SharedFinanceActivityType;
import com.finovara.contracts.finance.event.sharedaccount.finance.SharedAccountExpenseActivityEvent;
import com.finovara.contracts.finance.event.sharedaccount.finance.SharedAccountPiggyBankDepositActivityEvent;
import com.finovara.contracts.finance.event.sharedaccount.finance.SharedAccountRevenueActivityEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SharedAccountFinanceActivityFactory {

    private final SharedAccountFinanceActivityRepository financeActivityRepository;

    @Transactional
    public void createExpenseCreated(SharedAccountExpenseActivityEvent event) {
        SharedAccountFinanceActivity activity = SharedAccountFinanceActivity.builder()
                .ownerId(event.ownerId())
                .memberId(event.memberId())
                .userId(event.userId())
                .activityType(SharedFinanceActivityType.EXPENSE_CREATED)
                .transactionId(event.expenseId())
                .amount(event.amount())
                .createdAt(event.createdAt())
                .build();

        financeActivityRepository.save(activity);
        log.info("Saved EXPENSE activity: {} for user {}", activity.getId(), event.userId());
    }

    @Transactional
    public void createRevenueCreated(SharedAccountRevenueActivityEvent event) {
        SharedAccountFinanceActivity activity = SharedAccountFinanceActivity.builder()
                .ownerId(event.ownerId())
                .memberId(event.memberId())
                .userId(event.userId())
                .activityType(SharedFinanceActivityType.REVENUE_CREATED)
                .transactionId(event.revenueId())
                .amount(event.amount())
                .createdAt(event.createdAt())
                .build();

        financeActivityRepository.save(activity);
        log.info("Saved REVENUE activity: {} for user {}", activity.getId(), event.userId());
    }

    @Transactional
    public void createPiggyBankDepositAdded(SharedAccountPiggyBankDepositActivityEvent event) {
        SharedAccountFinanceActivity activity = SharedAccountFinanceActivity.builder()
                .ownerId(event.ownerId())
                .memberId(event.memberId())
                .userId(event.userId())
                .activityType(SharedFinanceActivityType.PIGGY_BANK_DEPOSIT)
                .transactionId(event.piggyBankId())
                .amount(event.amount())
                .createdAt(event.createdAt())
                .build();

        financeActivityRepository.save(activity);
        log.info("Saved PIGGY_BANK_DEPOSIT activity: {} for user {}", activity.getId(), event.userId());
    }
}