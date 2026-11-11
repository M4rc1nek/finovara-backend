package com.finovara.financeservice.trash.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "trash_items",
        uniqueConstraints = @UniqueConstraint(name = "uk_trash_entity", columnNames = {"entity_type", "entity_id"}),
        indexes = {
                @Index(name = "idx_trash_user_entity_deleted", columnList = "user_id, entity_type, deleted_at DESC"),
                @Index(name = "idx_trash_user_id", columnList = "user_id, id")
        })
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrashItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private TrashEntityType entityType;

    @Column(nullable = false)
    private Long entityId;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private LocalDateTime deletedAt;

    private Boolean restoreRecurringEnabled;

    private Boolean financialEffectsReverted;
}
