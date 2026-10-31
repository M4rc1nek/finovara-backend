package com.finovara.activitylogservice.activitylog.mainaccount.limit.repository;

import com.finovara.activitylogservice.activitylog.mainaccount.limit.dto.LimitActivityFilterDto;
import com.finovara.activitylogservice.activitylog.mainaccount.limit.model.LimitActivity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface LimitActivityRepository extends JpaRepository<LimitActivity, Long> {

    List<LimitActivity> findByUserId(Long userId, Pageable pageable);

    @Query("""
            SELECT l FROM LimitActivity l
            WHERE l.userId = :userId
              AND (CAST(:#{#filter.limitActivityType} AS string) IS NULL OR l.limitActivityType = :#{#filter.limitActivityType})
              AND (CAST(:#{#filter.periodType} AS string) IS NULL OR l.periodType = :#{#filter.periodType})
              AND (CAST(:#{#filter.amount} AS string) IS NULL OR l.amount = :#{#filter.amount})
              AND (CAST(:#{#filter.previousAmount} AS string) IS NULL OR l.previousAmount = :#{#filter.previousAmount})
              AND (CAST(:createdFrom AS timestamp) IS NULL OR l.createdAt >= :createdFrom)
              AND (CAST(:createdTo AS timestamp) IS NULL OR l.createdAt < :createdTo)
            ORDER BY l.createdAt DESC, l.id DESC
            """)
    List<LimitActivity> findFilteredActivities( Long userId, LimitActivityFilterDto filter, LocalDateTime createdFrom, LocalDateTime createdTo,
                                               Pageable pageable);

    List<LimitActivity> findAllByUserId(Long userId);

}
