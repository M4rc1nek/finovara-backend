package com.finovara.financeservice.trash.dto;

import com.finovara.financeservice.trash.model.TrashEntityType;

import java.time.LocalDateTime;

public record TrashItemDto(
        Long id,
        TrashEntityType entityType,
        Long entityId,
        LocalDateTime deletedAt,
        Boolean restoreRecurringEnabled
) {
}
