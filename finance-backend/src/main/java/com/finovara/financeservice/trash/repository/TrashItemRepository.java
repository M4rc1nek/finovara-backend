package com.finovara.financeservice.trash.repository;

import com.finovara.financeservice.trash.dto.TrashFilterDto;
import com.finovara.financeservice.trash.dto.TrashItemDto;
import com.finovara.financeservice.trash.model.TrashItem;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TrashItemRepository extends JpaRepository<TrashItem, Long> {
    @Query("""
            SELECT new com.finovara.financeservice.trash.dto.TrashItemDto(
                i.id, i.entityType, i.entityId, i.deletedAt, i.restoreRecurringEnabled
            )
            FROM TrashItem i
            WHERE i.userId = :userId
            ORDER BY i.entityType ASC, i.deletedAt DESC, i.id DESC
            """)
    List<TrashItemDto> findAllForUser(Long userId, Pageable pageable);


    @Query("""
        SELECT new com.finovara.financeservice.trash.dto.TrashItemDto(
            i.id, i.entityType, i.entityId, i.deletedAt, i.restoreRecurringEnabled
        )
        FROM TrashItem i
        WHERE i.userId = :userId
          AND (CAST(:#{#filter.entityType} AS string) IS NULL OR i.entityType = :#{#filter.entityType})
          AND (CAST(:#{#filter.entityId} AS string) IS NULL OR i.entityId = :#{#filter.entityId})
          AND (CAST(:deletedAt AS date) IS NULL OR CAST(i.deletedAt AS date) = :deletedAt)
          AND (CAST(:#{#filter.restoreRecurringEnabled} AS string) IS NULL
               OR (:#{#filter.restoreRecurringEnabled} = true AND i.restoreRecurringEnabled = true)
               OR (:#{#filter.restoreRecurringEnabled} = false
                   AND (i.restoreRecurringEnabled = false OR i.restoreRecurringEnabled IS NULL)))
        ORDER BY i.entityType ASC, i.deletedAt DESC, i.id DESC
        """)
    List<TrashItemDto> findFilteredForUser(Long userId, TrashFilterDto filter, LocalDate deletedAt, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM TrashItem i WHERE i.userId = :userId ORDER BY i.id")
    List<TrashItem> findAllForUserForUpdate(Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM TrashItem i ORDER BY i.id")
    List<TrashItem> findAllForUpdate();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM TrashItem i WHERE i.id = :id AND i.userId = :userId")
    Optional<TrashItem> findByIdAndUserIdForUpdate(Long id, Long userId);

}
