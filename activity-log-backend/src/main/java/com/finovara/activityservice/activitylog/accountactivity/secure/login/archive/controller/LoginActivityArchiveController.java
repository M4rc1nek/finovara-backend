package com.finovara.activityservice.activitylog.accountactivity.secure.login.archive.controller;

import com.finovara.activityservice.activitylog.accountactivity.secure.login.archive.dto.LoginActivityArchiveDto;
import com.finovara.activityservice.activitylog.accountactivity.secure.login.archive.service.LoginActivityArchiveService;
import com.finovara.activityservice.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/archive-activities/login-activities")
@RequiredArgsConstructor
public class LoginActivityArchiveController {

    private final LoginActivityArchiveService archiveLoginActivityService;

    @GetMapping
    public ResponseEntity<List<LoginActivityArchiveDto>> getLoginActivityArchive() {
        return ResponseEntity.ok(archiveLoginActivityService.getLoginActivityArchive(SecurityUtils.getCurrentUserId()));
    }

}
