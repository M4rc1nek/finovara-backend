package com.finovara.authservice.sharedaccount.repository;

import com.finovara.authservice.sharedaccount.model.SharedAccount;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SharedAccountRepository extends JpaRepository<SharedAccount, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT sa FROM SharedAccount sa WHERE sa.id = :accountId")
    Optional<SharedAccount> findByIdForUpdate(Long accountId);

    @Query("SELECT sa FROM SharedAccount sa JOIN sa.members m WHERE sa.id = :accountId AND m.userId = :userId")
    Optional<SharedAccount> findByIdAndUserId(Long accountId, Long userId);
}
