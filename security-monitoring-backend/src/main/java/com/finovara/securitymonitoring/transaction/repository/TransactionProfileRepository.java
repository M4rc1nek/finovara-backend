package com.finovara.securitymonitoring.transaction.repository;

import com.finovara.securitymonitoring.transaction.model.TransactionProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TransactionProfileRepository extends JpaRepository<TransactionProfile, Long> {
    boolean existsByUserId(Long userId);

    @Query("SELECT tp FROM TransactionProfile tp WHERE tp.userId = :userId")
    Optional<TransactionProfile> findByUserId(Long userId);

    void deleteByUserId(Long userId);
}