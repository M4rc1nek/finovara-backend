package com.finovara.securitymonitoring.accountchange.repository;

import com.finovara.securitymonitoring.accountchange.model.AccountChangeProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AccountChangeProfileRepository extends JpaRepository<AccountChangeProfile, Long> {
    boolean existsByUserId(Long userId);
    Optional<AccountChangeProfile> findByUserId(Long userId);
}