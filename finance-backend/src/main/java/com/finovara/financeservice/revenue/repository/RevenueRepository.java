package com.finovara.financeservice.revenue.repository;

import com.finovara.contracts.transaction.report.dto.DailyCashDto;
import com.finovara.contracts.transaction.report.dto.HighestRevenueDto;
import com.finovara.financeservice.revenue.model.Revenue;
import com.finovara.contracts.model.transaction.RevenueCategory;
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

    Optional<Revenue> findByIdAndUserId(Long revenueId, Long userId);

    List<Revenue> findAllByUserId(Long userId);

    @Query("SELECT r From Revenue r WHERE r.userId = :userId AND r.createdAt BETWEEN :startDate AND :endDate AND r.category = :category")
    List<Revenue> findAllByUserIdAndCreatedAtBetweenAndCategory(Long userId, @Param("startDate") LocalDate from,
                                                                        @Param("endDate") LocalDate to, RevenueCategory category);

    @Query("SELECT coalesce(sum(r.amount),0) FROM Revenue r WHERE r.userId = :userId")
    BigDecimal sumAllRevenuesByUserId(Long userId);

    @Query("SELECT SUM(r.amount) FROM Revenue r WHERE r.userId = :userId AND r.createdAt >= :startDate AND r.createdAt <= :endDate")
    Optional<BigDecimal> sumRevenuesByUserAndDateRange(Long userId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT CAST(AVG(r.amount) AS big_decimal) FROM Revenue r WHERE r.userId = :userId AND r.createdAt BETWEEN :startDate AND :endDate")
    Optional<BigDecimal> avgRevenuesByUserIdAndPeriod(Long userId, @Param("startDate") LocalDate from, @Param("endDate") LocalDate to);

    @Query("""
             SELECT NEW com.finovara.contracts.transaction.report.dto.HighestRevenueDto(
             r.category,
             r.amount
            )
            FROM Revenue r WHERE r.userId = :userId AND r.createdAt BETWEEN :from AND :to ORDER BY r.amount DESC
            """)
    List<HighestRevenueDto> findHighestRevenuesByUserIdAndPeriod(Long userId, LocalDate from, LocalDate to, Pageable pageable);

    @Query("""
                SELECT new com.finovara.contracts.transaction.report.dto.DailyCashDto(
                    r.createdAt,
                    SUM(r.amount)
                )
                FROM Revenue r
                WHERE r.userId = :userId
                GROUP BY r.createdAt
            """)
    List<DailyCashDto> sumRevenuesGroupedByDate(Long userId);

    @Query("""
                SELECT new com.finovara.contracts.transaction.report.dto.DailyCashDto(
                    r.createdAt,
                    CAST(AVG(r.amount) AS big_decimal)
                )
                FROM Revenue r
                WHERE r.userId = :userId
                GROUP BY r.createdAt
            """)
    List<DailyCashDto> avgRevenuesGroupedByDate(Long userId);

    void deleteByUserId(Long userId);

}
