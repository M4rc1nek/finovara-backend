package com.finovara.finovarabackend.accountactivity.settings.service;

import com.finovara.finovarabackend.accountactivity.settings.dto.SettingsActivityDto;
import com.finovara.finovarabackend.accountactivity.settings.mapper.SettingsActivityMapper;
import com.finovara.finovarabackend.accountactivity.settings.model.SettingActivitySort;
import com.finovara.finovarabackend.accountactivity.settings.model.SettingActivityStatus;
import com.finovara.finovarabackend.accountactivity.settings.model.SettingType;
import com.finovara.finovarabackend.accountactivity.settings.model.SettingsActivity;
import com.finovara.finovarabackend.accountactivity.settings.repository.SettingsActivityRepository;
import com.finovara.finovarabackend.config.TimeConfig;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.util.service.user.service.UserManagerService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SettingsActivityService {

    @Value("${user-activity.settings.page-size}")
    private int pageSize;

    private final UserManagerService userManagerService;
    private final SettingsActivityRepository settingsActivityRepository;
    private final SettingsActivityMapper settingsActivityMapper;
    private final TimeConfig timeConfig;


    @Transactional
    public void createSettingActivity(String email, SettingActivityStatus status, SettingType type) {
        User user = userManagerService.getUserByEmailOrThrow(email);

        SettingsActivity settingsActivity = SettingsActivity.builder()
                .userAssigned(user)
                .status(status)
                .settingType(type)
                .date(LocalDateTime.now(timeConfig.clock()))
                .build();

        settingsActivityRepository.save(settingsActivity);
    }

    public List<SettingsActivityDto> getSettingsActivities(String email, SettingActivitySort sort) {
        Pageable pageable = switch (sort) {
            case NEWEST -> PageRequest.of(0, pageSize, Sort.by("date").descending());
            case OLDEST -> PageRequest.of(0, pageSize, Sort.by("date").ascending());
        };

        return settingsActivityRepository.findByUserAssignedEmail(email, pageable)
                .stream().map(settingsActivityMapper::mapToSettingActivity)
                .toList();

    }

}
