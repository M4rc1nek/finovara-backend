package com.finovara.authbackend.revenue.service;

import com.finovara.contracts.event.activity.revenue.RevenueActivityEvent;
import com.finovara.contracts.model.activity.RevenueActivityType;
import com.finovara.contracts.model.transaction.RevenueCategory;
import com.finovara.contracts.exception.notfound.RequestedEntityNotFoundException;
import com.finovara.authbackend.revenue.dto.RevenueDto;
import com.finovara.authbackend.revenue.mapper.RevenueMapper;
import com.finovara.authbackend.revenue.model.Revenue;
import com.finovara.authbackend.revenue.repository.RevenueRepository;
import com.finovara.authbackend.usersetting.piggybank.autopayments.model.PiggyBankAutomationMode;
import com.finovara.authbackend.usersetting.piggybank.autopayments.service.AutoPaymentsService;
import com.finovara.authbackend.util.revenue.RevenueManagerService;
import com.finovara.authbackend.wallet.model.Wallet;
import com.finovara.authbackend.wallet.repository.WalletRepository;
import com.finovara.authbackend.wallet.service.WalletService;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RevenueService {

    private final RevenueRepository revenueRepository;
    private final WalletRepository walletRepository;
    private final WalletService walletService;
    private final RevenueManagerService revenueManagerService;
    private final RevenueMapper revenueMapper;
    private final AutoPaymentsService autoPaymentsService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Transactional
    public Long addRevenue(RevenueDto revenueDto, Long userId) {
        Revenue revenue = Revenue.builder()
                .amount(revenueDto.amount())
                .category(revenueDto.category())
                .createdAt(LocalDate.now())
                .description(revenueDto.description())
                .userId(userId)
                .build();
        walletService.addBalanceToWallet(userId, revenue.getAmount());
        kafkaTemplate.send("activity.revenue", new RevenueActivityEvent(userId, RevenueActivityType.ADDED_REVENUE, revenue.getAmount(), revenue.getCategory(), null, null, LocalDateTime.now()));
        revenueRepository.save(revenue);
        autoPaymentsService.handleRevenuePiggyBankAutomation(userId, revenue.getAmount(), PiggyBankAutomationMode.APPLY);

        return revenue.getId();
    }

    @Transactional
    public Long editRevenue(RevenueDto revenueDto, Long revenueId, Long userId) {
        Revenue existingRevenue = revenueManagerService.getRevenueOrThrow(revenueId);

        if (!existingRevenue.getUserId().equals(userId)) {
            throw new RequestedEntityNotFoundException("Revenue not found for this user");
        }

        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new RequestedEntityNotFoundException("Wallet not found"));

        BigDecimal oldAmount = existingRevenue.getAmount();
        BigDecimal newAmount = revenueDto.amount();
        RevenueCategory oldCategory = existingRevenue.getCategory();

        autoPaymentsService.handleRevenuePiggyBankAutomation(userId, oldAmount, PiggyBankAutomationMode.ROLLBACK);

        wallet.withdraw(oldAmount);
        wallet.deposit(newAmount);

        existingRevenue.setAmount(revenueDto.amount());
        existingRevenue.setCategory(revenueDto.category());
        existingRevenue.setDescription(revenueDto.description());

        kafkaTemplate.send("activity.revenue", new RevenueActivityEvent(userId, RevenueActivityType.EDITED_REVENUE, existingRevenue.getAmount(), existingRevenue.getCategory(), oldAmount, oldCategory, LocalDateTime.now()));
        autoPaymentsService.handleRevenuePiggyBankAutomation(userId, newAmount, PiggyBankAutomationMode.APPLY);

        walletRepository.save(wallet);
        revenueRepository.save(existingRevenue);

        return revenueId;
    }

    public List<RevenueDto> getRevenue(Long userId) {
        List<Revenue> revenue = revenueRepository.findAllByUserId(userId);

        return revenue.stream()
                .map(revenueMapper::mapRevenueToDto)
                .toList();
    }

    @Transactional
    public void deleteRevenue(Long revenueId, Long userId) {
        Revenue revenue = revenueRepository.findByIdAndUserId(revenueId, userId)
                .orElseThrow(() -> new RequestedEntityNotFoundException("Revenue not found"));
        autoPaymentsService.handleRevenuePiggyBankAutomation(userId, revenue.getAmount(), PiggyBankAutomationMode.ROLLBACK);
        walletService.removeBalanceFromWallet(userId, revenue.getAmount());
        kafkaTemplate.send("activity.revenue", new RevenueActivityEvent(userId, RevenueActivityType.DELETED_REVENUE, revenue.getAmount(), revenue.getCategory(), null, null, LocalDateTime.now()));
        revenueRepository.delete(revenue);
    }
}
