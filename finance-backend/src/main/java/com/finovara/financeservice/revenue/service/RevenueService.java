package com.finovara.financeservice.revenue.service;

import com.finovara.contracts.datadeletable.UserDataDeletable;
import com.finovara.contracts.event.activity.revenue.RevenueActivityEvent;
import com.finovara.contracts.exception.notfound.RequestedEntityNotFoundException;
import com.finovara.contracts.model.activity.RevenueActivityType;
import com.finovara.contracts.model.transaction.RevenueCategory;
import com.finovara.contracts.outbox.OutboxService;
import com.finovara.financeservice.revenue.dto.RevenueDto;
import com.finovara.financeservice.revenue.mapper.RevenueMapper;
import com.finovara.financeservice.revenue.model.Revenue;
import com.finovara.financeservice.revenue.repository.RevenueRepository;
import com.finovara.financeservice.settings.piggybank.autopayments.model.PiggyBankAutomationMode;
import com.finovara.financeservice.settings.piggybank.autopayments.service.AutoPaymentsService;
import com.finovara.financeservice.util.revenue.RevenueManagerService;
import com.finovara.financeservice.wallet.service.WalletService;
import com.finovara.financeservice.feignclient.AuthBackendClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.finovara.contracts.authorization.additionalcode.resolver.AdditionalAuthorizationCodeResolver;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RevenueService implements UserDataDeletable {

    private final OutboxService outboxService;
    private final RevenueRepository revenueRepository;
    private final WalletService walletService;
    private final RevenueManagerService revenueManagerService;
    private final RevenueMapper revenueMapper;
    private final AutoPaymentsService autoPaymentsService;
    private final AuthBackendClient authBackendClient;
    private final AdditionalAuthorizationCodeResolver additionalAuthorizationCodeResolver;

    @Transactional
    public Long addRevenue(RevenueDto revenueDto, Long userId) {
        authBackendClient.confirmAuthorizationCode(userId, additionalAuthorizationCodeResolver.resolve(revenueDto.authorizationCode()));

        Revenue revenue = Revenue.builder()
                .amount(revenueDto.amount())
                .category(revenueDto.category())
                .createdAt(LocalDate.now())
                .description(revenueDto.description())
                .userId(userId)
                .build();
        walletService.addBalanceToWallet(userId, revenue.getAmount());
        revenueRepository.save(revenue);
        outboxService.save("Revenue", revenue.getId().toString(), "activity.revenue",
                new RevenueActivityEvent(userId, RevenueActivityType.ADDED_REVENUE, revenue.getAmount(), revenue.getCategory(), null, null, LocalDateTime.now()));
        autoPaymentsService.handleRevenuePiggyBankAutomation(userId, revenue.getAmount(), PiggyBankAutomationMode.APPLY);
        return revenue.getId();
    }

    @Transactional
    public Long editRevenue(RevenueDto revenueDto, Long revenueId, Long userId) {
        authBackendClient.confirmAuthorizationCode(userId, additionalAuthorizationCodeResolver.resolve(revenueDto.authorizationCode()));

        Revenue existingRevenue = revenueManagerService.getRevenueOrThrow(revenueId);

        if (!existingRevenue.getUserId().equals(userId)) {
            throw new RequestedEntityNotFoundException("Revenue not found for this user");
        }

        BigDecimal oldAmount = existingRevenue.getAmount();
        BigDecimal newAmount = revenueDto.amount();
        RevenueCategory oldCategory = existingRevenue.getCategory();

        autoPaymentsService.handleRevenuePiggyBankAutomation(userId, oldAmount, PiggyBankAutomationMode.ROLLBACK);

        walletService.addBalanceToWallet(userId, newAmount);
        walletService.removeBalanceFromWallet(userId,  oldAmount);

        existingRevenue.setAmount(revenueDto.amount());
        existingRevenue.setCategory(revenueDto.category());
        existingRevenue.setDescription(revenueDto.description());

        revenueRepository.save(existingRevenue);

        outboxService.save("Revenue", revenueId.toString(), "activity.revenue",
                new RevenueActivityEvent(userId, RevenueActivityType.EDITED_REVENUE, existingRevenue.getAmount(), existingRevenue.getCategory(), oldAmount, oldCategory, LocalDateTime.now()));
        autoPaymentsService.handleRevenuePiggyBankAutomation(userId, newAmount, PiggyBankAutomationMode.APPLY);

        return revenueId;
    }

    public List<RevenueDto> getRevenue(Long userId) {
        List<Revenue> revenue = revenueRepository.findAllByUserId(userId);

        return revenue.stream()
                .map(revenueMapper::mapRevenueToDto)
                .toList();
    }

    @Transactional
    public void deleteRevenue(Long revenueId, Long userId, String authorizationCode) {
        authBackendClient.confirmAuthorizationCode(userId, additionalAuthorizationCodeResolver.resolve(authorizationCode));
        
        Revenue revenue = revenueRepository.findByIdAndUserId(revenueId, userId)
                .orElseThrow(() -> new RequestedEntityNotFoundException("Revenue not found"));
        autoPaymentsService.handleRevenuePiggyBankAutomation(userId, revenue.getAmount(), PiggyBankAutomationMode.ROLLBACK);
        walletService.removeBalanceFromWallet(userId, revenue.getAmount());
        outboxService.save("Revenue", revenueId.toString(), "activity.revenue",
                new RevenueActivityEvent(userId, RevenueActivityType.DELETED_REVENUE, revenue.getAmount(), revenue.getCategory(), null, null, LocalDateTime.now()));
        revenueRepository.delete(revenue);
    }

    @Override
    @Transactional
    public void deleteByUserId(Long userId) {
        revenueRepository.deleteByUserId(userId);
        log.info("Deleted revenue for userId={}", userId);
    }
}
