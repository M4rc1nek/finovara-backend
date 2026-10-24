package com.finovara.activitylogservice.activitylog.securitymonitoring.repository;

import com.finovara.activitylogservice.activitylog.securitymonitoring.model.RiskOperationActivity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RiskOperationActivityRepository extends JpaRepository<RiskOperationActivity, Long> {

    List<RiskOperationActivity> findByUserId(Long userId, Pageable pageable);

    boolean existsBySourceEventId(String sourceEventId);

    void deleteByUserId(Long userId);
}