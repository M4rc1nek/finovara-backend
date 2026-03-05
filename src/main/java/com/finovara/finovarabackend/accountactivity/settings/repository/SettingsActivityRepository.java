package com.finovara.finovarabackend.accountactivity.settings.repository;

import com.finovara.finovarabackend.accountactivity.settings.model.SettingsActivity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SettingsActivityRepository extends JpaRepository<SettingsActivity, Long> {

    @Query("SELECT s FROM SettingsActivity s WHERE s.userAssigned.email = :email")
    List<SettingsActivity> findByUserAssignedEmail(@Param("email") String email, Pageable pageable);

}
