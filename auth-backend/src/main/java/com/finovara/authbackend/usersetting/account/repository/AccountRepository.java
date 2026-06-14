package com.finovara.authbackend.usersetting.account.repository;

import com.finovara.authbackend.usersetting.account.model.AccountSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface AccountRepository extends JpaRepository<AccountSettings, Long> {

    @Modifying
    @Query("""
                UPDATE AccountSettings a
                SET a.emailChangeAttempts = a.emailChangeAttempts + 1
                WHERE a.userAssigned.id = :userId AND a.emailChangeAttempts < :maxAttempts
            """)
    int incrementEmailChangeAttempts(Long userId, int maxAttempts);

    @Modifying
    @Query("""
                UPDATE AccountSettings a
                SET a.passwordResetAttempts = a.passwordResetAttempts + 1
                WHERE a.userAssigned.email = :email AND a.passwordResetAttempts < :maxAttempts
            """)
    int incrementPasswordResetAttempts(String email, int maxAttempts);

    @Query("SELECT a.emailChangeAttempts FROM AccountSettings a WHERE a.userAssigned.id = :userId")
    int getEmailChangeAttemptsByUserId(Long userId);

    @Query("SELECT a.passwordResetAttempts FROM AccountSettings a WHERE a.userAssigned.email = :email")
    int getPasswordResetAttemptsByUserEmail(String email);
}