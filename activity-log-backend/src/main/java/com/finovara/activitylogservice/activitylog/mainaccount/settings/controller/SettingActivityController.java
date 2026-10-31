package com.finovara.activitylogservice.activitylog.mainaccount.settings.controller;

import com.finovara.activitylogservice.activitylog.mainaccount.settings.dto.SettingsActivityDto;
import com.finovara.activitylogservice.activitylog.mainaccount.settings.dto.SettingsActivityFilterDto;
import com.finovara.activitylogservice.activitylog.mainaccount.settings.service.SettingsActivityService;
import com.finovara.activitylogservice.security.SecurityUtils;
import com.finovara.contracts.util.SortType;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/account-activity/settings")
@RequiredArgsConstructor
public class SettingActivityController {

    private final SettingsActivityService settingsActivityService;

    @GetMapping
    public ResponseEntity<List<SettingsActivityDto>> getSettingsActivities(@RequestParam(defaultValue = "NEWEST") SortType sort, @RequestParam(defaultValue = "0") @Min(0) int page) {
        return ResponseEntity.ok(settingsActivityService.getSettingsActivities(SecurityUtils.getCurrentUserId(), sort, page));
    }

    @GetMapping("/filter")
    public ResponseEntity<List<SettingsActivityDto>> filterSettingsActivity(@ModelAttribute SettingsActivityFilterDto filter, @RequestParam(defaultValue = "0") @Min(0) int page) {
        return ResponseEntity.ok(settingsActivityService.filterSettingsActivity(SecurityUtils.getCurrentUserId(), filter, page));
    }

}
