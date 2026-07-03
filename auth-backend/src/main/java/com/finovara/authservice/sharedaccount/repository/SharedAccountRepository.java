package com.finovara.authservice.sharedaccount.repository;

import com.finovara.authservice.sharedaccount.model.SharedAccount;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SharedAccountRepository extends JpaRepository<SharedAccount, Long> {
}
