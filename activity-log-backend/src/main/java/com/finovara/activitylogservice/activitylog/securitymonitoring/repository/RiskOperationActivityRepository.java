package com.finovara.activitylogservice.activitylog.securitymonitoring.repository;

import com.finovara.activitylogservice.activitylog.securitymonitoring.dto.RiskOperationActivityFilterDto;
import com.finovara.activitylogservice.activitylog.securitymonitoring.model.RiskOperationActivity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface RiskOperationActivityRepository extends JpaRepository<RiskOperationActivity, Long> {

    List<RiskOperationActivity> findByUserId(Long userId, Pageable pageable);

    @Query("""
            SELECT r FROM RiskOperationActivity r
            WHERE r.userId = :userId
              AND (CAST(:#{#filter.sourceEventId} AS string) IS NULL OR :#{#filter.sourceEventId} = '' OR LOWER(r.sourceEventId) LIKE LOWER(CONCAT('%', :#{#filter.sourceEventId}, '%')))
              AND (CAST(:#{#filter.triggerType} AS string) IS NULL OR r.triggerType = :#{#filter.triggerType})
              AND (CAST(:#{#filter.score} AS string) IS NULL OR r.score = :#{#filter.score})
              AND (CAST(:#{#filter.action} AS string) IS NULL OR r.action = :#{#filter.action})
              AND (CAST(:#{#filter.operationDate} AS string) IS NULL OR r.operationDate = :#{#filter.operationDate})
              AND (CAST(:createdAt AS date) IS NULL OR CAST(r.createdAt AS date) = :createdAt)
            ORDER BY r.createdAt DESC, r.id DESC
            """)
    List<RiskOperationActivity> findFilteredActivities(Long userId, RiskOperationActivityFilterDto filter, LocalDate createdAt, Pageable pageable);

    boolean existsBySourceEventId(String sourceEventId);

    List<RiskOperationActivity> findAllByUserId(Long userId);
}
