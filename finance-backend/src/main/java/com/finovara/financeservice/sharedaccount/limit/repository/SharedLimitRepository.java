package com.finovara.financeservice.sharedaccount.limit.repository;

import com.finovara.contracts.model.PeriodType;
import com.finovara.contracts.model.transaction.ExpenseCategory;
import com.finovara.financeservice.sharedaccount.limit.model.SharedLimit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LimitRepository extends JpaRepository<SharedLimit, Long> {

    @Query("SELECT l FROM Limit l WHERE l.userId = :userId AND l.id = :limitId AND l.isActive = true")
    Optional<SharedLimit> findByIdAndUserId(Long userId, Long limitId);

    @Query("SELECT l FROM Limit l WHERE l.userId = :userId AND l.isActive = true")
    List<SharedLimit> findAllByUserId(Long userId);

    @Query("SELECT l FROM Limit l WHERE l.userId = :userId AND l.periodType = :periodType AND l.category IS NULL AND l.isActive = true")
    Optional<SharedLimit> findGeneralLimit(Long userId, PeriodType periodType);

    @Query("SELECT l FROM Limit l WHERE l.userId = :userId AND l.periodType = :periodType AND l.category = :category AND l.isActive = true")
    Optional<SharedLimit> findCategoryLimit(Long userId, PeriodType periodType, ExpenseCategory category);

    void deleteByUserId(Long userId);
}
