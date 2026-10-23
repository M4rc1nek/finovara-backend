package com.finovara.financeservice.sharedaccount.limit.repository;

import com.finovara.contracts.util.PeriodType;
import com.finovara.contracts.util.model.ExpenseCategory;
import com.finovara.financeservice.sharedaccount.limit.model.SharedLimit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SharedLimitRepository extends JpaRepository<SharedLimit, Long> {

    @Query("SELECT l FROM SharedLimit l WHERE (l.ownerId = :userId OR l.memberId = :userId) AND l.id = :limitId AND l.isActive = true")
    Optional<SharedLimit> findByIdAndOwnerIdOrMemberId(Long userId, Long limitId);

    @Query("SELECT e FROM SharedLimit e WHERE e.ownerId = :ownerId AND e.memberId = :memberId")
    List<SharedLimit> findAllByOwnerIdAndMemberId(Long ownerId, Long memberId);

    @Query("SELECT l FROM SharedLimit l WHERE (l.ownerId = :userId OR l.memberId = :userId) AND l.isActive = true ORDER BY l.id DESC")
    List<SharedLimit> findAllByUserId(Long userId);

    @Query("SELECT l FROM SharedLimit l WHERE (l.ownerId = :userId OR l.memberId = :userId) AND l.periodType = :periodType AND l.category IS NULL AND l.isActive = true")
    Optional<SharedLimit> findGeneralLimit(Long userId, PeriodType periodType);

    @Query("SELECT l FROM SharedLimit l WHERE (l.ownerId = :userId OR l.memberId = :userId) AND l.periodType = :periodType AND l.category = :category AND l.isActive = true")
    Optional<SharedLimit> findCategoryLimit(Long userId, PeriodType periodType, ExpenseCategory category);
}