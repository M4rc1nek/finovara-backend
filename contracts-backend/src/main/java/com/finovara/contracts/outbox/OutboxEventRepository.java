package com.finovara.contracts.outbox;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT oe FROM OutboxEvent oe WHERE oe.status = :status ORDER BY oe.createdAt ASC LIMIT 10")
    List<OutboxEvent> findPendingEvents(OutboxStatus status);

    @Query("SELECT oe FROM OutboxEvent oe WHERE oe.status = :status AND oe.sentAt < :before")
    List<OutboxEvent> findByStatusAndSentAtBefore(OutboxStatus status, LocalDateTime before);
}