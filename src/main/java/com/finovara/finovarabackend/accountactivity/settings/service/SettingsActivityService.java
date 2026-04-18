package com.finovara.finovarabackend.accountactivity.settings.service;

import com.finovara.finovarabackend.accountactivity.core.AccountActivityCore;
import com.finovara.finovarabackend.util.model.SortType;
import com.finovara.finovarabackend.accountactivity.settings.dto.SettingsActivityDto;
import com.finovara.finovarabackend.accountactivity.settings.mapper.SettingsActivityMapper;
import com.finovara.finovarabackend.accountactivity.settings.model.SettingActivityStatus;
import com.finovara.finovarabackend.accountactivity.settings.model.SettingType;
import com.finovara.finovarabackend.accountactivity.settings.model.SettingsActivity;
import com.finovara.finovarabackend.accountactivity.settings.repository.SettingsActivityRepository;
import com.finovara.finovarabackend.util.user.service.UserManagerService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class SettingsActivityService extends AccountActivityCore<SettingsActivity, SettingsActivityDto, SettingType> {

    @Value("${user-activity.settings.page-size}")
    private int pageSize;

    private final SettingsActivityRepository settingsActivityRepository;
    private final SettingsActivityMapper settingsActivityMapper;

    public SettingsActivityService(UserManagerService userManagerService,
                                   SettingsActivityRepository settingsActivityRepository,
                                   SettingsActivityMapper settingsActivityMapper) {
        super(userManagerService);
        this.settingsActivityRepository = settingsActivityRepository;
        this.settingsActivityMapper = settingsActivityMapper;
    }


    @Transactional
    public void createSettingActivity(String email, SettingActivityStatus status, SettingType type) {
        SettingsActivity settingsActivity = buildActivity(email, type);
        settingsActivity.setStatus(status);
        settingsActivityRepository.save(settingsActivity);
    }

    public List<SettingsActivityDto> getSettingsActivities(String email, SortType sort) {
        return getActivities(email, sort, pageSize);
    }

    @Override
    protected List<SettingsActivity> getRepositoryFindByUserEmail(String email, Pageable pageable) {
        return settingsActivityRepository.findByUserAssignedEmail(email, pageable);
    }

    @Override
    protected SettingsActivityDto mapToDto(SettingsActivity entity) {
        return settingsActivityMapper.mapToSettingActivity(entity);
    }

    @Override
    protected SettingsActivity buildActivity(String email, SettingType type) {
        return SettingsActivity.builder()
                .userAssigned(getUser(email))
                .settingType(type)
                .createdAt(LocalDateTime.now())
                .build();

    }

}
