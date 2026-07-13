package com.finovara.financeservice.sharedaccount.revenue.model;

import com.finovara.contracts.model.transaction.RevenueCategory;
import com.finovara.contracts.transaction.report.dto.DailyCashDto;
import com.finovara.contracts.transaction.report.dto.HighestRevenueDto;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SharedRevenueRepository extends JpaRepository<SharedRevenue, Long> {

    @Query("SELECT sr FROM SharedRevenue sr WHERE sr.ownerId = :userId OR sr.memberId = :userId")
    List<SharedRevenue> findAllByOwnerIdOrMemberId(@Param("userId") Long userId);

    @Query("SELECT sr FROM SharedRevenue sr WHERE sr.id = :revenueId AND (sr.ownerId = :userId OR sr.memberId = :userId)")
    Optional<SharedRevenue> findByIdAndOwnerIdOrMemberId(@Param("revenueId") Long revenueId, @Param("userId") Long userId);

    @Modifying
    @Query("DELETE FROM SharedRevenue sr WHERE sr.ownerId = :ownerId AND sr.memberId = :memberId")
    void deleteAllByOwnerIdAndMemberId(@Param("ownerId") Long ownerId, @Param("memberId") Long memberId);

    @Query("""
            SELECT COALESCE(SUM(r.amount), 0) FROM SharedRevenue r
            WHERE r.ownerId = :ownerId AND r.memberId = :memberId AND r.createdByUserId = :userId
            """)
    BigDecimal sumRevenueByCreatedByUserId(@Param("ownerId") Long ownerId, @Param("memberId") Long memberId, @Param("userId") Long userId);

    @Query("""
            SELECT r FROM SharedRevenue r
            WHERE (r.ownerId = :ownerId OR r.memberId = :memberId)
            AND r.createdAt BETWEEN :startDate AND :endDate
            AND r.category = :category
            """)
    List<SharedRevenue> findAllByOwnerIdOrMemberIdAndCreatedAtBetweenAndCategory(@Param("ownerId") Long ownerId, @Param("memberId") Long memberId, @Param("startDate") LocalDate from, @Param("endDate") LocalDate to, @Param("category") RevenueCategory category);

    @Query("""
            SELECT COALESCE(SUM(r.amount), 0) FROM SharedRevenue r
            WHERE r.ownerId = :ownerId OR r.memberId = :memberId
            """)
    BigDecimal sumAllRevenuesByOwnerIdOrMemberId(@Param("ownerId") Long ownerId, @Param("memberId") Long memberId);

    @Query("""
            SELECT SUM(r.amount) FROM SharedRevenue r
            WHERE (r.ownerId = :ownerId OR r.memberId = :memberId)
            AND r.createdAt >= :startDate AND r.createdAt <= :endDate
            """)
    Optional<BigDecimal> sumRevenuesByOwnerIdOrMemberIdAndDateRange(@Param("ownerId") Long ownerId, @Param("memberId") Long memberId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("""
             SELECT NEW com.finovara.contracts.transaction.report.dto.HighestRevenueDto(
             r.category,
             r.amount
            )
            FROM SharedRevenue r
            WHERE (r.ownerId = :ownerId OR r.memberId = :memberId)
            AND r.createdAt BETWEEN :from AND :to
            ORDER BY r.amount DESC
            """)
    List<HighestRevenueDto> findHighestRevenuesByOwnerIdOrMemberIdAndPeriod(@Param("ownerId") Long ownerId, @Param("memberId") Long memberId, @Param("from") LocalDate from, @Param("to") LocalDate to, Pageable pageable);

    @Query("""
                SELECT new com.finovara.contracts.transaction.report.dto.DailyCashDto(
                    r.createdAt,
                    SUM(r.amount)
                )
                FROM SharedRevenue r
                WHERE r.ownerId = :ownerId OR r.memberId = :memberId
                GROUP BY r.createdAt
            """)
    List<DailyCashDto> sumRevenuesGroupedByDateForOwnerIdOrMemberId(@Param("ownerId") Long ownerId, @Param("memberId") Long memberId);

    @Query("""
                SELECT new com.finovara.contracts.transaction.report.dto.DailyCashDto(
                    r.createdAt,
                    CAST(AVG(r.amount) AS big_decimal)
                )
                FROM SharedRevenue r
                WHERE r.ownerId = :ownerId OR r.memberId = :memberId
                GROUP BY r.createdAt
            """)
    List<DailyCashDto> avgRevenuesGroupedByDateForOwnerIdOrMemberId(@Param("ownerId") Long ownerId, @Param("memberId") Long memberId);

}
