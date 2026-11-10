package com.finovara.financeservice.trash.repository;

import com.finovara.financeservice.trash.dto.TrashItemDto;
import com.finovara.financeservice.trash.model.TrashItem;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface TrashItemRepository extends JpaRepository<TrashItem, Long> {

    @Query("""
            SELECT new com.finovara.financeservice.trash.dto.TrashItemDto(
                item.id, item.entityType, item.entityId, item.deletedAt, item.restoreRecurringEnabled
            )
            FROM TrashItem item
            WHERE item.userId = :userId
            ORDER BY item.entityType ASC, item.deletedAt DESC
            """)
    List<TrashItemDto> findAllForUser(Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT item FROM TrashItem item WHERE item.userId = :userId ORDER BY item.id")
    List<TrashItem> findAllForUserForUpdate(Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT item FROM TrashItem item ORDER BY item.id")
    List<TrashItem> findAllForUpdate();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT item FROM TrashItem item WHERE item.id = :id AND item.userId = :userId")
    Optional<TrashItem> findByIdAndUserIdForUpdate(Long id, Long userId);

}
