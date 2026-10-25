package com.finovara.authservice.settings.security;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SecuritySettingsRepository extends JpaRepository<SecuritySettings, Long> {

    @Query("SELECT ss FROM User u JOIN u.securitySettings ss WHERE u.id = :userId")
    SecuritySettings findByUserId(Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT ss FROM SecuritySettings ss
            WHERE ss.id = (SELECT u.securitySettings.id FROM User u WHERE u.id = :userId)
            """)
    Optional<SecuritySettings> findByUserIdForUpdate(Long userId);

    @Query("""
                SELECT ss.additionalAuthorizationAttempts
                FROM User u JOIN u.securitySettings ss
                WHERE u.id = :userId
            """)
    int getAdditionalAuthorizationAttemptsByUserId(Long userId);
}