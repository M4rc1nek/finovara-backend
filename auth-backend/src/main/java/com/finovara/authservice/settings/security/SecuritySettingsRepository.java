package com.finovara.authservice.settings.security;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface SecuritySettingsRepository extends JpaRepository<SecuritySettings, Long> {

    @Query("SELECT ss FROM SecuritySettings ss WHERE ss.userAssigned.id = :userId")
    SecuritySettings findByUserId(Long userId);

}
