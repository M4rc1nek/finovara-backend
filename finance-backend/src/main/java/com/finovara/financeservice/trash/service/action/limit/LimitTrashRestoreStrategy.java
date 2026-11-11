package com.finovara.financeservice.trash.service.action.limit;

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
        Limit limit = getLimit(item);
        validateNoDuplicate(item, limit);
        restoreLimit(limit);
        publishLimitRestoredEvent(item, limit);
    }

    private Limit getLimit(TrashItem item) {
        return limitRepository.findTrashedByIdAndUserIdForUpdate(item.getEntityId(), item.getUserId())
                .orElseThrow(() -> new RequestedEntityNotFoundException("Limit not found in trash"));
    }

    private void validateNoDuplicate(TrashItem item, Limit limit) {
        boolean duplicate = limit.getCategory() == null
                ? limitRepository.findGeneralLimit(item.getUserId(), limit.getPeriodType()).isPresent()
                : limitRepository.findCategoryLimit(item.getUserId(), limit.getPeriodType(), limit.getCategory()).isPresent();
        if (!duplicate) {
            return;
        }
        throw new EntityAlreadyExistsException(limit.getCategory() == null ? "General limit already exists" : "Category limit already exists");
    }

    private void restoreLimit(Limit limit) {
        limit.setDeletedAt(null);
        limitRepository.save(limit);
    }

    private void publishLimitRestoredEvent(TrashItem item, Limit limit) {
        outboxService.save("Limit", limit.getId().toString(), "trash.limit.activity",
                new LimitActivityEvent(item.getUserId(), LimitActivityType.RESTORED_FROM_TRASH, limit.getPeriodType().name(),
                        limit.getAmount(), null, LocalDateTime.now()));
    }
}
