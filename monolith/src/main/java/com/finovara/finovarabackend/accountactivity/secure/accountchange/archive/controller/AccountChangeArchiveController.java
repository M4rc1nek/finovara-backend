package com.finovara.finovarabackend.accountactivity.secure.accountchange.archive.controller;

import com.finovara.finovarabackend.accountactivity.secure.accountchange.archive.dto.AccountChangeArchiveDto;
import com.finovara.finovarabackend.accountactivity.secure.accountchange.archive.service.AccountChangeArchiveService;
import com.finovara.finovarabackend.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/archive-activities/account-changes-activities")
@RequiredArgsConstructor
public class AccountChangeArchiveController {

    private final AccountChangeArchiveService accountChangeArchiveService;

    @GetMapping
    public ResponseEntity<List<AccountChangeArchiveDto>> getAccountChangeArchive() {
        return ResponseEntity.ok(accountChangeArchiveService.getAccountChangeArchive(SecurityUtils.getCurrentUserId()));
    }

}
