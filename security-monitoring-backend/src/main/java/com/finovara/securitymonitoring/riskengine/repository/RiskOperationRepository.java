package com.finovara.securitymonitoring.riskengine.repository;

import com.finovara.securitymonitoring.riskengine.model.RiskOperation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RiskOperationRepository extends JpaRepository<RiskOperation, Long> {
    Optional<RiskOperation> findBySourceEventId(String sourceEventId);
}