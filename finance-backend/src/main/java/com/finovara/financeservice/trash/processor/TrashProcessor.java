package com.finovara.financeservice.trash.processor;

import com.finovara.financeservice.trash.model.TrashItem;
import com.finovara.financeservice.trash.repository.TrashItemRepository;
import com.finovara.financeservice.trash.service.TrashPermanentDeletionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TrashProcessor {

    private final TrashItemRepository trashItemRepository;
    private final TrashPermanentDeletionService permanentDeletionService;

    @Transactional
    public void deleteAllTrashes() {
        List<TrashItem> items = trashItemRepository.findAllForUpdate();

        if (items.isEmpty()) {
            return;
        }

        permanentDeletionService.deleteAll(items);
        trashItemRepository.deleteAllInBatch(items);
    }
}
