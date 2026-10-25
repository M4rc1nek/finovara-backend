package com.finovara.financeservice.sharedaccount.wallet.service;

import com.finovara.financeservice.sharedaccount.wallet.dto.SharedWalletDto;
import com.finovara.financeservice.sharedaccount.wallet.model.SharedWallet;
import com.finovara.financeservice.sharedaccount.wallet.repository.SharedWalletRepository;
import com.finovara.financeservice.util.sharedaccount.wallet.SharedWalletManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SharedWalletService {

    private final SharedWalletRepository sharedWalletRepository;
    private final SharedWalletManager sharedWalletManager;

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "wallet:shared", key = "#ownerId"),
            @CacheEvict(value = "wallet:shared", key = "#memberId")
    })
    public SharedWalletDto createSharedWallet(Long ownerId, Long memberId) {
        Optional<SharedWallet> existing = sharedWalletRepository.findByOwnerIdOrMemberId(ownerId, ownerId);
        if (existing.isPresent()) {
            log.info("Shared wallet already exists for ownerId={}, memberId={}, returning existing", ownerId, memberId);
            return toDto(existing.get());
        }

        SharedWallet wallet = sharedWalletRepository.save(SharedWallet.create(ownerId, memberId));
        log.info("Created shared wallet ownerId={}, memberId={}", ownerId, memberId);
        return toDto(wallet);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public SharedWallet lockWalletForUpdate(Long callerId) {
        return sharedWalletManager.getWalletForUpdateOrThrow(callerId);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "wallet:shared", key = "#result.ownerId"),
            @CacheEvict(value = "wallet:shared", key = "#result.memberId")
    })
    public SharedWalletDto addBalanceToWallet(Long callerId, BigDecimal amount) {
        SharedWallet wallet = sharedWalletManager.getWalletForUpdateOrThrow(callerId);

        wallet.deposit(amount);
        log.info("Adding balance to shared wallet, callerId={}", callerId);

        return toDto(wallet);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "wallet:shared", key = "#result.ownerId"),
            @CacheEvict(value = "wallet:shared", key = "#result.memberId")
    })
    public SharedWalletDto removeBalanceFromWallet(Long callerId, BigDecimal amount) {
        SharedWallet wallet = sharedWalletManager.getWalletForUpdateOrThrow(callerId);

        wallet.withdraw(amount);
        log.info("Withdrawing balance from shared wallet, callerId={}", callerId);

        return toDto(wallet);
    }

    @Cacheable(value = "wallet:shared", key = "#callerId")
    public SharedWalletDto getWallet(Long callerId) {
        return toDto(sharedWalletManager.getWalletOrThrow(callerId));
    }

    private SharedWalletDto toDto(SharedWallet wallet) {
        return new SharedWalletDto(
                wallet.getId(),
                wallet.getOwnerId(),
                wallet.getMemberId(),
                wallet.getBalance());
    }
}