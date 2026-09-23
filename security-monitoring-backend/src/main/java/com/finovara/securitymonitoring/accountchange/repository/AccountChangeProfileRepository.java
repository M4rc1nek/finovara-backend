package com.finovara.securitymonitoring.accountchange.repository;

import com.finovara.securitymonitoring.accountchange.model.AccountChangeProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AccountChangeProfileRepository extends JpaRepository<AccountChangeProfile, Long> {
    boolean existsByUserId(Long userId);

    @Query("SELECT ac FROM AccountChangeProfile ac WHERE ac.userId = :userId")
    Optional<AccountChangeProfile> findByUserId(Long userId);

    void deleteByUserId(Long userId);

}