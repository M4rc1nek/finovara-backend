package com.finovara.financeservice.sharedaccount.revenue.repository;

import com.finovara.contracts.mainaccount.report.financial.dto.DailyCashDto;
import com.finovara.contracts.mainaccount.report.financial.dto.HighestRevenueDto;
import com.finovara.financeservice.internal.report.util.dto.RevenueCategorySumDto;
import com.finovara.financeservice.sharedaccount.revenue.model.SharedRevenue;
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
            SELECT NEW com.finovara.financeservice.internal.report.util.dto.RevenueCategorySumDto(
                r.category,
                SUM(r.amount)
            )
            FROM SharedRevenue r
            WHERE (r.ownerId = :ownerId OR r.memberId = :memberId)
            AND r.createdAt BETWEEN :from AND :to
            GROUP BY r.category
            """)
    List<RevenueCategorySumDto> sumRevenuesGroupedByCategoryForOwnerIdOrMemberId(@Param("ownerId") Long ownerId, @Param("memberId") Long memberId, @Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("""
             SELECT NEW com.finovara.contracts.mainaccount.report.financial.dto.HighestRevenueDto(
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
                SELECT new com.finovara.contracts.mainaccount.report.financial.dto.DailyCashDto(
                    r.createdAt,
                    SUM(r.amount)
                )
                FROM SharedRevenue r
                WHERE r.ownerId = :ownerId OR r.memberId = :memberId
                GROUP BY r.createdAt
            """)
    List<DailyCashDto> sumRevenuesGroupedByDateForOwnerIdOrMemberId(@Param("ownerId") Long ownerId, @Param("memberId") Long memberId);

    @Query("""
                SELECT new com.finovara.contracts.mainaccount.report.financial.dto.DailyCashDto(
                    r.createdAt,
                    CAST(AVG(r.amount) AS big_decimal)
                )
                FROM SharedRevenue r
                WHERE r.ownerId = :ownerId OR r.memberId = :memberId
                GROUP BY r.createdAt
            """)
    List<DailyCashDto> avgRevenuesGroupedByDateForOwnerIdOrMemberId(@Param("ownerId") Long ownerId, @Param("memberId") Long memberId);

}