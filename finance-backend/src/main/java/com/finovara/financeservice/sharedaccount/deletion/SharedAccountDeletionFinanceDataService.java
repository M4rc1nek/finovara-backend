package com.finovara.financeservice.sharedaccount.deletion;

import com.finovara.contracts.activity.event.sharedaccount.SharedAccountActivityEvent;
import com.finovara.contracts.notification.event.sharedaccount.deletion.SharedAccountDeletedEvent;
import com.finovara.contracts.exception.notfound.RequestedEntityNotFoundException;
import com.finovara.contracts.model.activity.SharedAccountActivityType;
import com.finovara.contracts.outbox.OutboxService;
import com.finovara.financeservice.sharedaccount.expense.repository.SharedExpenseRepository;
import com.finovara.financeservice.sharedaccount.limit.repository.SharedLimitRepository;
import com.finovara.financeservice.sharedaccount.note.repository.SharedAccountNoteRepository;
import com.finovara.financeservice.sharedaccount.piggybank.repository.SharedPiggyBankRepository;
import com.finovara.financeservice.sharedaccount.revenue.model.SharedRevenueRepository;
import com.finovara.financeservice.sharedaccount.settings.SharedAccountSettingsRepository;
import com.finovara.financeservice.sharedaccount.wallet.repository.SharedWalletRepository;
import com.finovara.financeservice.wallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class SharedAccountDeletionFinanceDataService {

    private final WalletService walletService;
    private final SharedExpenseRepository sharedExpenseRepository;
    private final SharedRevenueRepository sharedRevenueRepository;
    private final SharedWalletRepository sharedWalletRepository;
    private final SharedPiggyBankRepository sharedPiggyBankRepository;
    private final SharedLimitRepository sharedLimitRepository;
    private final SharedAccountNoteRepository sharedAccountNoteRepository;
    private final SharedAccountSettingsRepository sharedAccountSettingsRepository;
    private final OutboxService outboxService;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean deleteData(SharedAccountDeletedEvent event) {
        Long ownerId = Objects.requireNonNull(event.ownerId(), "event.ownerId must not be null");
        Long memberId = Objects.requireNonNull(event.memberId(), "event.memberId must not be null");

        if (!sharedWalletRepository.existsByOwnerIdAndMemberId(ownerId, memberId)) {
            log.info("Shared account financial data already deleted for accountId={}, ownerId={}, memberId={} " +
                    "(duplicate Kafka delivery) — skipping.", event.accountId(), ownerId, memberId);
            return false;
        }

        refundContributedRevenue(event);

        deleteSharedFinancialData(ownerId, memberId);

        log.info("Deleted shared financial data for accountId={}, ownerId={}, memberId={}",
                event.accountId(), ownerId, memberId);
        return true;
    }

    private void refundContributedRevenue(SharedAccountDeletedEvent event) {
        Long ownerId = event.ownerId();
        Long memberId = event.memberId();

        NetContribution ownerContribution = calculateNetContribution(ownerId, memberId, ownerId);
        NetContribution memberContribution = calculateNetContribution(ownerId, memberId, memberId);

        BigDecimal ownerRefund;
        BigDecimal memberRefund;

        if (ownerContribution.isNegative()) {
            ownerRefund = BigDecimal.ZERO;
            memberRefund = ownerContribution.net().add(memberContribution.net()).max(BigDecimal.ZERO);
        } else if (memberContribution.isNegative()) {
            memberRefund = BigDecimal.ZERO;
            ownerRefund = ownerContribution.net().add(memberContribution.net()).max(BigDecimal.ZERO);
        } else {
            ownerRefund = ownerContribution.net();
            memberRefund = memberContribution.net();
        }

        doRefund(new RefundInstruction(ownerId, event.memberUsername(), event.memberEmail(), ownerRefund, ownerContribution));
        doRefund(new RefundInstruction(memberId, event.ownerUsername(), event.ownerEmail(), memberRefund, memberContribution));
    }

    private NetContribution calculateNetContribution(Long ownerId, Long memberId, Long contributorId) {
        BigDecimal revenue = sharedRevenueRepository.sumRevenueByCreatedByUserId(ownerId, memberId, contributorId);
        BigDecimal expense = sharedExpenseRepository.sumExpenseByCreatedByUserId(ownerId, memberId, contributorId);
        return new NetContribution(revenue, expense);
    }

    private void doRefund(RefundInstruction instruction) {
        if (instruction.amount().compareTo(BigDecimal.ZERO) <= 0) {
            log.info("No refund for userId={} — insufficient shared account balance after deficit reconciliation " +
                            "(revenue={}, expense={})",
                    instruction.userId(), instruction.contribution().revenue(), instruction.contribution().expense());
            return;
        }

        try {
            walletService.addBalanceToWallet(instruction.userId(), instruction.amount());
        } catch (RequestedEntityNotFoundException ex) {
            log.warn("Refund skipped for userId={} — personal wallet no longer exists " +
                    "(likely concurrent full account deletion). amount={}", instruction.userId(), instruction.amount());
            return;
        }

        outboxService.save("User", instruction.userId().toString(), "activity.shared-account",
                new SharedAccountActivityEvent(instruction.userId(), SharedAccountActivityType.REFUND_BALANCE_AFTER_LEFT_SHARED_ACCOUNT,
                        instruction.amount(), instruction.coFounderUsername(), instruction.coFounderEmail(), LocalDateTime.now()));

        log.info("Refunded net contribution={} (revenue={}, expense={}) to userId={}",
                instruction.amount(), instruction.contribution().revenue(), instruction.contribution().expense(), instruction.userId());
    }

    private record NetContribution(BigDecimal revenue, BigDecimal expense) {
        BigDecimal net() {
            return revenue.subtract(expense);
        }

        boolean isNegative() {
            return net().compareTo(BigDecimal.ZERO) < 0;
        }
    }

    private void deleteSharedFinancialData(Long ownerId, Long memberId) {
        sharedExpenseRepository.deleteAllByOwnerIdAndMemberId(ownerId, memberId);
        sharedRevenueRepository.deleteAllByOwnerIdAndMemberId(ownerId, memberId);
        sharedWalletRepository.deleteByOwnerIdAndMemberId(ownerId, memberId);
        sharedPiggyBankRepository.deleteByOwnerIdAndMemberId(ownerId, memberId);
        sharedLimitRepository.deleteByOwnerIdAndMemberId(ownerId, memberId);
        sharedAccountNoteRepository.deleteByOwnerIdAndMemberId(ownerId,memberId);
        sharedAccountSettingsRepository.deleteByOwnerIdAndMemberId(ownerId, memberId);
    }

    private record RefundInstruction(Long userId, String coFounderUsername, String coFounderEmail,
                                     BigDecimal amount, NetContribution contribution) {
    }
}