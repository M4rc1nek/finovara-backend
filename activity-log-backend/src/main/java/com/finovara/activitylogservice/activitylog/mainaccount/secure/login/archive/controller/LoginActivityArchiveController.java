package com.finovara.activitylogservice.activitylog.mainaccount.secure.login.archive.controller;

import com.finovara.activitylogservice.activitylog.mainaccount.secure.login.archive.dto.LoginActivityArchiveDto;
import com.finovara.activitylogservice.activitylog.mainaccount.secure.login.archive.dto.LoginActivityArchiveFilterDto;
import com.finovara.activitylogservice.activitylog.mainaccount.secure.login.archive.service.LoginActivityArchiveService;
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
@RequestMapping("/api/archive-activities/login-activities")
@RequiredArgsConstructor
public class LoginActivityArchiveController {

    private final LoginActivityArchiveService archiveLoginActivityService;

    @GetMapping
    public ResponseEntity<List<LoginActivityArchiveDto>> getLoginActivityArchive(@RequestParam(defaultValue = "NEWEST") SortType sortType, @RequestParam(defaultValue = "0") @Min(0) int page) {
        return ResponseEntity.ok(archiveLoginActivityService.getLoginActivityArchive(SecurityUtils.getCurrentUserId(), sortType, page));
    }

    @GetMapping("/filter")
    public ResponseEntity<List<LoginActivityArchiveDto>> filterLoginActivityArchive(@ModelAttribute LoginActivityArchiveFilterDto filter, @RequestParam(defaultValue = "0") @Min(0) int page) {
        return ResponseEntity.ok(archiveLoginActivityService.filterLoginActivityArchive(SecurityUtils.getCurrentUserId(), filter, page));
    }

}
