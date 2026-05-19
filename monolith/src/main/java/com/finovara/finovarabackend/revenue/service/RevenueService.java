package com.finovara.finovarabackend.revenue.service;

import com.finovara.finovarabackend.accountactivity.revenue.model.RevenueActivityType;
import com.finovara.finovarabackend.accountactivity.revenue.service.RevenueActivityService;
import com.finovara.finovarabackend.exception.notfound.WalletNotFoundException;
import com.finovara.finovarabackend.revenue.dto.RevenueDto;
import com.finovara.finovarabackend.revenue.exception.notfound.RevenueNotFoundException;
import com.finovara.finovarabackend.revenue.mapper.RevenueMapper;
import com.finovara.finovarabackend.revenue.model.Revenue;
import com.finovara.finovarabackend.revenue.model.RevenueCategory;
import com.finovara.finovarabackend.revenue.repository.RevenueRepository;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.usersetting.piggybank.autopayments.model.PiggyBankAutomationMode;
import com.finovara.finovarabackend.usersetting.piggybank.autopayments.service.AutoPaymentsService;
import com.finovara.finovarabackend.util.revenue.RevenueManagerService;
import com.finovara.finovarabackend.util.user.service.UserManagerService;
import com.finovara.finovarabackend.wallet.model.Wallet;
import com.finovara.finovarabackend.wallet.repository.WalletRepository;
import com.finovara.finovarabackend.wallet.service.WalletService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RevenueService {

    private final UserManagerService userManagerService;
    private final RevenueRepository revenueRepository;
    private final WalletRepository walletRepository;
    private final WalletService walletService;
    private final RevenueManagerService revenueManagerService;
    private final RevenueMapper revenueMapper;
    private final AutoPaymentsService autoPaymentsService;
    private final RevenueActivityService revenueActivityService;

    @Transactional
    public Long addRevenue(RevenueDto revenueDto, Long userId) {
        User user = userManagerService.getUserByIdOrThrow(userId);

        Revenue revenue = Revenue.builder()
                .amount(revenueDto.amount())
                .category(revenueDto.category())
                .createdAt(LocalDate.now())
                .description(revenueDto.description())
                .userAssigned(user)
                .build();
        walletService.addBalanceToWallet(userId, revenue.getAmount());
        revenueActivityService.createRevenueActivity(userId, RevenueActivityType.ADDED_REVENUE, revenue);
        //wallet jest zapisywany w repo klasie WalletService
        revenueRepository.save(revenue);
        autoPaymentsService.handleRevenuePiggyBankAutomation(userId, revenue.getAmount(), PiggyBankAutomationMode.APPLY);

        return revenue.getId();
    }

    @Transactional
    public Long editRevenue(RevenueDto revenueDto, Long revenueId, Long userId) {
        Revenue existingRevenue = revenueManagerService.getRevenueOrThrow(revenueId);
        User user = userManagerService.getUserByIdOrThrow(userId);

        if (!existingRevenue.getUserAssigned().getId().equals(user.getId())) {
            throw new RevenueNotFoundException("Revenue not found for this user");
        }

        Wallet wallet = walletRepository.findByUserAssignedId(userId)
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found"));

        BigDecimal oldAmount = existingRevenue.getAmount();
        BigDecimal newAmount = revenueDto.amount();
        RevenueCategory oldCategory = existingRevenue.getCategory();

        autoPaymentsService.handleRevenuePiggyBankAutomation(userId, oldAmount, PiggyBankAutomationMode.ROLLBACK);

        wallet.withdraw(oldAmount);
        wallet.deposit(newAmount);

        existingRevenue.setAmount(revenueDto.amount());
        existingRevenue.setCategory(revenueDto.category());
        existingRevenue.setDescription(revenueDto.description());

        revenueActivityService.updateRevenueActivity(userId, RevenueActivityType.EDITED_REVENUE, existingRevenue, oldAmount, oldCategory);
        autoPaymentsService.handleRevenuePiggyBankAutomation(userId, newAmount, PiggyBankAutomationMode.APPLY);

        walletRepository.save(wallet);
        revenueRepository.save(existingRevenue);

        return revenueId;
    }

    public List<RevenueDto> getRevenue(Long userId) {
        User user = userManagerService.getUserByIdOrThrow(userId);
        List<Revenue> revenue = revenueRepository.findAllByUserAssignedId(user.getId());

        return revenue.stream()
                .map(revenueMapper::mapRevenueToDto)
                .toList();
    }

    @Transactional
    public void deleteRevenue(Long revenueId, Long userId) {
        User user = userManagerService.getUserByIdOrThrow(userId);
        Revenue revenue = revenueRepository.findByIdAndUserAssignedId(revenueId, user.getId())
                .orElseThrow(() -> new RevenueNotFoundException("Revenue not found"));
        autoPaymentsService.handleRevenuePiggyBankAutomation(userId, revenue.getAmount(), PiggyBankAutomationMode.ROLLBACK);
        walletService.removeBalanceFromWallet(userId, revenue.getAmount());
        revenueActivityService.createRevenueActivity(userId, RevenueActivityType.DELETED_REVENUE, revenue);
        revenueRepository.delete(revenue);
    }
}
