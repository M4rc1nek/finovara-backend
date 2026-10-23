package com.finovara.authservice.settings.account.repository;

import com.finovara.authservice.settings.account.model.AccountSettings;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
@Repository
public interface AccountRepository extends JpaRepository<AccountSettings, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT a FROM AccountSettings a
            WHERE a.id = (SELECT u.accountSettings.id FROM User u WHERE u.id = :userId)
            """)
    Optional<AccountSettings> findByUserIdForUpdate(Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT a FROM AccountSettings a
            WHERE a.id = (SELECT u.accountSettings.id FROM User u WHERE LOWER(u.email) = LOWER(:email))
            """)
    Optional<AccountSettings> findByUserEmailForUpdate(String email);

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