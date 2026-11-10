package com.finovara.financeservice.wallet.reservation.service;

import com.finovara.contracts.mainaccount.activity.model.TransactionType;
import com.finovara.contracts.mainaccount.activity.model.WalletTransactionReason;
import com.finovara.contracts.exception.badrequest.InvalidInputException;
import com.finovara.contracts.exception.notfound.RequestedEntityNotFoundException;
import com.finovara.financeservice.util.wallet.WalletManagerService;
import com.finovara.financeservice.wallet.model.Wallet;
import com.finovara.financeservice.wallet.reservation.dto.FundReservationDto;
import com.finovara.financeservice.wallet.reservation.dto.UnreserveReservationDto;
import com.finovara.financeservice.wallet.reservation.model.FundReservation;
import com.finovara.financeservice.wallet.reservation.repository.FundReservationRepository;
import com.finovara.financeservice.trash.service.TrashMoverService;
import com.finovara.financeservice.wallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class FundReservationCommandService {

    private final WalletManagerService walletManagerService;
    private final FundReservationRepository fundReservationRepository;
    private final WalletService walletService;
    private final TrashMoverService trashMoverService;

    @Transactional
    public Long create(Long userId, FundReservationDto dto) {
        Wallet wallet = walletManagerService.getWalletForUpdateOrThrow(userId);
        wallet.reserve(dto.amount());
        walletService.publishStatusChange(userId, wallet, TransactionType.RESERVATION,
                WalletTransactionReason.FUND_RESERVATION, dto.amount());

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

        BigDecimal remaining = reservation.getAmount().subtract(dto.amount());
        if (remaining.signum() == 0) {
            trashMoverService.moveFundReservationToTrash(reservation, wallet, userId);
        } else {
            wallet.unreserve(dto.amount());
            walletService.publishStatusChange(userId, wallet, TransactionType.RESERVATION_RELEASE,
                    WalletTransactionReason.FUND_RESERVATION, dto.amount());
            reservation.setAmount(remaining);
            fundReservationRepository.save(reservation);
        }
    }

    @Transactional
    public void delete(Long userId, Long reservationId) {
        Wallet wallet = walletManagerService.getWalletForUpdateOrThrow(userId);
        FundReservation reservation = getOwnedReservationOrThrow(reservationId, wallet.getId());

        trashMoverService.moveFundReservationToTrash(reservation, wallet, userId);
    }

    private FundReservation getOwnedReservationOrThrow(Long reservationId, Long walletId) {
        return fundReservationRepository.findByIdAndWalletId(reservationId, walletId)
                .orElseThrow(() -> new RequestedEntityNotFoundException("Reservation not found"));
    }
}
