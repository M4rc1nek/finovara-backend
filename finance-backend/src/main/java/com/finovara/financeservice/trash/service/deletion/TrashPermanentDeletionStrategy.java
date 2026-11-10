package com.finovara.financeservice.trash.service.deletion;

import com.finovara.financeservice.trash.model.TrashEntityType;
import com.finovara.financeservice.trash.model.TrashItem;
import java.util.List;

public interface TrashPermanentDeletionStrategy {

    boolean supports(TrashEntityType entityType);

    void delete(TrashItem item);

    void deleteAll(List<TrashItem> items);
}
