package com.finovara.financeservice.trash.service.deletion;

import com.finovara.contracts.mainaccount.activity.event.revenue.RevenueActivityEvent;
import com.finovara.contracts.mainaccount.activity.model.RevenueActivityType;
import com.finovara.contracts.outbox.OutboxService;
import com.finovara.financeservice.revenue.model.Revenue;
import com.finovara.financeservice.revenue.repository.RevenueRepository;
import com.finovara.financeservice.trash.model.TrashEntityType;
import com.finovara.financeservice.trash.model.TrashItem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class RevenuePermanentDeletionStrategy implements TrashPermanentDeletionStrategy {

    private final RevenueRepository revenueRepository;
    private final OutboxService outboxService;

    @Override
    public boolean supports(TrashEntityType entityType) {
        return entityType == TrashEntityType.REVENUE;
    }

    @Override
    @Transactional
    public void delete(TrashItem item) {
        deleteAllRevenues(List.of(item));
    }

    @Override
    @Transactional
    public void deleteAll(List<TrashItem> items) {
        deleteAllRevenues(items);
    }

    private void deleteAllRevenues(List<TrashItem> items) {
        List<Revenue> revenues = revenueRepository.findAllByIdInAndDeletedAtIsNotNull(entityIds(items));
        if (revenues.size() != items.size()) {
            throw new IllegalStateException("One or more revenues for trash items no longer exist");
        }
        revenues.forEach(revenue -> outboxService.save("Revenue", revenue.getId().toString(),
                "trash.revenue.activity", new RevenueActivityEvent(revenue.getUserId(),
                        RevenueActivityType.PERMANENTLY_DELETED, revenue.getAmount(), revenue.getCategory(),
                        null, null, LocalDateTime.now())));
        revenueRepository.deleteAllInBatch(revenues);
    }

    private List<Long> entityIds(List<TrashItem> items) {
        return items.stream().map(TrashItem::getEntityId).toList();
    }

}
