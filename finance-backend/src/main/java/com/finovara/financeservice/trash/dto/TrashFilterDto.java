package com.finovara.financeservice.trash.dto;

import com.finovara.financeservice.trash.model.TrashEntityType;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

public record TrashFilterDto(
        TrashEntityType entityType,
        Long entityId,
        @DateTimeFormat(pattern = "dd.MM.yyyy") LocalDate deletedAt,
        Boolean restoreRecurringEnabled
) {
}
