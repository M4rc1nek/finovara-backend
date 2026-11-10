package com.finovara.financeservice.trash.service.action.limit;

import com.finovara.contracts.mainaccount.activity.event.limit.LimitActivityEvent;
import com.finovara.contracts.mainaccount.activity.model.LimitActivityType;
import com.finovara.contracts.outbox.OutboxService;
import com.finovara.financeservice.limit.model.Limit;
import com.finovara.financeservice.limit.repository.LimitRepository;
import com.finovara.financeservice.trash.model.TrashEntityType;
import com.finovara.financeservice.trash.service.TrashService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Transactional
public class LimitTrashMover {

    private final LimitRepository limitRepository;
    private final OutboxService outboxService;
    private final TrashService trashService;

    public void move(Limit limit, Long userId) {
        LocalDateTime deletedAt = LocalDateTime.now();
        limit.setDeletedAt(deletedAt);
        limitRepository.save(limit);
        outboxService.save("Limit", limit.getId().toString(), "limit.changed",
                new LimitActivityEvent(userId, LimitActivityType.DELETED_LIMIT, limit.getPeriodType().name(),
                        limit.getAmount(), null, deletedAt));
        trashService.add(TrashEntityType.LIMIT, limit.getId(), userId, deletedAt, null, false);
    }
}
