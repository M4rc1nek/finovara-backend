package com.finovara.financeservice.trash.service.deletion;

import com.finovara.contracts.mainaccount.activity.event.wallet.WalletStatusActivityEvent;
import com.finovara.contracts.mainaccount.activity.model.TransactionType;
import com.finovara.contracts.mainaccount.activity.model.WalletTransactionReason;
import com.finovara.contracts.outbox.OutboxService;
import com.finovara.financeservice.trash.model.TrashEntityType;
import com.finovara.financeservice.trash.model.TrashItem;
import com.finovara.financeservice.wallet.reservation.model.FundReservation;
import com.finovara.financeservice.wallet.reservation.repository.FundReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ReservationPermanentDeletionStrategy implements TrashPermanentDeletionStrategy {

    private final FundReservationRepository fundReservationRepository;
    private final OutboxService outboxService;

    @Override
    public boolean supports(TrashEntityType entityType) {
        return entityType == TrashEntityType.RESERVATION;
    }

    @Override
    @Transactional
    public void delete(TrashItem item) {
        deleteAllReservations(List.of(item));
    }

    @Override
    @Transactional
    public void deleteAll(List<TrashItem> items) {
        deleteAllReservations(items);
    }

    private void deleteAllReservations(List<TrashItem> items) {
        List<FundReservation> reservations = fundReservationRepository.findAllByIdInAndDeletedAtIsNotNull(entityIds(items));
        if (reservations.size() != items.size()) {
            throw new IllegalStateException("One or more reservations for trash items no longer exist");
        }
        Map<Long, FundReservation> reservationsById = reservations.stream().collect(Collectors.toMap(FundReservation::getId, Function.identity()));
        items.forEach(item -> {
            FundReservation reservation = reservationsById.get(item.getEntityId());
            outboxService.save("FundReservation", reservation.getId().toString(), "trash.reservation.activity",
                    new WalletStatusActivityEvent(item.getUserId(), TransactionType.RESERVATION_PERMANENTLY_DELETED,
                            WalletTransactionReason.FUND_RESERVATION, reservation.getAmount(), LocalDateTime.now()));
        });
        fundReservationRepository.deleteAllInBatch(reservations);
    }

    private List<Long> entityIds(List<TrashItem> items) {
        return items.stream().map(TrashItem::getEntityId).toList();
    }

}
