package com.finovara.financeservice.wallet.reservation.service;

import com.finovara.contracts.authorization.additionalcode.resolver.AdditionalAuthorizationCodeResolver;
import com.finovara.contracts.exception.badrequest.InvalidInputException;
import com.finovara.contracts.exception.notfound.RequestedEntityNotFoundException;
import com.finovara.financeservice.feignclient.AuthBackendClient;
import com.finovara.financeservice.util.wallet.WalletManagerService;
import com.finovara.financeservice.wallet.model.Wallet;
import com.finovara.financeservice.wallet.reservation.dto.FundReservationDto;
import com.finovara.financeservice.wallet.reservation.dto.UnreserveReservationDto;
import com.finovara.financeservice.wallet.reservation.model.FundReservation;
import com.finovara.financeservice.wallet.reservation.repository.FundReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class FundReservationService {

    private final WalletManagerService walletManagerService;
    private final FundReservationRepository fundReservationRepository;
    private final AuthBackendClient authBackendClient;
    private final AdditionalAuthorizationCodeResolver additionalAuthorizationCodeResolver;

    @Transactional
    @CacheEvict(value = "wallet:user", key = "#userId")
    public Long createReservation(Long userId, FundReservationDto dto) {
        Wallet wallet = walletManagerService.getWalletByUserIdOrThrow(userId);

        authBackendClient.confirmAuthorizationCode(userId, additionalAuthorizationCodeResolver.resolve(dto.authorizationCode()));
        wallet.reserve(dto.amount());

        FundReservation reservation = FundReservation.builder()
                .category(dto.category())
                .amount(dto.amount())
                .walletId(wallet.getId())
                .build();

        FundReservation savedReservation = fundReservationRepository.save(reservation);

        return savedReservation.getId();
    }

    @Transactional
    @CacheEvict(value = "wallet:user", key = "#userId")
    public void unreserveReservation(Long userId, Long reservationId, UnreserveReservationDto dto) {
        Wallet wallet = walletManagerService.getWalletByUserIdOrThrow(userId);
        FundReservation reservation = getOwnedReservationOrThrow(reservationId, wallet.getId());

        authBackendClient.confirmAuthorizationCode(userId, additionalAuthorizationCodeResolver.resolve(dto.authorizationCode()));
        if (dto.amount().compareTo(reservation.getAmount()) > 0) {
            throw new InvalidInputException("Cannot unreserve more than reserved amount");
        }

        wallet.unreserve(dto.amount());

        BigDecimal remaining = reservation.getAmount().subtract(dto.amount());
        reservation.setAmount(remaining);

        fundReservationRepository.save(reservation);
    }

    @Transactional
    @CacheEvict(value = "wallet:user", key = "#userId")
    public void cancelReservation(Long userId, Long reservationId, String authorizationCode) {
        Wallet wallet = walletManagerService.getWalletByUserIdOrThrow(userId);
        FundReservation reservation = getOwnedReservationOrThrow(reservationId, wallet.getId());

        authBackendClient.confirmAuthorizationCode(userId, additionalAuthorizationCodeResolver.resolve(authorizationCode));
        wallet.unreserve(reservation.getAmount());

        fundReservationRepository.delete(reservation);
    }

    private FundReservation getOwnedReservationOrThrow(Long reservationId, Long walletId) {
        FundReservation reservation = fundReservationRepository.findById(reservationId)
                .orElseThrow(() -> new RequestedEntityNotFoundException("Reservation not found"));

        if (!reservation.getWalletId().equals(walletId)) {
            throw new RequestedEntityNotFoundException("Reservation not found");
        }

        return reservation;
    }
}