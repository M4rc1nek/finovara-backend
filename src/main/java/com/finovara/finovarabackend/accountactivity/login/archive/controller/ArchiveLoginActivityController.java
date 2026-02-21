package com.finovara.finovarabackend.accountactivity.login.archive.controller;

import com.finovara.finovarabackend.accountactivity.login.archive.dto.ArchiveLoginActivityDto;
import com.finovara.finovarabackend.accountactivity.login.archive.service.ArchiveLoginActivityService;
import com.finovara.finovarabackend.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/archive-activities/login-activities")
@RequiredArgsConstructor
public class ArchiveLoginActivityController {

    private final ArchiveLoginActivityService archiveLoginActivityService;

    @GetMapping
    public ResponseEntity<List<ArchiveLoginActivityDto>> getArchiveLoginActivity() {
        return ResponseEntity.ok(archiveLoginActivityService.getArchiveLoginActivity(SecurityUtils.getCurrentUserEmail()));
    }

}
