package com.finovara.activitylogservice.activitylog.sharedaccount.controller;

import com.finovara.activitylogservice.activitylog.sharedaccount.dto.SharedAccountActivityLogDto;
import com.finovara.activitylogservice.activitylog.sharedaccount.dto.SharedAccountActivityLogFilterDto;
import com.finovara.activitylogservice.activitylog.sharedaccount.service.SharedAccountActivityLogService;
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
@RequestMapping("/api/shared-accounts/activity")
@RequiredArgsConstructor
public class SharedAccountActivityLogController {

    private final SharedAccountActivityLogService sharedAccountActivityLogService;

    @GetMapping
    public ResponseEntity<List<SharedAccountActivityLogDto>> getSharedAccountActivityLog(@RequestParam(defaultValue = "NEWEST") SortType sort, @RequestParam(defaultValue = "0") @Min(0) int page) {
        return ResponseEntity.ok(sharedAccountActivityLogService.getSharedAccountLogActivity(SecurityUtils.getCurrentUserId(), sort, page));
    }

    @GetMapping("/filter")
    public ResponseEntity<List<SharedAccountActivityLogDto>> filterSharedAccountActivityLog(@ModelAttribute SharedAccountActivityLogFilterDto filter, @RequestParam(defaultValue = "0") @Min(0) int page) {
        return ResponseEntity.ok(sharedAccountActivityLogService.filterSharedAccountLogActivity(SecurityUtils.getCurrentUserId(), filter, page));
    }

}
