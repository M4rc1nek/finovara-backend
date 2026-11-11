package com.finovara.financeservice.trash.service.deletion;

import com.finovara.financeservice.trash.model.TrashEntityType;
import com.finovara.financeservice.trash.model.TrashItem;
import com.finovara.financeservice.wallet.reservation.model.FundReservation;
import com.finovara.financeservice.wallet.reservation.repository.FundReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ReservationPermanentDeletionStrategy implements TrashPermanentDeletionStrategy {

    private final FundReservationRepository fundReservationRepository;

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
        fundReservationRepository.deleteAllInBatch(reservations);
    }

    private List<Long> entityIds(List<TrashItem> items) {
        return items.stream().map(TrashItem::getEntityId).toList();
    }

}
