package com.finovara.financeservice.sharedaccount.deletion;

import com.finovara.financeservice.sharedaccount.expense.model.SharedExpense;
import com.finovara.financeservice.sharedaccount.expense.repository.SharedExpenseRepository;
import com.finovara.financeservice.sharedaccount.limit.model.SharedLimit;
import com.finovara.financeservice.sharedaccount.limit.repository.SharedLimitRepository;
import com.finovara.financeservice.sharedaccount.note.model.SharedAccountNote;
import com.finovara.financeservice.sharedaccount.note.repository.SharedAccountNoteRepository;
import com.finovara.financeservice.sharedaccount.piggybank.model.SharedPiggyBank;
import com.finovara.financeservice.sharedaccount.piggybank.repository.SharedPiggyBankRepository;
import com.finovara.financeservice.sharedaccount.revenue.model.SharedRevenue;
import com.finovara.financeservice.sharedaccount.revenue.repository.SharedRevenueRepository;
import com.finovara.financeservice.sharedaccount.settings.SharedAccountSettings;
import com.finovara.financeservice.sharedaccount.settings.SharedAccountSettingsRepository;
import com.finovara.financeservice.sharedaccount.wallet.repository.SharedWalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
public class SharedAccountFinanceDeleter {

    private final SharedExpenseRepository sharedExpenseRepository;
    private final SharedRevenueRepository sharedRevenueRepository;
    private final SharedWalletRepository sharedWalletRepository;
    private final SharedPiggyBankRepository sharedPiggyBankRepository;
    private final SharedLimitRepository sharedLimitRepository;
    private final SharedAccountNoteRepository sharedAccountNoteRepository;
    private final SharedAccountSettingsRepository sharedAccountSettingsRepository;

    @Transactional
    public void deleteAll(Long ownerId, Long memberId) {
        deleteExpenses(ownerId, memberId);
        deleteRevenues(ownerId, memberId);
        deletePiggyBanks(ownerId, memberId);
        deleteLimits(ownerId, memberId);
        deleteNotes(ownerId, memberId);
        deleteSettings(ownerId, memberId);
        deleteWallets(ownerId, memberId);
    }

    private void deleteExpenses(Long ownerId, Long memberId) {
        List<SharedExpense> expenses = sharedExpenseRepository.findAllByOwnerIdAndMemberId(ownerId, memberId);
        sharedExpenseRepository.deleteAll(expenses);
    }

    private void deleteRevenues(Long ownerId, Long memberId) {
        List<SharedRevenue> revenues = sharedRevenueRepository.findAllByOwnerIdAndMemberId(ownerId, memberId);
        sharedRevenueRepository.deleteAll(revenues);
    }

    private void deletePiggyBanks(Long ownerId, Long memberId) {
        List<SharedPiggyBank> piggyBanks = sharedPiggyBankRepository.findAllByOwnerIdAndMemberId(ownerId, memberId);
        sharedPiggyBankRepository.deleteAll(piggyBanks);
    }

    private void deleteLimits(Long ownerId, Long memberId) {
        List<SharedLimit> limits = sharedLimitRepository.findAllByOwnerIdAndMemberId(ownerId, memberId);
        sharedLimitRepository.deleteAll(limits);
    }

    private void deleteNotes(Long ownerId, Long memberId) {
        List<SharedAccountNote> notes = sharedAccountNoteRepository.findAllByOwnerIdAndMemberId(ownerId, memberId);
        sharedAccountNoteRepository.deleteAll(notes);
    }

    private void deleteSettings(Long ownerId, Long memberId) {
        List<SharedAccountSettings> settings = sharedAccountSettingsRepository.findAllByOwnerIdAndMemberId(ownerId, memberId);
        sharedAccountSettingsRepository.deleteAll(settings);
    }

    private void deleteWallets(Long ownerId, Long memberId) {
        sharedWalletRepository.findByOwnerIdAndMemberId(ownerId, memberId)
                .ifPresent(sharedWalletRepository::delete);
    }
}