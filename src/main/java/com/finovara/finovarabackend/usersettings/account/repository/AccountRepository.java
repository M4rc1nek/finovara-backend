package com.finovara.finovarabackend.usersettings.account.repository;

import com.finovara.finovarabackend.usersettings.account.model.AccountSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AccountRepository extends JpaRepository<AccountSettings, Long> {

    @Query("SELECT a FROM AccountSettings a WHERE a.userAssigned.id = :userId AND a.forgotPasswordCode = :forgotPasswordCode")
    int getForgotPasswordCode(Long userId, @Param("forgotPasswordCode") int forgotPasswordCode);
}
