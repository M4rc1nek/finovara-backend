package com.finovara.finovarabackend.accountactivity.accountchanges.controller;

import com.finovara.finovarabackend.accountactivity.accountchanges.dto.UserActivityAccountChangesDto;
import com.finovara.finovarabackend.accountactivity.accountchanges.service.UserActivityAccountChangesService;
import com.finovara.finovarabackend.security.SecurityUtils;
import com.finovara.finovarabackend.util.confirmationpassword.dto.ConfirmPasswordDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/account-activity/account-changes")
@RequiredArgsConstructor
public class UserActivityAccountChangesController {

    private final UserActivityAccountChangesService userActivityAccountChangesService;

    @GetMapping
    public ResponseEntity<List<UserActivityAccountChangesDto>> getUserActivityAccountChanges() {
        return ResponseEntity.ok(userActivityAccountChangesService.getUserActivityAccountChanges(SecurityUtils.getCurrentUserEmail()));
    }

    @PostMapping
    public ResponseEntity<Void> confirmPasswordToAccountChangesActivity(@RequestBody ConfirmPasswordDto confirmPasswordDto) {
        userActivityAccountChangesService.confirmPasswordToUserActivityLogin(SecurityUtils.getCurrentUserEmail(), confirmPasswordDto);
        return ResponseEntity.noContent().build();
    }
}
