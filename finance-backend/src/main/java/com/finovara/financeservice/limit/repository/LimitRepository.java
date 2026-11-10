package com.finovara.financeservice.limit.repository;

import com.finovara.contracts.util.PeriodType;
import com.finovara.contracts.util.model.ExpenseCategory;
import com.finovara.financeservice.limit.model.Limit;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LimitRepository extends JpaRepository<Limit, Long> {

    @Query("SELECT l FROM Limit l WHERE l.userId = :userId AND l.id = :limitId AND l.deletedAt IS NULL AND l.isActive = true")
    Optional<Limit> findByIdAndUserId(Long userId, Long limitId);

    @Query("SELECT l FROM Limit l WHERE l.userId = :userId AND l.deletedAt IS NULL AND l.isActive = true ORDER BY l.id DESC")
    List<Limit> findAllActiveByUserId(Long userId);

    @Query("SELECT l FROM Limit l WHERE l.userId IN :userIds AND l.deletedAt IS NULL AND l.isActive = true ORDER BY l.userId, l.id DESC")
    List<Limit> findAllActiveByUserIdIn(List<Long> userIds);

    @Query("SELECT l FROM Limit l WHERE l.userId = :userId AND l.deletedAt IS NULL AND l.periodType = :periodType AND l.category IS NULL AND l.isActive = true")
    Optional<Limit> findGeneralLimit(Long userId, PeriodType periodType);

    @Query("SELECT l FROM Limit l WHERE l.userId = :userId AND l.deletedAt IS NULL AND l.periodType = :periodType AND l.category = :category AND l.isActive = true")
    Optional<Limit> findCategoryLimit(Long userId, PeriodType periodType, ExpenseCategory category);

    List<Limit> findAllByUserId(Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT l FROM Limit l WHERE l.id = :id AND l.userId = :userId AND l.deletedAt IS NOT NULL")
    Optional<Limit> findTrashedByIdAndUserIdForUpdate(Long id, Long userId);

    @Query("SELECT lim FROM Limit lim WHERE lim.id IN :ids AND lim.deletedAt IS NOT NULL")
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<Limit> findAllByIdInAndDeletedAtIsNotNull(List<Long> ids);
}
