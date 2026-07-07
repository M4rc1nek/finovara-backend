package com.finovara.financeservice.sharedaccount.repository.revenue;

import com.finovara.financeservice.sharedaccount.model.revenue.SharedRevenue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface SharedRevenueRepository extends JpaRepository<SharedRevenue, Long> {

    @Query("SELECT sr FROM SharedRevenue sr WHERE sr.ownerId = :userId OR sr.memberId = :userId")
    List<SharedRevenue> findAllByOwnerIdOrMemberId(Long userId);

    @Query("SELECT sr FROM SharedRevenue sr WHERE sr.id = :revenueId AND (sr.ownerId = :userId OR sr.memberId = :userId)")
    Optional<SharedRevenue> findByIdAndOwnerIdOrMemberId(Long revenueId, Long userId);

    @Modifying
    @Query("DELETE FROM SharedRevenue sr WHERE sr.ownerId = :ownerId AND sr.memberId = :memberId")
    void deleteAllByOwnerIdAndMemberId(Long ownerId, Long memberId);

    @Query("""
            SELECT COALESCE(SUM(r.amount), 0) FROM SharedRevenue r
            WHERE r.ownerId = :ownerId AND r.memberId = :memberId AND r.createdByUserId = :userId
            """)
    BigDecimal sumRevenueByCreatedByUserId(Long ownerId, Long memberId, Long userId);

}
