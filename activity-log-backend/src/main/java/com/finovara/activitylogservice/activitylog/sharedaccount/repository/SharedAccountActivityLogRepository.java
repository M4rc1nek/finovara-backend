package com.finovara.activitylogservice.activitylog.sharedaccount.repository;

import com.finovara.activitylogservice.activitylog.sharedaccount.dto.SharedAccountActivityLogFilterDto;
import com.finovara.activitylogservice.activitylog.sharedaccount.model.SharedAccountActivityLog;
import com.finovara.contracts.sharedaccount.SharedAccountActivityLogType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SharedAccountActivityLogRepository extends JpaRepository<SharedAccountActivityLog, Long> {

    @Query("SELECT a FROM SharedAccountActivityLog a WHERE a.ownerId = :userId OR a.memberId = :userId")
    List<SharedAccountActivityLog> findAllByOwnerIdOrMemberId(Long userId, Pageable pageable);

    @Query("SELECT a FROM SharedAccountActivityLog a WHERE a.ownerId = :ownerId AND a.memberId = :memberId")
    List<SharedAccountActivityLog> findAllByOwnerIdAndMemberId(Long ownerId, Long memberId);

    @Query("""
        SELECT a FROM SharedAccountActivityLog a
        WHERE (a.ownerId = :userId OR a.memberId = :userId)
          AND (CAST(:#{#filter.type} AS string) IS NULL OR a.activityType = :#{#filter.type})
          AND (CAST(:#{#filter.targetId} AS string) IS NULL OR a.targetId = :#{#filter.targetId})
          AND (CAST(:actorId AS string) IS NULL OR a.userId = :actorId)
          AND (CAST(:createdFrom AS timestamp) IS NULL OR a.createdAt >= :createdFrom)
          AND (CAST(:createdTo AS timestamp) IS NULL OR a.createdAt < :createdTo)
        ORDER BY a.createdAt DESC, a.id DESC
        """)
    List<SharedAccountActivityLog> findFilteredActivities(Long userId, SharedAccountActivityLogFilterDto filter, Long actorId, LocalDateTime createdFrom, LocalDateTime createdTo, Pageable pageable);

    List<SharedAccountActivityLog> findAllByUserId(Long userId);

    @Query("""
            SELECT COUNT(a)
            FROM SharedAccountActivityLog a
            WHERE a.userId = :userId
              AND a.activityType = :activityType
              AND a.createdAt BETWEEN :from AND :to
            """)
    int countByUserIdAndActivityType(Long userId, SharedAccountActivityLogType activityType, LocalDateTime from, LocalDateTime to);

    @Query("""
            SELECT a.createdAt
            FROM SharedAccountActivityLog a
            WHERE a.userId = :userId
              AND a.activityType = :activityType
            ORDER BY a.createdAt DESC
            LIMIT 1
            """)
    LocalDateTime findLastActivityDateByUserIdAndActivityType(Long userId, SharedAccountActivityLogType activityType);

    @Query("""
            SELECT a.createdAt
            FROM SharedAccountActivityLog a
            WHERE a.userId = :userId
            ORDER BY a.createdAt DESC
            LIMIT 1
            """)
    LocalDateTime findLastActivityDateByUserId(Long userId);
}