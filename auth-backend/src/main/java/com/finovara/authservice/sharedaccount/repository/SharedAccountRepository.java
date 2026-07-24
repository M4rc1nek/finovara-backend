package com.finovara.authservice.sharedaccount.repository;

import com.finovara.authservice.sharedaccount.model.SharedAccount;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface SharedAccountRepository extends JpaRepository<SharedAccount, Long> {

    @Modifying
    @Query("DELETE FROM SharedAccount a WHERE a.id = :accountId")
    void deleteAccountById(Long accountId);


    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT sa FROM SharedAccount sa WHERE sa.id = :accountId")
    Optional<SharedAccount> findByIdForUpdate(Long accountId);
}
