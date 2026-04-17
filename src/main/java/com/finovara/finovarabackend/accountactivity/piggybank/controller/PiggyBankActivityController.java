package com.finovara.finovarabackend.accountactivity.piggybank.controller;

import com.finovara.finovarabackend.accountactivity.model.SortType;
import com.finovara.finovarabackend.accountactivity.piggybank.dto.PiggyBankActivityDto;
import com.finovara.finovarabackend.accountactivity.piggybank.service.PiggyBankActivityService;
import com.finovara.finovarabackend.security.SecurityUtils;
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
        return ResponseEntity.ok(piggyBankActivityService.getPiggyBankActivities(SecurityUtils.getCurrentUserEmail(), sort));
    }

}
