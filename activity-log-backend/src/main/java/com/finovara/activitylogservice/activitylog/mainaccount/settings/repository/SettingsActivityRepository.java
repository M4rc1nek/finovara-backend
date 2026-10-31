package com.finovara.activitylogservice.activitylog.mainaccount.settings.repository;

import com.finovara.activitylogservice.activitylog.mainaccount.settings.dto.SettingsActivityFilterDto;
import com.finovara.activitylogservice.activitylog.mainaccount.settings.model.SettingsActivity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SettingsActivityRepository extends JpaRepository<SettingsActivity, Long> {

    List<SettingsActivity> findByUserId(Long userId, Pageable pageable);

    @Query("""
            SELECT s FROM SettingsActivity s
            WHERE s.userId = :userId
              AND (CAST(:#{#filter.status} AS string) IS NULL OR s.status = :#{#filter.status})
              AND (CAST(:#{#filter.settingType} AS string) IS NULL OR s.settingType = :#{#filter.settingType})
              AND (CAST(:createdFrom AS timestamp) IS NULL OR s.createdAt >= :createdFrom)
              AND (CAST(:createdTo AS timestamp) IS NULL OR s.createdAt < :createdTo)
            ORDER BY s.createdAt DESC, s.id DESC
            """)
    List<SettingsActivity> findFilteredActivities( Long userId, SettingsActivityFilterDto filter, LocalDateTime createdFrom, LocalDateTime createdTo,
                                                  Pageable pageable);

    List<SettingsActivity> findAllByUserId(Long userId);
}
