package com.finovara.financeservice.sharedaccount.limit.repository;

import com.finovara.contracts.model.PeriodType;
import com.finovara.contracts.model.transaction.ExpenseCategory;
import com.finovara.financeservice.sharedaccount.limit.model.SharedLimit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SharedLimitRepository extends JpaRepository<SharedLimit, Long> {

    @Query("SELECT l FROM SharedLimit l WHERE (l.ownerId = :userId OR l.memberId = :userId) AND l.id = :limitId AND l.isActive = true")
    Optional<SharedLimit> findByIdAndUserId(Long userId, Long limitId);

    @Query("SELECT l FROM SharedLimit l WHERE (l.ownerId = :userId OR l.memberId = :userId) AND l.isActive = true")
    List<SharedLimit> findAllByUserId(Long userId);

    @Query("SELECT l FROM SharedLimit l WHERE (l.ownerId = :userId OR l.memberId = :userId) AND l.periodType = :periodType AND l.category IS NULL AND l.isActive = true")
    Optional<SharedLimit> findGeneralLimit(Long userId, PeriodType periodType);

    @Query("SELECT l FROM SharedLimit l WHERE (l.ownerId = :userId OR l.memberId = :userId) AND l.periodType = :periodType AND l.category = :category AND l.isActive = true")
    Optional<SharedLimit> findCategoryLimit(Long userId, PeriodType periodType, ExpenseCategory category);

    @Modifying
    @Query("DELETE FROM SharedLimit sh WHERE sh.ownerId = :ownerId AND sh.memberId = :memberId")
    void deleteByOwnerIdAndMemberId(Long ownerId, Long memberId);
}