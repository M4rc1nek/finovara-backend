package com.finovara.authservice.settings.account.repository;

import com.finovara.authservice.settings.account.model.AccountSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface AccountRepository extends JpaRepository<AccountSettings, Long> {

    @Modifying
    @Query("""
                UPDATE AccountSettings a
                SET a.emailChangeAttempts = a.emailChangeAttempts + 1,
                    a.version = a.version + 1
                WHERE a.id = (SELECT u.accountSettings.id FROM User u WHERE u.id = :userId)
                AND a.emailChangeAttempts < :maxAttempts
            """)
    int incrementEmailChangeAttempts(Long userId, int maxAttempts);

    @Modifying
    @Query("""
                UPDATE AccountSettings a
                SET a.passwordResetAttempts = a.passwordResetAttempts + 1,
                    a.version = a.version + 1
                WHERE a.id = (SELECT u.accountSettings.id FROM User u WHERE LOWER(u.email) = LOWER(:email))
                AND a.passwordResetAttempts < :maxAttempts
            """)
    int incrementPasswordResetAttempts(String email, int maxAttempts);

    @Query("""
                SELECT a.emailChangeAttempts
                FROM User u JOIN u.accountSettings a
                WHERE u.id = :userId
            """)
    int getEmailChangeAttemptsByUserId(Long userId);

    @Query("""
                SELECT a.passwordResetAttempts
                FROM User u JOIN u.accountSettings a
                WHERE LOWER(u.email) = LOWER(:email)
            """)
    int getPasswordResetAttemptsByUserEmail(String email);
}