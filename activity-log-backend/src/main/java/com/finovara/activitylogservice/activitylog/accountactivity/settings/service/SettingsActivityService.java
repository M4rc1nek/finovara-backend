package com.finovara.activitylogservice.activitylog.accountactivity.settings.service;

import com.finovara.activitylogservice.activitylog.accountactivity.core.AccountActivityCore;
import com.finovara.activitylogservice.activitylog.accountactivity.settings.dto.SettingsActivityDto;
import com.finovara.activitylogservice.activitylog.accountactivity.settings.mapper.SettingsActivityMapper;
import com.finovara.activitylogservice.activitylog.accountactivity.settings.model.SettingsActivity;
import com.finovara.activitylogservice.activitylog.accountactivity.settings.repository.SettingsActivityRepository;
import com.finovara.contracts.datadeletable.UserDataDeletable;
import com.finovara.contracts.activity.event.settings.SettingsActivityEvent;
import com.finovara.contracts.model.SortType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SettingsActivityService extends AccountActivityCore<SettingsActivity, SettingsActivityDto> implements UserDataDeletable {

    @Value("${user-activity.settings.page-size}")
    private int pageSize;

    private final SettingsActivityRepository settingsActivityRepository;
    private final SettingsActivityMapper settingsActivityMapper;

    @Transactional
    public void handleEvent(SettingsActivityEvent event) {
        SettingsActivity settingsActivity = SettingsActivity.builder()
                .userId(event.userId())
                .settingType(event.settingType())
                .status(event.status())
                .createdAt(event.occurredAt())
                .build();

        settingsActivityRepository.save(settingsActivity);
    }

    public List<SettingsActivityDto> getSettingsActivities(Long userId, SortType sort) {
        return getActivities(userId, sort, pageSize);
    }

    @Override
    protected List<SettingsActivity> getRepositoryFindByUserId(Long userId, Pageable pageable) {
        return settingsActivityRepository.findByUserId(userId, pageable);
    }

    @Override
    protected SettingsActivityDto mapToDto(SettingsActivity entity) {
        return settingsActivityMapper.mapToSettingActivity(entity);
    }

    @Override
    @Transactional
    public void deleteByUserId(Long userId) {
        settingsActivityRepository.deleteByUserId(userId);
        log.info("Deleted settings activity for userId={}", userId);
    }
}
