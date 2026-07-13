package com.finovara.financeservice.sharedaccount.revenue.service;

import com.finovara.contracts.exception.notfound.RequestedEntityNotFoundException;
import com.finovara.financeservice.feignclient.AuthBackendClient;
import com.finovara.financeservice.sharedaccount.revenue.dto.SharedRevenueDto;
import com.finovara.financeservice.sharedaccount.revenue.dto.SharedRevenueResponse;
import com.finovara.financeservice.sharedaccount.wallet.dto.SharedWalletDto;
import com.finovara.financeservice.sharedaccount.revenue.model.SharedRevenue;
import com.finovara.financeservice.sharedaccount.revenue.model.SharedRevenueRepository;
import com.finovara.financeservice.sharedaccount.revenue.mapper.SharedRevenueMapper;
import com.finovara.financeservice.sharedaccount.wallet.service.SharedWalletService;
import com.finovara.financeservice.util.revenue.SharedRevenueManagerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SharedRevenueService {

    private final SharedRevenueMapper sharedRevenueMapper;
    private final SharedRevenueRepository sharedRevenueRepository;
    private final SharedWalletService sharedWalletService;
    private final SharedRevenueManagerService sharedRevenueManagerService;
    private final AuthBackendClient authBackendClient;
    @Transactional
    public SharedRevenueResponse addSharedRevenue(SharedRevenueDto sharedRevenueDto, Long userId) {
        SharedWalletDto walletDto = sharedWalletService.getWallet(userId);
        String createdByUsername = authBackendClient.getUsername(userId);

        SharedRevenue revenue = SharedRevenue.builder()
                .amount(sharedRevenueDto.amount())
                .category(sharedRevenueDto.category())
                .createdAt(LocalDate.now())
                .description(sharedRevenueDto.description())
                .ownerId(walletDto.ownerId())
                .memberId(walletDto.memberId())
                .createdByUserId(userId)
                .build();

        sharedWalletService.addBalanceToWallet(userId, revenue.getAmount());
        sharedRevenueRepository.save(revenue);

        return new SharedRevenueResponse(revenue.getId(), userId, createdByUsername);
    }

    @Transactional
    public Long editRevenue(SharedRevenueDto sharedRevenueDto, Long revenueId, Long userId) {
        SharedRevenue existingRevenue = sharedRevenueManagerService.getSharedRevenueOrThrow(revenueId);

        if (!isOwnerOrMember(existingRevenue, userId)) {
            throw new RequestedEntityNotFoundException("Revenue not found for this user");
        }

        sharedWalletService.addBalanceToWallet(userId, sharedRevenueDto.amount());
        sharedWalletService.removeBalanceFromWallet(userId, existingRevenue.getAmount());

        existingRevenue.setAmount(sharedRevenueDto.amount());
        existingRevenue.setCategory(sharedRevenueDto.category());
        existingRevenue.setDescription(sharedRevenueDto.description());

        sharedRevenueRepository.save(existingRevenue);

        return revenueId;
    }

    public List<SharedRevenueDto> getRevenue(Long userId) {
        List<SharedRevenue> revenues = sharedRevenueRepository.findAllByOwnerIdOrMemberId(userId);

        Map<Long, String> usernameById = revenues.stream()
                .map(SharedRevenue::getCreatedByUserId)
                .distinct()
                .collect(Collectors.toMap(id -> id, authBackendClient::getUsername));

        return revenues.stream()
                .map(r -> sharedRevenueMapper.mapToDto(r, usernameById.get(r.getCreatedByUserId())))
                .toList();
    }


    @Transactional
    public void deleteRevenue(Long revenueId, Long userId) {
        SharedRevenue revenue = sharedRevenueRepository.findByIdAndOwnerIdOrMemberId(revenueId, userId)
                .orElseThrow(() -> new RequestedEntityNotFoundException("Revenue not found"));
        sharedWalletService.removeBalanceFromWallet(userId, revenue.getAmount());
        sharedRevenueRepository.delete(revenue);
    }

    private boolean isOwnerOrMember(SharedRevenue revenue, Long userId) {
        return revenue.getOwnerId().equals(userId) || revenue.getMemberId().equals(userId);
    }
}