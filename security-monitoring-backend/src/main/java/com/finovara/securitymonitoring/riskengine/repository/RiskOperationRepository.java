package com.finovara.securitymonitoring.riskengine.repository;

import com.finovara.securitymonitoring.riskengine.model.RiskOperation;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RiskOperationRepository extends JpaRepository<RiskOperation, Long> {
    Optional<RiskOperation> findBySourceEventId(String sourceEventId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT ro FROM RiskOperation ro WHERE ro.id = :riskOperationId")
    Optional<RiskOperation> findByIdForUpdate(Long riskOperationId);
}