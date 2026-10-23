package com.finovara.financeservice.wallet.reservation.service;

import com.finovara.contracts.user.authorization.additionalcode.resolver.AdditionalAuthorizationCodeResolver;
import com.finovara.contracts.user.datadeletable.UserDataDeletable;
import com.finovara.financeservice.feignclient.AuthBackendClient;
import com.finovara.financeservice.util.wallet.WalletManagerService;
import com.finovara.financeservice.wallet.model.Wallet;
import com.finovara.financeservice.wallet.repository.WalletRepository;
import com.finovara.financeservice.wallet.reservation.dto.FundReservationDto;
import com.finovara.financeservice.wallet.reservation.dto.UnreserveReservationDto;
import com.finovara.financeservice.wallet.reservation.model.FundReservation;
import com.finovara.financeservice.wallet.reservation.repository.FundReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FundReservationService implements UserDataDeletable {

    private final FundReservationCommandService commandService;
    private final FundReservationRepository fundReservationRepository;
    private final WalletManagerService walletManagerService;
    private final AuthBackendClient authBackendClient;
    private final AdditionalAuthorizationCodeResolver additionalAuthorizationCodeResolver;

    @CacheEvict(value = "wallet:user", key = "#userId")
    public Long createReservation(Long userId, FundReservationDto dto) {
        confirmAuthorization(userId, dto.authorizationCode());
        return commandService.create(userId, dto);
    }

    @CacheEvict(value = "wallet:user", key = "#userId")
    public void unreserveReservation(Long userId, Long reservationId, UnreserveReservationDto dto) {
        confirmAuthorization(userId, dto.authorizationCode());
        commandService.unreserve(userId, reservationId, dto);
    }

    @CacheEvict(value = "wallet:user", key = "#userId")
    public void manualDeleteReservation(Long userId, Long reservationId, String authorizationCode) {
        confirmAuthorization(userId, authorizationCode);
        commandService.delete(userId, reservationId);
    }

    @Override
    @Transactional
    public void deleteByUserId(Long userId) {
        Wallet wallet = walletManagerService.getWalletByUserIdOrThrow(userId);
        List<FundReservation> reservations = fundReservationRepository.findAllByWalletId(wallet.getId());
        fundReservationRepository.deleteAll(reservations);
    }

    private void confirmAuthorization(Long userId, String rawCode) {
        authBackendClient.confirmAuthorizationCode(userId, additionalAuthorizationCodeResolver.resolve(rawCode));
    }
}
