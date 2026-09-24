package com.finovara.activitylogservice.activitylog.securitymonitoring.repository;

import com.finovara.activitylogservice.activitylog.securitymonitoring.model.RiskOperationActivity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface RiskOperationActivityRepository extends JpaRepository<RiskOperationActivity, Long> {

    @Query("SELECT ro FROM RiskOperationActivity ro WHERE ro.userId = :userId")
    List<RiskOperationActivity> findByUserId(Long userId, Pageable pageable);

    boolean existsBySourceEventId(String sourceEventId);

    void deleteByUserId(Long userId);
}