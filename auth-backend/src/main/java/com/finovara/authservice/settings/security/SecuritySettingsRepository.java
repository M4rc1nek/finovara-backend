package com.finovara.authservice.settings.security;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface SecuritySettingsRepository extends JpaRepository<SecuritySettings, Long> {

    @Query("SELECT ss FROM SecuritySettings ss WHERE ss.userAssigned.id = :userId")
    SecuritySettings findByUserId(Long userId);

    @Modifying
    @Query("""
                UPDATE SecuritySettings ss
                SET ss.additionalAuthorizationAttempts = ss.additionalAuthorizationAttempts + 1
                WHERE ss.userAssigned.id = :userId AND ss.additionalAuthorizationAttempts < :maxAttempts
            """)
    int incrementAdditionalAuthorizationAttempts(Long userId, int maxAttempts);

    @Query("SELECT ss.additionalAuthorizationAttempts FROM SecuritySettings ss WHERE ss.userAssigned.id = :userId")
    int getAdditionalAuthorizationAttemptsByUserId(Long userId);
}
