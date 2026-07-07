package com.finovara.financeservice.sharedaccount.repository;

import com.finovara.financeservice.sharedaccount.model.revenue.SharedRevenue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface SharedRevenueRepository extends JpaRepository<SharedRevenue, Long> {

    @Query("SELECT sr FROM SharedRevenue sr WHERE sr.ownerId = :userId OR sr.memberId = :userId")
    List<SharedRevenue> findAllByOwnerIdOrMemberId(Long userId);

    @Query("SELECT sr FROM SharedRevenue sr WHERE sr.id = :revenueId AND (sr.ownerId = :userId OR sr.memberId = :userId)")
    Optional<SharedRevenue> findByIdAndOwnerIdOrMemberId(Long revenueId, Long userId);
}