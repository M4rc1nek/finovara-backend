package com.finovara.activitylogservice.activitylog.accountactivity.settings.repository;

import com.finovara.activitylogservice.activitylog.accountactivity.settings.model.SettingsActivity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SettingsActivityRepository extends JpaRepository<SettingsActivity, Long> {

    List<SettingsActivity> findByUserId(Long userId, Pageable pageable);

    void deleteByUserId(Long userId);
}
