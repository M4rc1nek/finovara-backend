package com.finovara.activitylogservice.activitylog.mainaccount.secure.accountchange.archive.controller;

import com.finovara.activitylogservice.activitylog.mainaccount.secure.accountchange.archive.dto.AccountChangeArchiveDto;
import com.finovara.activitylogservice.activitylog.mainaccount.secure.accountchange.archive.dto.AccountChangeArchiveFilterDto;
import com.finovara.activitylogservice.activitylog.mainaccount.secure.accountchange.archive.service.AccountChangeArchiveService;
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
@RequestMapping("/api/archive-activities/account-changes-activities")
@RequiredArgsConstructor
public class AccountChangeArchiveController {

    private final AccountChangeArchiveService accountChangeArchiveService;

    @GetMapping
    public ResponseEntity<List<AccountChangeArchiveDto>> getAccountChangeArchive(@RequestParam(defaultValue = "NEWEST") SortType sortType, @RequestParam(defaultValue = "0") @Min(0) int page) {
        return ResponseEntity.ok(accountChangeArchiveService.getAccountChangeArchive(SecurityUtils.getCurrentUserId(), sortType, page));
    }

    @GetMapping("/filter")
    public ResponseEntity<List<AccountChangeArchiveDto>> filterAccountChangeArchive(@ModelAttribute AccountChangeArchiveFilterDto filter, @RequestParam(defaultValue = "0") @Min(0) int page) {
        return ResponseEntity.ok(accountChangeArchiveService.filterAccountChangeArchive(SecurityUtils.getCurrentUserId(), filter, page));
    }

}
