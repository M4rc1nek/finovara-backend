package com.finovara.financeservice.revenue.service;

import com.finovara.contracts.activity.event.revenue.RevenueActivityEvent;
import com.finovara.contracts.datadeletable.UserDataDeletable;
import com.finovara.contracts.exception.notfound.RequestedEntityNotFoundException;
import com.finovara.contracts.model.activity.RevenueActivityType;
import com.finovara.contracts.model.transaction.RevenueCategory;
import com.finovara.contracts.outbox.OutboxService;
import com.finovara.contracts.securitymonitoring.dto.RiskTriggerType;
import com.finovara.financeservice.feignclient.AuthBackendClient;
import com.finovara.financeservice.revenue.dto.RevenueDto;
import com.finovara.financeservice.revenue.mapper.RevenueMapper;
import com.finovara.financeservice.revenue.model.Revenue;
import com.finovara.financeservice.revenue.repository.RevenueRepository;
import com.finovara.financeservice.riskverification.service.RiskGuardService;
import com.finovara.financeservice.settings.piggybank.autopayments.model.PiggyBankAutomationMode;
import com.finovara.financeservice.settings.piggybank.autopayments.service.AutoPaymentsService;
import com.finovara.financeservice.util.transaction.TransactionOrigin;
import com.finovara.financeservice.util.transaction.revenue.RevenueManagerService;
import com.finovara.financeservice.wallet.service.WalletService;
import com.finovara.contracts.authorization.additionalcode.resolver.AdditionalAuthorizationCodeResolver;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final RiskGuardService riskGuardService;

    @Transactional
    @CacheEvict(value = "revenue:suggestion", key = "#userId")
    public Long addRevenue(RevenueDto revenueDto, Long userId, HttpServletRequest servletRequest, TransactionOrigin origin) {
        if (origin == TransactionOrigin.USER_MANUAL) {
            authBackendClient.confirmAuthorizationCode(userId, additionalAuthorizationCodeResolver.resolve(revenueDto.authorizationCode()));
            riskGuardService.guard(userId, RiskTriggerType.REVENUE, revenueDto.amount(), revenueDto.category().name(),
                    authBackendClient.getUserEmail(userId), revenueDto.riskVerificationSourceEventId(), servletRequest);
        }

        Revenue revenue = Revenue.builder()
                .amount(revenueDto.amount())
                .category(revenueDto.category())
                .createdAt(LocalDate.now())
                .description(revenueDto.description())
                .userId(userId)
                .build();

        walletService.addBalanceToWallet(userId, revenue.getAmount());
        revenueRepository.save(revenue);
        outboxService.save("Revenue", revenue.getId().toString(), "revenue.created",
                new RevenueActivityEvent(userId, RevenueActivityType.ADDED_REVENUE, revenue.getAmount(), revenue.getCategory(), null, null, LocalDateTime.now()));
        autoPaymentsService.handleRevenuePiggyBankAutomation(userId, revenue.getAmount(), PiggyBankAutomationMode.APPLY);
        return revenue.getId();
    }

    @Transactional
    @CacheEvict(value = "revenue:suggestion", key = "#userId")
    public Long editRevenue(RevenueDto revenueDto, Long revenueId, Long userId, HttpServletRequest servletRequest) {
        authBackendClient.confirmAuthorizationCode(userId, additionalAuthorizationCodeResolver.resolve(revenueDto.authorizationCode()));

        Revenue existingRevenue = revenueManagerService.getRevenueOrThrow(revenueId);

        if (!existingRevenue.getUserId().equals(userId)) {
            throw new RequestedEntityNotFoundException("Revenue not found for this user");
        }

        riskGuardService.guard(userId, RiskTriggerType.REVENUE, revenueDto.amount(), revenueDto.category().name(),
                authBackendClient.getUserEmail(userId), revenueDto.riskVerificationSourceEventId(), servletRequest);

        BigDecimal oldAmount = existingRevenue.getAmount();
        BigDecimal newAmount = revenueDto.amount();
        RevenueCategory oldCategory = existingRevenue.getCategory();

        autoPaymentsService.handleRevenuePiggyBankAutomation(userId, oldAmount, PiggyBankAutomationMode.ROLLBACK);

        walletService.addBalanceToWallet(userId, newAmount);
        walletService.removeBalanceFromWallet(userId, oldAmount);

        existingRevenue.setAmount(revenueDto.amount());
        existingRevenue.setCategory(revenueDto.category());
        existingRevenue.setDescription(revenueDto.description());

        revenueRepository.save(existingRevenue);

        outboxService.save("Revenue", revenueId.toString(), "revenue.created",
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
    @CacheEvict(value = "revenue:suggestion", key = "#userId")
    public void deleteRevenue(Long revenueId, Long userId, String authorizationCode) {
        authBackendClient.confirmAuthorizationCode(userId, additionalAuthorizationCodeResolver.resolve(authorizationCode));

        Revenue revenue = revenueRepository.findByIdAndUserId(revenueId, userId)
                .orElseThrow(() -> new RequestedEntityNotFoundException("Revenue not found"));
        autoPaymentsService.handleRevenuePiggyBankAutomation(userId, revenue.getAmount(), PiggyBankAutomationMode.ROLLBACK);
        walletService.removeBalanceFromWallet(userId, revenue.getAmount());
        outboxService.save("Revenue", revenueId.toString(), "revenue.created",
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