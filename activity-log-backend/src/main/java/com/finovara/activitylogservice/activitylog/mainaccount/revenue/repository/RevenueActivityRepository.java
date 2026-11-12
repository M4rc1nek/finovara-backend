package com.finovara.activitylogservice.activitylog.mainaccount.revenue.repository;

import com.finovara.activitylogservice.activitylog.mainaccount.revenue.dto.RevenueActivityFilterDto;
import com.finovara.activitylogservice.activitylog.mainaccount.revenue.model.RevenueActivity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface RevenueActivityRepository extends JpaRepository<RevenueActivity, Long> {

    List<RevenueActivity> findByUserId(Long userId, Pageable pageable);

    @Query("""
            SELECT r FROM RevenueActivity r
            WHERE r.userId = :userId
              AND (CAST(:#{#filter.type} AS string) IS NULL OR r.type = :#{#filter.type})
              AND (CAST(:#{#filter.amount} AS string) IS NULL OR r.amount = :#{#filter.amount})
              AND (CAST(:#{#filter.previousAmount} AS string) IS NULL OR r.previousAmount = :#{#filter.previousAmount})
              AND (CAST(:#{#filter.category} AS string) IS NULL OR r.category = :#{#filter.category})
              AND (CAST(:#{#filter.previousCategory} AS string) IS NULL OR r.previousCategory = :#{#filter.previousCategory})
              AND (CAST(:createdAt AS date) IS NULL OR CAST(r.createdAt AS date) = :createdAt)
            ORDER BY r.createdAt DESC, r.id DESC
            """)
    List<RevenueActivity> findFilteredActivities(Long userId, RevenueActivityFilterDto filter, LocalDate createdAt, Pageable pageable);

    List<RevenueActivity> findAllByUserId(Long userId);
}
