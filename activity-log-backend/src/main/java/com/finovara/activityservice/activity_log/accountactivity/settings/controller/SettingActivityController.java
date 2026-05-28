package com.finovara.activityservice.activity_log.accountactivity.settings.controller;

import com.finovara.activityservice.activity_log.accountactivity.settings.dto.SettingsActivityDto;
import com.finovara.activityservice.activity_log.accountactivity.settings.service.SettingsActivityService;
import com.finovara.activityservice.security.SecurityUtils;
import com.finovara.contracts.model.SortType;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/account-activity/settings")
@RequiredArgsConstructor
public class SettingActivityController {

    private final SettingsActivityService settingsActivityService;

    @GetMapping
    public ResponseEntity<List<SettingsActivityDto>> getSettingsActivities(@RequestParam(defaultValue = "NEWEST") SortType sort){
        return ResponseEntity.ok(settingsActivityService.getSettingsActivities(SecurityUtils.getCurrentUserId(), sort));
    }

}
