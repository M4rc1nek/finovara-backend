package com.finovara.corebackend.limit.repository;

import com.finovara.corebackend.limit.model.Limit;
import com.finovara.contracts.model.PeriodType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface LimitRepository extends JpaRepository<Limit, Long> {

    @Query("SELECT l FROM Limit l WHERE l.userAssigned.id = :userId AND l.id = :limitId AND l.isActive = true")
    Optional<Limit>  findByIdAndUserAssignedId(Long userId, Long limitId);

    @Query("SELECT l FROM Limit l WHERE l.userAssigned.id = :userId AND l.periodType = :periodType AND l.isActive = true")
    List<Limit> findByUserAssignedIdAndType(Long userId, PeriodType periodType);

    @Query("SELECT l FROM Limit l WHERE l.userAssigned.id = :userId AND l.isActive = true")
    List<Limit> findAllByUserAssignedId(Long userId);

    @Query("SELECT l.amount FROM Limit l WHERE l.userAssigned.id = :userId AND l.periodType = :periodType AND l.isActive = true")
    Optional<BigDecimal> getLimitAmountByUserIdAndType(Long userId, PeriodType periodType);
}