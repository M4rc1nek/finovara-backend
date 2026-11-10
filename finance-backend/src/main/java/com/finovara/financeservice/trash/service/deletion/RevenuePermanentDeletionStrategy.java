package com.finovara.financeservice.trash.service.deletion;

import com.finovara.financeservice.revenue.model.Revenue;
import com.finovara.financeservice.revenue.repository.RevenueRepository;
import com.finovara.financeservice.trash.model.TrashEntityType;
import com.finovara.financeservice.trash.model.TrashItem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
@Transactional
public class RevenuePermanentDeletionStrategy implements TrashPermanentDeletionStrategy {

    private final RevenueRepository revenueRepository;

    @Override
    public boolean supports(TrashEntityType entityType) {
        return entityType == TrashEntityType.REVENUE;
    }

    @Override
    public void delete(TrashItem item) {
        deleteAll(List.of(item));
    }

    @Override
    public void deleteAll(List<TrashItem> items) {
        List<Revenue> revenues = revenueRepository.findAllByIdInAndDeletedAtIsNotNull(entityIds(items));
        if (revenues.size() != items.size()) {
            throw new IllegalStateException("One or more revenues for trash items no longer exist");
        }
        revenueRepository.deleteAll(revenues);
    }

    private List<Long> entityIds(List<TrashItem> items) {
        return items.stream().map(TrashItem::getEntityId).toList();
    }

}
