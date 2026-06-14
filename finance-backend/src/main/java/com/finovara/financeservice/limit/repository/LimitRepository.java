package com.finovara.financeservice.limit.repository;

import com.finovara.financeservice.limit.model.Limit;
import com.finovara.contracts.model.PeriodType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface LimitRepository extends JpaRepository<Limit, Long> {

    @Query("SELECT l FROM Limit l WHERE l.userId = :userId AND l.id = :limitId AND l.isActive = true")
    Optional<Limit> findByIdAndUserId(Long userId, Long limitId);

    @Query("SELECT l FROM Limit l WHERE l.userId = :userId AND l.periodType = :periodType AND l.isActive = true")
    List<Limit> findByUserIdAndType(Long userId, PeriodType periodType);

    @Query("SELECT l FROM Limit l WHERE l.userId = :userId AND l.isActive = true")
    List<Limit> findAllByUserId(Long userId);

    @Query("SELECT l.amount FROM Limit l WHERE l.userId = :userId AND l.periodType = :periodType AND l.isActive = true")
    Optional<BigDecimal> getLimitAmountByUserIdAndType(Long userId, PeriodType periodType);
}
