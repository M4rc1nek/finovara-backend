package com.finovara.finovarabackend.revenue.repository;

import com.finovara.finovarabackend.report.finances.chart.dto.DateAmountDto;
import com.finovara.finovarabackend.report.finances.highestrevenue.dto.HighestRevenueDto;
import com.finovara.finovarabackend.revenue.model.Revenue;
import com.finovara.finovarabackend.revenue.model.RevenueCategory;
import org.springframework.data.domain.Pageable;
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

    @Query("SELECT r From Revenue r WHERE r.userAssigned.id = :userId AND r.createdAt BETWEEN :startDate AND :endDate")
    List<Revenue> findAllByUserAssignedIdAndCreatedAtBetween(@Param("userId") Long userId, @Param("startDate") LocalDate from, @Param("endDate") LocalDate to);

    @Query("SELECT r From Revenue r WHERE r.userAssigned.id = :userId AND r.createdAt BETWEEN :startDate AND :endDate AND r.category = :category")
    List<Revenue> findAllByUserAssignedIdAndCreatedAtBetweenAndCategory(@Param("userId") Long userId, @Param("startDate") LocalDate from,
                                                                        @Param("endDate") LocalDate to, @Param("category") RevenueCategory category);

    // coalesce zwroci mi przychody lub 0 jest przychody sa null
    @Query("SELECT coalesce(sum(r.amount),0) from Revenue r WHERE r.userAssigned.id = :userId AND r.createdAt = :date")
    BigDecimal sumRevenueForDay(@Param("userId") Long userId, @Param("date") LocalDate date);

    @Query("SELECT coalesce(sum(r.amount),0) FROM Revenue r WHERE r.userAssigned.id = :userId")
    BigDecimal sumAllRevenuesByUserAssignedId(Long userId);

    @Query("SELECT SUM(r.amount) FROM Revenue r WHERE r.userAssigned.id = :userId AND r.createdAt >= :startDate AND r.createdAt <= :endDate")
    BigDecimal sumRevenuesByUserAndDateRange(Long userId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("""
             SELECT NEW com.finovara.finovarabackend.report.finances.highestrevenue.dto.HighestRevenueDto(
             r.category,
             r.amount
            )
            FROM Revenue r WHERE r.userAssigned.id = :userId AND r.createdAt BETWEEN :from AND :to ORDER BY r.amount DESC
            """)
    List<HighestRevenueDto> findHighestRevenuesByUserAssignedIdAndPeriod(@Param("userId") Long userId, LocalDate from, LocalDate to, Pageable pageable);

    @Query("""
                SELECT new com.finovara.finovarabackend.report.finances.chart.cashflow.dto.DailyAmountDto(
                    r.createdAt,
                    SUM(r.amount)
                )
                FROM Revenue r
                WHERE r.userAssigned.id = :userId
                GROUP BY r.createdAt
            """)
    List<DateAmountDto> sumRevenuesGroupedByDate(Long userId);

}