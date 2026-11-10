package com.finovara.financeservice.trash.service;

import com.finovara.financeservice.trash.model.TrashEntityType;
import com.finovara.financeservice.trash.model.TrashItem;

public interface TrashRestoreStrategy {

    boolean supports(TrashEntityType entityType);

    void restore(TrashItem item);
}
