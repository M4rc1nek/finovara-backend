package com.finovara.financeservice.sharedaccount.revenue.repository;

import com.finovara.contracts.mainaccount.report.financial.dto.DailyCashDto;
import com.finovara.contracts.mainaccount.report.financial.dto.HighestRevenueDto;
import com.finovara.contracts.util.model.RevenueCategory;
import com.finovara.financeservice.internal.report.util.dto.RevenueCategorySumDto;
import com.finovara.financeservice.sharedaccount.revenue.model.SharedRevenue;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SharedRevenueRepository extends JpaRepository<SharedRevenue, Long> {

    @Query("SELECT sr FROM SharedRevenue sr WHERE sr.ownerId = :userId OR sr.memberId = :userId")
    List<SharedRevenue> findAllByOwnerIdOrMemberId(Long userId);

    @Query("SELECT e FROM SharedRevenue e WHERE e.ownerId = :ownerId AND e.memberId = :memberId")
    List<SharedRevenue> findAllByOwnerIdAndMemberId(Long ownerId, Long memberId);

    @Query("SELECT sr FROM SharedRevenue sr WHERE sr.id = :revenueId AND (sr.ownerId = :userId OR sr.memberId = :userId)")
    Optional<SharedRevenue> findByIdAndOwnerIdOrMemberId(Long revenueId, Long userId);

    @Query("SELECT sr FROM SharedRevenue sr WHERE sr.ownerId = :userId OR sr.memberId = :userId ORDER BY sr.createdAt DESC, sr.id DESC")
    List<SharedRevenue> findAllByOwnerIdOrMemberId(Long userId, Pageable pageable);

    @Query("""
            SELECT r FROM SharedRevenue r
            WHERE (r.ownerId = :userId OR r.memberId = :userId)
              AND (:createdByUserId IS NULL OR r.createdByUserId = :createdByUserId)
              AND (COALESCE(:category, r.category) = r.category)
              AND (COALESCE(:amount, r.amount) = r.amount)
              AND (COALESCE(:createdAt, r.createdAt) = r.createdAt)
              AND (COALESCE(:query, '') = '' OR LOWER(r.description) LIKE LOWER(CONCAT('%', :query, '%')))
            ORDER BY r.createdAt DESC, r.id DESC
            """)
    List<SharedRevenue> findFilteredRevenues(Long userId, Long createdByUserId,
                                             RevenueCategory category, BigDecimal amount,
                                             LocalDate createdAt, String query, Pageable pageable);

    @Query("""
            SELECT COALESCE(SUM(r.amount), 0) FROM SharedRevenue r
            WHERE r.ownerId = :ownerId AND r.memberId = :memberId AND r.createdByUserId = :userId
            """)
    BigDecimal sumRevenueByCreatedByUserId(Long ownerId, Long memberId, Long userId);

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
    List<RevenueCategorySumDto> sumRevenuesGroupedByCategoryForOwnerIdOrMemberId(Long ownerId, Long memberId, LocalDate from, LocalDate to);

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
    List<HighestRevenueDto> findHighestRevenuesByOwnerIdOrMemberIdAndPeriod(Long ownerId, Long memberId, LocalDate from, LocalDate to, Pageable pageable);

    @Query("""
                SELECT new com.finovara.contracts.mainaccount.report.financial.dto.DailyCashDto(
                    r.createdAt,
                    SUM(r.amount)
                )
                FROM SharedRevenue r
                WHERE r.ownerId = :ownerId OR r.memberId = :memberId
                GROUP BY r.createdAt
            """)
    List<DailyCashDto> sumRevenuesGroupedByDateForOwnerIdOrMemberId(Long ownerId, Long memberId);

    @Query("""
                SELECT new com.finovara.contracts.mainaccount.report.financial.dto.DailyCashDto(
                    r.createdAt,
                    CAST(AVG(r.amount) AS big_decimal)
                )
                FROM SharedRevenue r
                WHERE r.ownerId = :ownerId OR r.memberId = :memberId
                GROUP BY r.createdAt
            """)
    List<DailyCashDto> avgRevenuesGroupedByDateForOwnerIdOrMemberId(Long ownerId, Long memberId);

}