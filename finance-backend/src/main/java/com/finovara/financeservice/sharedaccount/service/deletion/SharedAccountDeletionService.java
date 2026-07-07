package com.finovara.financeservice.sharedaccount.service.deletion;

import com.finovara.contracts.event.finance.sharedaccount.SharedAccountDeletedEvent;
import com.finovara.financeservice.sharedaccount.repository.expense.SharedExpenseRepository;
import com.finovara.financeservice.sharedaccount.repository.revenue.SharedRevenueRepository;
import com.finovara.financeservice.sharedaccount.repository.wallet.SharedWalletRepository;
import com.finovara.financeservice.wallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class SharedAccountDeletionService {

    private final WalletService walletService;
    private final SharedExpenseRepository sharedExpenseRepository;
    private final SharedRevenueRepository sharedRevenueRepository;
    private final SharedWalletRepository sharedWalletRepository;

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "wallet:shared", key = "#event.ownerId()"),
            @CacheEvict(value = "wallet:shared", key = "#event.memberId()"),
            @CacheEvict(value = "wallet:user", key = "#event.ownerId()"),
            @CacheEvict(value = "wallet:user", key = "#event.memberId()")
    })
    public void deleteData(SharedAccountDeletedEvent event) {
        Long ownerId = event.ownerId();
        Long memberId = event.memberId();

        refundContributedRevenue(ownerId, memberId);

        sharedExpenseRepository.deleteAllByOwnerIdAndMemberId(ownerId, memberId);
        sharedRevenueRepository.deleteAllByOwnerIdAndMemberId(ownerId, memberId);
        sharedWalletRepository.deleteByOwnerIdAndMemberId(ownerId, memberId);

        log.info("Deleted shared financial data for accountId={}, ownerId={}, memberId={}",
                event.accountId(), ownerId, memberId);
    }

    private void refundContributedRevenue(Long ownerId, Long memberId) {

        BigDecimal ownerRevenue = sharedRevenueRepository.sumRevenueByCreatedByUserId(ownerId, memberId, ownerId);
        BigDecimal ownerExpense = sharedExpenseRepository.sumExpenseByCreatedByUserId(ownerId, memberId, ownerId);
        BigDecimal ownerNet = ownerRevenue.subtract(ownerExpense);

        BigDecimal memberRevenue = sharedRevenueRepository.sumRevenueByCreatedByUserId(ownerId, memberId, memberId);
        BigDecimal memberExpense = sharedExpenseRepository.sumExpenseByCreatedByUserId(ownerId, memberId, memberId);
        BigDecimal memberNet = memberRevenue.subtract(memberExpense);

        BigDecimal ownerRefund;
        BigDecimal memberRefund;

        if (ownerNet.compareTo(BigDecimal.ZERO) < 0) {
            ownerRefund = BigDecimal.ZERO;
            memberRefund = ownerNet.add(memberNet).max(BigDecimal.ZERO);
        } else if (memberNet.compareTo(BigDecimal.ZERO) < 0) {
            memberRefund = BigDecimal.ZERO;
            ownerRefund = ownerNet.add(memberNet).max(BigDecimal.ZERO);
        } else {
            ownerRefund = ownerNet;
            memberRefund = memberNet;
        }

        doRefund(ownerId, ownerId, memberId, ownerRefund, ownerRevenue, ownerExpense);
        doRefund(memberId, ownerId, memberId, memberRefund, memberRevenue, memberExpense);
    }

    private void doRefund(Long userId, Long ownerId, Long memberId,
                          BigDecimal refundAmount, BigDecimal contributedRevenue, BigDecimal contributedExpense) {

        if (refundAmount.compareTo(BigDecimal.ZERO) > 0) {
            walletService.addBalanceToWallet(userId, refundAmount);
            log.info("Refunded net contribution={} (revenue={}, expense={}) to userId={} from shared account ownerId={}, memberId={}",
                    refundAmount, contributedRevenue, contributedExpense, userId, ownerId, memberId);
        } else {
            log.info("No refund for userId={} — insufficient shared account balance after deficit reconciliation (revenue={}, expense={})",
                    userId, contributedRevenue, contributedExpense);
        }
    }
}