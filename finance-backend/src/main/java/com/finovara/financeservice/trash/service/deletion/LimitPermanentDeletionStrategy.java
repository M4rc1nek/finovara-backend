package com.finovara.financeservice.trash.service.deletion;

import com.finovara.financeservice.limit.model.Limit;
import com.finovara.financeservice.limit.repository.LimitRepository;
import com.finovara.financeservice.trash.model.TrashEntityType;
import com.finovara.financeservice.trash.model.TrashItem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
public class LimitPermanentDeletionStrategy implements TrashPermanentDeletionStrategy {

    private final LimitRepository limitRepository;

    @Override
    public boolean supports(TrashEntityType entityType) {
        return entityType == TrashEntityType.LIMIT;
    }

    @Override
    @Transactional
    public void delete(TrashItem item) {
        deleteAllLimits(List.of(item));
    }

    @Override
    @Transactional
    public void deleteAll(List<TrashItem> items) {
        deleteAllLimits(items);
    }

    private void deleteAllLimits(List<TrashItem> items) {
        List<Limit> limits = limitRepository.findAllByIdInAndDeletedAtIsNotNull(entityIds(items));
        if (limits.size() != items.size()) {
            throw new IllegalStateException("One or more limits for trash items no longer exist");
        }
        limitRepository.deleteAllInBatch(limits);
    }

    private List<Long> entityIds(List<TrashItem> items) {
        return items.stream().map(TrashItem::getEntityId).toList();
    }

}
