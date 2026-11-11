package com.finovara.financeservice.trash.service.action.fundreservation;

import com.finovara.contracts.mainaccount.activity.model.TransactionType;
import com.finovara.contracts.mainaccount.activity.model.WalletTransactionReason;
import com.finovara.financeservice.trash.model.TrashEntityType;
import com.finovara.financeservice.trash.service.TrashService;
import com.finovara.financeservice.wallet.model.Wallet;
import com.finovara.financeservice.wallet.reservation.model.FundReservation;
import com.finovara.financeservice.wallet.reservation.repository.FundReservationRepository;
import com.finovara.financeservice.wallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class FundReservationTrashMover {

    private final FundReservationRepository fundReservationRepository;
    private final WalletService walletService;
    private final TrashService trashService;

    @Transactional
    public void move(FundReservation reservation, Wallet wallet, Long userId) {
        wallet.unreserve(reservation.getAmount());
        walletService.publishStatusChange(userId, wallet, TransactionType.RESERVATION_RELEASE,
                WalletTransactionReason.FUND_RESERVATION, reservation.getAmount());

        LocalDateTime deletedAt = LocalDateTime.now();
        reservation.setDeletedAt(deletedAt);
        fundReservationRepository.save(reservation);
        trashService.add(TrashEntityType.RESERVATION, reservation.getId(), userId, deletedAt, null, false);
    }
}
