package com.finovara.financeservice.trash.service;

import com.finovara.financeservice.trash.model.TrashEntityType;
import com.finovara.financeservice.trash.model.TrashItem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TrashRestoreService {

    private final List<TrashRestoreStrategy> strategies;

    @Transactional
    public void restore(TrashItem item) {
        TrashEntityType entityType = item.getEntityType();
        TrashRestoreStrategy strategy = strategies.stream()
                .filter(candidate -> candidate.supports(entityType))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No restore strategy for " + entityType));
        strategy.restore(item);
    }
}
