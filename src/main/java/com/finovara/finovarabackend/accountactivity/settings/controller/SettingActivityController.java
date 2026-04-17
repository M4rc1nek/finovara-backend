package com.finovara.finovarabackend.accountactivity.settings.controller;

import com.finovara.finovarabackend.accountactivity.model.SortType;
import com.finovara.finovarabackend.accountactivity.settings.dto.SettingsActivityDto;
import com.finovara.finovarabackend.accountactivity.settings.service.SettingsActivityService;
import com.finovara.finovarabackend.security.SecurityUtils;
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
        return ResponseEntity.ok(settingsActivityService.getSettingsActivities(SecurityUtils.getCurrentUserEmail(), sort));
    }

}
