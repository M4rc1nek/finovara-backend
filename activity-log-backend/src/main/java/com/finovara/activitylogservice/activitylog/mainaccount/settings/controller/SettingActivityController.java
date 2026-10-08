package com.finovara.activitylogservice.activitylog.mainaccount.settings.controller;

import com.finovara.activitylogservice.activitylog.mainaccount.settings.dto.SettingsActivityDto;
import com.finovara.activitylogservice.activitylog.mainaccount.settings.service.SettingsActivityService;
import com.finovara.activitylogservice.security.SecurityUtils;
import com.finovara.contracts.util.SortType;
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
