package com.finovara.financeservice.revenue.service;

import com.finovara.contracts.exception.badrequest.InvalidInputException;
import com.finovara.contracts.mainaccount.activity.event.revenue.RevenueActivityEvent;
import com.finovara.contracts.mainaccount.activity.model.RevenueActivityType;
import com.finovara.contracts.outbox.OutboxService;
import com.finovara.contracts.securitymonitoring.model.RiskTriggerType;
import com.finovara.contracts.user.authorization.additionalcode.resolver.AdditionalAuthorizationCodeResolver;
import com.finovara.contracts.user.datadeletable.UserDataDeletable;
import com.finovara.contracts.util.model.RevenueCategory;
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
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.PageRequest;
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

    @Value("${finance.pagination.page-size}")
    private int pageSize;

    @Transactional
    @CacheEvict(value = "revenue:suggestion", key = "#userId")
    public Long addRevenue(RevenueDto revenueDto, Long userId, HttpServletRequest servletRequest, TransactionOrigin origin) {
        validateAmount(revenueDto.amount());

        if (origin == TransactionOrigin.USER_MANUAL) {
            riskGuardService.guard(userId, RiskTriggerType.REVENUE, revenueDto.amount(), revenueDto.category().name(),
                    authBackendClient.getUserEmail(userId), revenueDto.riskVerificationSourceEventId(), servletRequest);
            authBackendClient.confirmAuthorizationCode(userId, additionalAuthorizationCodeResolver.resolve(revenueDto.authorizationCode()));
        }

        walletService.addBalanceToWallet(userId, revenueDto.amount());
        Revenue revenue = revenueRepository.save(Revenue.builder()
                .amount(revenueDto.amount())
                .category(revenueDto.category())
                .createdAt(LocalDate.now())
                .description(revenueDto.description())
                .userId(userId)
                .build());

        outboxService.save("Revenue", revenue.getId().toString(), "revenue.created",
                new RevenueActivityEvent(userId, RevenueActivityType.ADDED_REVENUE, revenue.getAmount(), revenue.getCategory(), null, null, LocalDateTime.now()));
        autoPaymentsService.handleRevenuePiggyBankAutomation(userId, revenue.getAmount(), PiggyBankAutomationMode.APPLY);
        return revenue.getId();
    }

    @Transactional
    @CacheEvict(value = "revenue:suggestion", key = "#userId")
    public Long editRevenue(RevenueDto revenueDto, Long revenueId, Long userId, HttpServletRequest servletRequest) {
        validateAmount(revenueDto.amount());

        authBackendClient.confirmAuthorizationCode(userId, additionalAuthorizationCodeResolver.resolve(revenueDto.authorizationCode()));
        riskGuardService.guard(userId, RiskTriggerType.REVENUE, revenueDto.amount(), revenueDto.category().name(),
                authBackendClient.getUserEmail(userId), revenueDto.riskVerificationSourceEventId(), servletRequest);

        walletService.lockWalletForUpdate(userId);
        Revenue existing = revenueManagerService.getRevenueByUserIdOrThrow(revenueId, userId);

        BigDecimal oldAmount = existing.getAmount();
        BigDecimal newAmount = revenueDto.amount();
        RevenueCategory oldCategory = existing.getCategory();

        autoPaymentsService.handleRevenuePiggyBankAutomation(userId, oldAmount, PiggyBankAutomationMode.ROLLBACK);

        BigDecimal delta = newAmount.subtract(oldAmount);
        if (delta.signum() > 0) {
            walletService.addBalanceToWallet(userId, delta);
        } else if (delta.signum() < 0) {
            walletService.removeBalanceFromWallet(userId, delta.negate());
        }

        existing.setAmount(newAmount);
        existing.setCategory(revenueDto.category());
        existing.setDescription(revenueDto.description());

        outboxService.save("Revenue", revenueId.toString(), "revenue.created",
                new RevenueActivityEvent(userId, RevenueActivityType.EDITED_REVENUE, existing.getAmount(), existing.getCategory(), oldAmount, oldCategory, LocalDateTime.now()));
        autoPaymentsService.handleRevenuePiggyBankAutomation(userId, newAmount, PiggyBankAutomationMode.APPLY);

        return revenueId;
    }

    @Transactional(readOnly = true)
    public List<RevenueDto> getRevenue(Long userId, int page) {
        return revenueRepository.findAllByUserId(userId, PageRequest.of(page, pageSize)).stream()
                .map(revenueMapper::mapRevenueToDto)
                .toList();
    }

    @Transactional
    @CacheEvict(value = "revenue:suggestion", key = "#userId")
    public void deleteRevenue(Long revenueId, Long userId, String authorizationCode) {
        authBackendClient.confirmAuthorizationCode(userId, additionalAuthorizationCodeResolver.resolve(authorizationCode));

        walletService.lockWalletForUpdate(userId);
        Revenue revenue = revenueManagerService.getRevenueByUserIdOrThrow(revenueId, userId);

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

    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new InvalidInputException("Revenue amount must be positive");
        }
    }
}