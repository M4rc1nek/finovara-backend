package com.finovara.financeservice.wallet.service;

import com.finovara.contracts.datadeletable.UserDataDeletable;
import com.finovara.contracts.notification.event.wallet.WalletBalanceChangedEvent;
import com.finovara.financeservice.util.wallet.WalletManagerService;
import com.finovara.financeservice.wallet.dto.WalletDto;
import com.finovara.financeservice.wallet.dto.WalletResponse;
import com.finovara.financeservice.wallet.model.Wallet;
import com.finovara.financeservice.wallet.repository.WalletRepository;
import com.finovara.financeservice.wallet.reservation.dto.FundReservationDto;
import com.finovara.financeservice.wallet.reservation.repository.FundReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class WalletService implements UserDataDeletable {

    private final WalletRepository walletRepository;
    private final WalletManagerService walletManagerService;
    private final FundReservationRepository fundReservationRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Transactional
    @CacheEvict(value = "wallet:user", key = "#userId")
    public WalletDto addBalanceToWallet(Long userId, BigDecimal amount) {
        Wallet wallet = walletManagerService.getWalletByUserIdOrThrow(userId);

        wallet.deposit(amount);
        log.info("Adding balance to wallet for userId: {}", userId);

        return returnNewWalletDto(userId, wallet);
    }

    @Transactional
    @CacheEvict(value = "wallet:user", key = "#userId")
    public WalletDto removeBalanceFromWallet(Long userId, BigDecimal amount) {
        Wallet wallet = walletManagerService.getWalletByUserIdOrThrow(userId);

        BigDecimal previousBalance = wallet.getBalance();
        wallet.withdraw(amount);
        log.info("Withdrawing balance from wallet for userId: {}", userId);

        kafkaTemplate.send("wallet.balance-changed",
                new WalletBalanceChangedEvent(userId, previousBalance, wallet.getBalance(), LocalDateTime.now()));

        return returnNewWalletDto(userId, wallet);
    }

    @Transactional
    @Cacheable(value = "wallet:user", key = "#userId")
    public WalletResponse getWalletWithReservations(Long userId) {
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseGet(() -> walletRepository.save(Wallet.create(userId)));

        BigDecimal available = wallet.getBalance().subtract(wallet.getReservedAmount());

        List<FundReservationDto> reservations  = fundReservationRepository.findByWalletId(wallet.getId())
                .stream()
                .map(fundReservation -> new FundReservationDto(fundReservation.getId(), fundReservation.getCategory(), fundReservation.getAmount())).toList();

        return new WalletResponse(wallet.getId(), userId, wallet.getBalance(), wallet.getReservedAmount(), available, reservations);
    }

    @Override
    @Transactional
    @CacheEvict(value = "wallet:user", key = "#userId")
    public void deleteByUserId(Long userId) {
        walletRepository.deleteByUserId(userId);
        log.info("Deleted wallet for userId={}", userId);
    }

    private WalletDto returnNewWalletDto(Long userId, Wallet wallet) {
        BigDecimal available = wallet.getBalance().subtract(wallet.getReservedAmount());
        return new WalletDto(wallet.getId(), userId, wallet.getBalance(), wallet.getReservedAmount(), available);
    }
}