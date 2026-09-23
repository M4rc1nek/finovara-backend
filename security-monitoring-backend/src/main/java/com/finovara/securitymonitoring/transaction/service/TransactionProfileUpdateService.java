package com.finovara.securitymonitoring.transaction.service;

import com.finovara.contracts.activity.event.expense.ExpenseActivityEvent;
import com.finovara.contracts.activity.event.piggybank.PiggyBankActivityEvent;
import com.finovara.contracts.activity.event.revenue.RevenueActivityEvent;
import com.finovara.contracts.datadeletable.UserDataDeletable;
import com.finovara.contracts.exception.notfound.RequestedEntityNotFoundException;
import com.finovara.contracts.model.activity.ExpenseActivityType;
import com.finovara.contracts.model.activity.PiggyBankActivityType;
import com.finovara.securitymonitoring.transaction.model.TransactionProfile;
import com.finovara.securitymonitoring.transaction.repository.TransactionProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionProfileUpdateService implements UserDataDeletable {

    private final TransactionProfileRepository transactionProfileRepository;

    @Transactional
    public void handleExpenseEvent(ExpenseActivityEvent event) {
        if (event.type() != ExpenseActivityType.ADDED_EXPENSE) {
            return;
        }

        TransactionProfile profile = getProfileOrThrow(event.userId());

        BigDecimal newAverage = calculateNewAverage(profile.getAverageExpenseAmount(), profile.getExpenseCount(), event.amount());

        profile.setAverageExpenseAmount(newAverage);
        profile.setExpenseCount(profile.getExpenseCount() + 1);
        profile.setLastExpenseAmount(event.amount());
        profile.setLastExpenseCategory(event.category());
        profile.setLastExpenseAt(event.occurredAt());

        if (profile.getLargestExpenseAmount() == null || event.amount().compareTo(profile.getLargestExpenseAmount()) > 0) {
            profile.setLargestExpenseAmount(event.amount());
        }

        profile.setUpdatedAt(LocalDateTime.now());
        transactionProfileRepository.save(profile);
    }

    @Transactional
    public void handleRevenueEvent(RevenueActivityEvent event) {
        TransactionProfile profile = getProfileOrThrow(event.userId());

        BigDecimal newAverage = calculateNewAverage(profile.getAverageRevenueAmount(), profile.getRevenueCount(), event.amount());

        profile.setAverageRevenueAmount(newAverage);
        profile.setRevenueCount(profile.getRevenueCount() + 1);
        profile.setLastRevenueAmount(event.amount());
        profile.setLastRevenueCategory(event.category());
        profile.setLastRevenueAt(event.occurredAt());
        profile.setUpdatedAt(LocalDateTime.now());

        transactionProfileRepository.save(profile);
    }


    @Transactional
    public void handlePiggyBankEvent(PiggyBankActivityEvent event) {
        if (event.type() != PiggyBankActivityType.AMOUNT_ADDED_TO_PIGGY_BANK_DIRECTLY) {
            return;
        }

        TransactionProfile profile = getProfileOrThrow(event.userId());

        profile.setLastPiggyBankDepositAmount(event.amountPaid());
        profile.setLastPiggyBankDepositAt(event.occurredAt());

        if (profile.getLargestPiggyBankDeposit() == null
                || event.amountPaid().compareTo(profile.getLargestPiggyBankDeposit()) > 0) {
            profile.setLargestPiggyBankDeposit(event.amountPaid());
        }

        profile.setUpdatedAt(LocalDateTime.now());
        transactionProfileRepository.save(profile);
    }


    private BigDecimal calculateNewAverage(BigDecimal currentAverage, Long currentCount, BigDecimal newValue) {
        if (currentAverage == null || currentCount == 0) {
            return newValue;
        }
        BigDecimal totalCount = BigDecimal.valueOf(currentCount + 1);
        BigDecimal currentTotal = currentAverage.multiply(BigDecimal.valueOf(currentCount));
        return currentTotal.add(newValue).divide(totalCount, 2, RoundingMode.HALF_UP);
    }

    private TransactionProfile getProfileOrThrow(Long userId) {
        return transactionProfileRepository.findByUserId(userId).orElseThrow(()
                -> new RequestedEntityNotFoundException("Transaction profile not found for userId=" + userId));
    }

    @Override
    @Transactional
    public void deleteByUserId(Long userId) {
        transactionProfileRepository.deleteByUserId(userId);
        log.info("Deleted transaction profile for userId={}", userId);
    }
}
