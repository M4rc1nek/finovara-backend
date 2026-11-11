package com.finovara.financeservice.trash.service;

import com.finovara.financeservice.trash.model.TrashEntityType;
import com.finovara.financeservice.trash.model.TrashItem;
import com.finovara.financeservice.trash.service.deletion.TrashPermanentDeletionStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class TrashPermanentDeletionService {

    private static final int BATCH_SIZE = 500;

    private final List<TrashPermanentDeletionStrategy> strategies;

    @Transactional
    public void delete(TrashItem item) {
        findStrategy(item.getEntityType()).delete(item);
        log.info("Deleted trash item {} permanently", item.getId());
    }

    @Transactional
    public void deleteAll(List<TrashItem> items) {
        Map<TrashEntityType, List<TrashItem>> itemsByType = groupByType(items);
        for (Map.Entry<TrashEntityType, List<TrashItem>> entry : itemsByType.entrySet()) {
            deleteAllInBatches(findStrategy(entry.getKey()), entry.getValue());
        }
        log.info("Deleted {} trash items permanently", items.size());
    }

    private Map<TrashEntityType, List<TrashItem>> groupByType(List<TrashItem> items) {
        Map<TrashEntityType, List<TrashItem>> itemsByType = new EnumMap<>(TrashEntityType.class);
        for (TrashItem item : items) {
            itemsByType.computeIfAbsent(item.getEntityType(), type -> new ArrayList<>()).add(item);
        }
        return itemsByType;
    }

    private void deleteAllInBatches(TrashPermanentDeletionStrategy strategy, List<TrashItem> items) {
        for (int start = 0; start < items.size(); start += BATCH_SIZE) {
            int end = Math.min(start + BATCH_SIZE, items.size());
            strategy.deleteAll(items.subList(start, end));
        }
    }

    private TrashPermanentDeletionStrategy findStrategy(TrashEntityType entityType) {
        return strategies.stream()
                .filter(strategy -> strategy.supports(entityType))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No permanent deletion strategy for " + entityType));
    }
}
