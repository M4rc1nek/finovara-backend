package com.finovara.financeservice.sharedaccount.revenue.service;

import com.finovara.contracts.exception.badrequest.InvalidInputException;
import com.finovara.contracts.exception.notfound.RequestedEntityNotFoundException;
import com.finovara.contracts.outbox.OutboxService;
import com.finovara.contracts.sharedaccount.SharedAccountActivityLogType;
import com.finovara.contracts.sharedaccount.event.activity.SharedAccountActivityLogEvent;
import com.finovara.financeservice.sharedaccount.participants.SharedAccountParticipantsResponse;
import com.finovara.financeservice.sharedaccount.participants.SharedAccountParticipantsService;
import com.finovara.financeservice.sharedaccount.revenue.dto.SharedRevenueDto;
import com.finovara.financeservice.sharedaccount.revenue.dto.SharedRevenueResponse;
import com.finovara.financeservice.sharedaccount.revenue.mapper.SharedRevenueMapper;
import com.finovara.financeservice.sharedaccount.revenue.model.SharedRevenue;
import com.finovara.financeservice.sharedaccount.revenue.repository.SharedRevenueRepository;
import com.finovara.financeservice.sharedaccount.wallet.service.SharedWalletService;
import com.finovara.financeservice.util.sharedaccount.SharedUsernameResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SharedRevenueService {

    private final SharedRevenueMapper sharedRevenueMapper;
    private final SharedRevenueRepository sharedRevenueRepository;
    private final SharedAccountParticipantsService sharedAccountParticipantsService;
    private final SharedWalletService sharedWalletService;
    private final SharedUsernameResolver usernameResolver;
    private final OutboxService outboxService;

    @Value("${finance.pagination.page-size}")
    private int pageSize;

    @Transactional
    public SharedRevenueResponse addSharedRevenue(SharedRevenueDto dto, Long userId) {
        validateAmount(dto.amount());

        SharedAccountParticipantsResponse participants = sharedAccountParticipantsService.getParticipants(userId);
        String createdByUsername = usernameResolver.resolve(List.of(userId)).get(userId);

        sharedWalletService.addBalanceToWallet(userId, dto.amount());

        SharedRevenue saved = sharedRevenueRepository.save(SharedRevenue.builder()
                .amount(dto.amount())
                .category(dto.category())
                .createdAt(LocalDate.now())
                .description(dto.description())
                .ownerId(participants.ownerId())
                .memberId(participants.memberId())
                .createdByUserId(userId)
                .build());

        logActivity(participants, userId, saved.getId(), SharedAccountActivityLogType.REVENUE_CREATED);
        return new SharedRevenueResponse(saved.getId(), userId, createdByUsername);
    }

    @Transactional
    public Long editRevenue(SharedRevenueDto dto, Long revenueId, Long userId) {
        validateAmount(dto.amount());

        sharedWalletService.lockWalletForUpdate(userId);
        SharedRevenue existing = getSharedRevenueByUserIdOrThrow(userId, revenueId);

        applyWalletDelta(userId, existing.getAmount(), dto.amount());

        existing.setAmount(dto.amount());
        existing.setCategory(dto.category());
        existing.setDescription(dto.description());

        logActivity(sharedAccountParticipantsService.getParticipants(userId), userId, revenueId, SharedAccountActivityLogType.REVENUE_EDITED);

        return revenueId;
    }

    @Transactional(readOnly = true)
    public List<SharedRevenueDto> getRevenue(Long userId, int page) {
        List<SharedRevenue> revenues = sharedRevenueRepository.findAllByOwnerIdOrMemberId(userId, PageRequest.of(page, pageSize));

        Map<Long, String> usernameById = usernameResolver.resolve(
                revenues.stream().map(SharedRevenue::getCreatedByUserId).collect(Collectors.toSet()));

        return revenues.stream()
                .map(revenue -> sharedRevenueMapper.mapToDto(revenue, usernameById.get(revenue.getCreatedByUserId())))
                .toList();
    }

    @Transactional
    public void deleteRevenue(Long revenueId, Long userId) {
        sharedWalletService.lockWalletForUpdate(userId);
        SharedRevenue revenue = getSharedRevenueByUserIdOrThrow(userId, revenueId);

        sharedWalletService.removeBalanceFromWallet(userId, revenue.getAmount());
        sharedRevenueRepository.delete(revenue);

        logActivity(sharedAccountParticipantsService.getParticipants(userId), userId, revenueId, SharedAccountActivityLogType.REVENUE_DELETED);
    }

    public SharedRevenue getSharedRevenueByUserIdOrThrow(Long userId, Long revenueId) {
        return sharedRevenueRepository.findByIdAndOwnerIdOrMemberId(revenueId, userId)
                .orElseThrow(() -> new RequestedEntityNotFoundException("Revenue not found"));
    }

    private void applyWalletDelta(Long userId, BigDecimal oldAmount, BigDecimal newAmount) {
        BigDecimal delta = newAmount.subtract(oldAmount);
        if (delta.signum() > 0) {
            sharedWalletService.addBalanceToWallet(userId, delta);
        } else if (delta.signum() < 0) {
            sharedWalletService.removeBalanceFromWallet(userId, delta.negate());
        }
    }

    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new InvalidInputException("Revenue amount must be positive");
        }
    }

    private void logActivity(SharedAccountParticipantsResponse participants, Long userId, Long revenueId, SharedAccountActivityLogType type) {
        outboxService.save("SharedAccountRevenue", revenueId.toString(), "shared-account.activity",
                new SharedAccountActivityLogEvent(participants.ownerId(), participants.memberId(), userId, revenueId, type, LocalDateTime.now()));
    }
}