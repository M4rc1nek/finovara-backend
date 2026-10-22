package com.finovara.authservice.settings.security;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface SecuritySettingsRepository extends JpaRepository<SecuritySettings, Long> {

    @Query("SELECT ss FROM User u JOIN u.securitySettings ss WHERE u.id = :userId")
    SecuritySettings findByUserId(Long userId);

    @Modifying
    @Query("""
            UPDATE SecuritySettings ss
            SET ss.additionalAuthorizationAttempts = ss.additionalAuthorizationAttempts + 1,
                ss.version = ss.version + 1
            WHERE ss.id = (SELECT u.securitySettings.id FROM User u WHERE u.id = :userId)
            AND ss.additionalAuthorizationAttempts < :maxAttempts
        """)
    int incrementAdditionalAuthorizationAttempts(Long userId, int maxAttempts);

    @Modifying
    @Query("""
            UPDATE SecuritySettings ss
            SET ss.additionalAuthorizationVisible = :visible,
                ss.version = ss.version + 1
            WHERE ss.id = (SELECT u.securitySettings.id FROM User u WHERE u.id = :userId)
            AND ss.additionalAuthorizationVisible <> :visible
        """)
    int updateAdditionalAuthorizationVisible(Long userId, boolean visible);

    @Query("""
                SELECT ss.additionalAuthorizationAttempts
                FROM User u JOIN u.securitySettings ss
                WHERE u.id = :userId
            """)
    int getAdditionalAuthorizationAttemptsByUserId(Long userId);
}