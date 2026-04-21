package com.finovara.finovarabackend.usersetting.account.repository;

import com.finovara.finovarabackend.usersetting.account.model.AccountSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AccountRepository extends JpaRepository<AccountSettings, Long> {

    @Modifying
    @Query("""
                UPDATE AccountSettings a
                SET a.emailChangeAttempts = a.emailChangeAttempts + 1
                WHERE a.userAssigned.id = :userId AND a.emailChangeAttempts < :maxAttempts
            """)
    int incrementEmailChangeAttempts(@Param("userId") Long userId, @Param("maxAttempts") int maxAttempts);

    @Modifying
    @Query("""
                UPDATE AccountSettings a
                SET a.passwordResetAttempts = a.passwordResetAttempts + 1
                WHERE a.userAssigned.email = :email AND a.passwordResetAttempts < :maxAttempts
            """)
    int incrementPasswordResetAttempts(@Param("email") String email, @Param("maxAttempts") int maxAttempts);

    @Query("SELECT a.emailChangeAttempts FROM AccountSettings a WHERE a.userAssigned.id = :userId")
    int getEmailChangeAttemptsByUserId(@Param("userId") Long userId);

    @Query("SELECT a.passwordResetAttempts FROM AccountSettings a WHERE a.userAssigned.email = :email")
    int getPasswordResetAttemptsByUserId(@Param("email") String email);
}