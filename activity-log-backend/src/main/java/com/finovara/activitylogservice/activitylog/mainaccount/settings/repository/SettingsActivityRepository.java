package com.finovara.activitylogservice.activitylog.mainaccount.settings.repository;

import com.finovara.activitylogservice.activitylog.mainaccount.settings.dto.SettingsActivityFilterDto;
import com.finovara.activitylogservice.activitylog.mainaccount.settings.model.SettingsActivity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface SettingsActivityRepository extends JpaRepository<SettingsActivity, Long> {

    List<SettingsActivity> findByUserId(Long userId, Pageable pageable);

    @Query("""
            SELECT s FROM SettingsActivity s
            WHERE s.userId = :userId
              AND (CAST(:#{#filter.status} AS string) IS NULL OR s.status = :#{#filter.status})
              AND (CAST(:#{#filter.settingType} AS string) IS NULL OR s.settingType = :#{#filter.settingType})
              AND (CAST(:createdAt AS date) IS NULL OR CAST(s.createdAt AS date) = :createdAt)
            ORDER BY s.createdAt DESC, s.id DESC
            """)
    List<SettingsActivity> findFilteredActivities(Long userId, SettingsActivityFilterDto filter, LocalDate createdAt, Pageable pageable);

    List<SettingsActivity> findAllByUserId(Long userId);
}
