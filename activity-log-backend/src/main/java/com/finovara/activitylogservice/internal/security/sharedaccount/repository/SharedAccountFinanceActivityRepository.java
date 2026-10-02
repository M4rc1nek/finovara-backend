package com.finovara.activitylogservice.internal.security.sharedaccount.repository;

import com.finovara.activitylogservice.internal.security.sharedaccount.model.SharedAccountFinanceActivity;
import com.finovara.contracts.mainaccount.activity.event.sharedaccount.SharedFinanceActivityType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface SharedAccountFinanceActivityRepository extends JpaRepository<SharedAccountFinanceActivity, Long> {

    @Query("""
            SELECT COUNT(a)
            FROM SharedAccountFinanceActivity a
            WHERE a.userId = :userId
              AND a.activityType = :activityType
              AND a.createdAt BETWEEN :from AND :to
            """)
    int countByUserIdAndActivityType(Long userId, SharedFinanceActivityType activityType, LocalDateTime from, LocalDateTime to);

    @Query("""
            SELECT a.createdAt
            FROM SharedAccountFinanceActivity a
            WHERE a.userId = :userId
              AND a.activityType = :activityType
            ORDER BY a.createdAt DESC
            LIMIT 1
            """)
    LocalDateTime findLastActivityDateByUserIdAndActivityType(Long userId, SharedFinanceActivityType activityType);

    @Query("""
            SELECT a.createdAt
            FROM SharedAccountFinanceActivity a
            WHERE a.userId = :userId
            ORDER BY a.createdAt DESC
            LIMIT 1
            """)
    LocalDateTime findLastActivityDateByUserId(Long userId);
}