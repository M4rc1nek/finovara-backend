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
@Transactional
public class LimitPermanentDeletionStrategy implements TrashPermanentDeletionStrategy {

    private final LimitRepository limitRepository;

    @Override
    public boolean supports(TrashEntityType entityType) {
        return entityType == TrashEntityType.LIMIT;
    }

    @Override
    public void delete(TrashItem item) {
        deleteAll(List.of(item));
    }

    @Override
    public void deleteAll(List<TrashItem> items) {
        List<Limit> limits = limitRepository.findAllByIdInAndDeletedAtIsNotNull(entityIds(items));
        if (limits.size() != items.size()) {
            throw new IllegalStateException("One or more limits for trash items no longer exist");
        }
        limitRepository.deleteAll(limits);
    }

    private List<Long> entityIds(List<TrashItem> items) {
        return items.stream().map(TrashItem::getEntityId).toList();
    }

}
