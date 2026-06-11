package com.finovara.activityservice.activitylog.accountactivity.piggybank.controller;

import com.finovara.activityservice.activitylog.accountactivity.piggybank.dto.PiggyBankActivityDto;
import com.finovara.activityservice.activitylog.accountactivity.piggybank.service.PiggyBankActivityService;
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
@RequestMapping("/api/account-activity/piggy-bank")
@RequiredArgsConstructor
public class PiggyBankActivityController {

    private final PiggyBankActivityService piggyBankActivityService;

    @GetMapping
    public ResponseEntity<List<PiggyBankActivityDto>> getPiggyBanksActivities(@RequestParam(defaultValue = "NEWEST") SortType sort) {
        return ResponseEntity.ok(piggyBankActivityService.getPiggyBankActivities(SecurityUtils.getCurrentUserId(), sort));
    }

}
