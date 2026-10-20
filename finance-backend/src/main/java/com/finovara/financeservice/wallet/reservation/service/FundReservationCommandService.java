package com.finovara.financeservice.wallet.reservation.service;

import com.finovara.contracts.exception.badrequest.InvalidInputException;
import com.finovara.contracts.exception.notfound.RequestedEntityNotFoundException;
import com.finovara.financeservice.util.wallet.WalletManagerService;
import com.finovara.financeservice.wallet.model.Wallet;
import com.finovara.financeservice.wallet.reservation.dto.FundReservationDto;
import com.finovara.financeservice.wallet.reservation.dto.UnreserveReservationDto;
import com.finovara.financeservice.wallet.reservation.model.FundReservation;
import com.finovara.financeservice.wallet.reservation.repository.FundReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class FundReservationCommandService {

    private final WalletManagerService walletManagerService;
    private final FundReservationRepository fundReservationRepository;

    @Transactional
    public Long create(Long userId, FundReservationDto dto) {
        Wallet wallet = walletManagerService.getWalletForUpdateOrThrow(userId);
        wallet.reserve(dto.amount());

        FundReservation reservation = FundReservation.builder()
                .category(dto.category())
                .amount(dto.amount())
                .walletId(wallet.getId())
                .build();

        return fundReservationRepository.save(reservation).getId();
    }

    @Transactional
    public void unreserve(Long userId, Long reservationId, UnreserveReservationDto dto) {
        Wallet wallet = walletManagerService.getWalletForUpdateOrThrow(userId);
        FundReservation reservation = getOwnedReservationOrThrow(reservationId, wallet.getId());

        if (dto.amount().compareTo(reservation.getAmount()) > 0) {
            throw new InvalidInputException("Cannot unreserve more than reserved amount");
        }

        wallet.unreserve(dto.amount());

        BigDecimal remaining = reservation.getAmount().subtract(dto.amount());
        if (remaining.signum() == 0) {
            fundReservationRepository.delete(reservation);
        } else {
            reservation.setAmount(remaining);
        }
    }

    @Transactional
    public void delete(Long userId, Long reservationId) {
        Wallet wallet = walletManagerService.getWalletForUpdateOrThrow(userId);
        FundReservation reservation = getOwnedReservationOrThrow(reservationId, wallet.getId());

        wallet.unreserve(reservation.getAmount());
        fundReservationRepository.delete(reservation);
    }

    private FundReservation getOwnedReservationOrThrow(Long reservationId, Long walletId) {
        return fundReservationRepository.findByIdAndWalletId(reservationId, walletId)
                .orElseThrow(() -> new RequestedEntityNotFoundException("Reservation not found"));
    }
}
