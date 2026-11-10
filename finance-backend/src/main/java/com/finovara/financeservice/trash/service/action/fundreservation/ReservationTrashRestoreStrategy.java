package com.finovara.financeservice.trash.service.action.fundreservation;

import com.finovara.contracts.exception.notfound.RequestedEntityNotFoundException;
import com.finovara.contracts.mainaccount.activity.model.TransactionType;
import com.finovara.contracts.mainaccount.activity.model.WalletTransactionReason;
import com.finovara.financeservice.trash.model.TrashEntityType;
import com.finovara.financeservice.trash.model.TrashItem;
import com.finovara.financeservice.trash.service.TrashRestoreStrategy;
import com.finovara.financeservice.util.wallet.WalletManagerService;
import com.finovara.financeservice.wallet.model.Wallet;
import com.finovara.financeservice.wallet.reservation.model.FundReservation;
import com.finovara.financeservice.wallet.reservation.repository.FundReservationRepository;
import com.finovara.financeservice.wallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReservationTrashRestoreStrategy implements TrashRestoreStrategy {

    private final FundReservationRepository fundReservationRepository;
    private final WalletManagerService walletManagerService;
    private final WalletService walletService;

    @Override
    public boolean supports(TrashEntityType entityType) {
        return entityType == TrashEntityType.RESERVATION;
    }

    @Override
    public void restore(TrashItem item) {
        Wallet wallet = walletManagerService.getWalletForUpdateOrThrow(item.getUserId());
        FundReservation reservation = getReservation(item, wallet);
        restoreReservation(wallet, reservation);
        publishRestoredStatus(item, wallet, reservation);
    }

    private FundReservation getReservation(TrashItem item, Wallet wallet) {
        return fundReservationRepository.findTrashedByIdAndWalletIdForUpdate(item.getEntityId(), wallet.getId())
                .orElseThrow(() -> new RequestedEntityNotFoundException("Reservation not found in trash"));
    }

    private void restoreReservation(Wallet wallet, FundReservation reservation) {
        wallet.reserve(reservation.getAmount());
        reservation.setDeletedAt(null);
        fundReservationRepository.save(reservation);
    }

    private void publishRestoredStatus(TrashItem item, Wallet wallet, FundReservation reservation) {
        walletService.publishStatusChange(item.getUserId(), wallet, TransactionType.RESERVATION,
                WalletTransactionReason.FUND_RESERVATION, reservation.getAmount());
    }
}
