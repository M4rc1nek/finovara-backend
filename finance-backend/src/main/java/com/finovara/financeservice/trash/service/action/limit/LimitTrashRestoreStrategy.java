package com.finovara.financeservice.trash.service.action;

import com.finovara.contracts.exception.conflict.EntityAlreadyExistsException;
import com.finovara.contracts.exception.notfound.RequestedEntityNotFoundException;
import com.finovara.contracts.mainaccount.activity.event.limit.LimitActivityEvent;
import com.finovara.contracts.mainaccount.activity.model.LimitActivityType;
import com.finovara.contracts.outbox.OutboxService;
import com.finovara.financeservice.limit.model.Limit;
import com.finovara.financeservice.limit.repository.LimitRepository;
import com.finovara.financeservice.trash.model.TrashEntityType;
import com.finovara.financeservice.trash.model.TrashItem;
import com.finovara.financeservice.trash.service.TrashRestoreStrategy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class LimitTrashRestoreStrategy implements TrashRestoreStrategy {

    private final LimitRepository limitRepository;
    private final OutboxService outboxService;

    @Override
    public boolean supports(TrashEntityType entityType) {
        return entityType == TrashEntityType.LIMIT;
    }

    @Override
    public void restore(TrashItem item) {
        Limit limit = limitRepository.findTrashedByIdAndUserIdForUpdate(item.getEntityId(), item.getUserId())
                .orElseThrow(() -> new RequestedEntityNotFoundException("Limit not found in trash"));
        boolean duplicate = limit.getCategory() == null
                ? limitRepository.findGeneralLimit(item.getUserId(), limit.getPeriodType()).isPresent()
                : limitRepository.findCategoryLimit(item.getUserId(), limit.getPeriodType(), limit.getCategory()).isPresent();
        if (duplicate) {
            throw new EntityAlreadyExistsException(limit.getCategory() == null
                    ? "General limit already exists" : "Category limit already exists");
        }
        limit.setDeletedAt(null);
        limitRepository.save(limit);
        outboxService.save("Limit", limit.getId().toString(), "limit.changed",
                new LimitActivityEvent(item.getUserId(), LimitActivityType.ADDED_LIMIT, limit.getPeriodType().name(),
                        limit.getAmount(), null, LocalDateTime.now()));
    }
}
