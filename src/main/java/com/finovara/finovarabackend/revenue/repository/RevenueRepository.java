package com.finovara.finovarabackend.revenue.repository;

import com.finovara.finovarabackend.revenue.model.Revenue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface RevenueRepository extends JpaRepository<Revenue, Long> {

    Optional<Revenue> findByIdAndUserAssignedId(Long revenueId, Long userId);

    List<Revenue> findAllByUserAssignedId(Long userId);

    List<Revenue> findAllByUserAssignedIdAndCreatedAtBetween(Long userId, LocalDate from, LocalDate to);

    // coalesce zwroci mi przychody lub 0 jest przychody sa null
    @Query("SELECT coalesce(sum(r.amount),0) from Revenue r WHERE r.userAssigned.id = :userId AND r.createdAt = :date")
    BigDecimal sumRevenueForDay(@Param("userId") Long userId, @Param("date") LocalDate date);
}
