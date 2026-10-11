package com.finovara.activitylogservice.activitylog.mainaccount.secure.accountchange.archive.controller;

import com.finovara.activitylogservice.activitylog.mainaccount.secure.accountchange.archive.dto.AccountChangeArchiveDto;
import com.finovara.activitylogservice.activitylog.mainaccount.secure.accountchange.archive.service.AccountChangeArchiveService;
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
@RequestMapping("/api/archive-activities/account-changes-activities")
@RequiredArgsConstructor
public class AccountChangeArchiveController {

    private final AccountChangeArchiveService accountChangeArchiveService;

    @GetMapping
    public ResponseEntity<List<AccountChangeArchiveDto>> getAccountChangeArchive(@RequestParam(defaultValue = "NEWEST") SortType sortType) {
        return ResponseEntity.ok(accountChangeArchiveService.getAccountChangeArchive(SecurityUtils.getCurrentUserId(), sortType));
    }

}
