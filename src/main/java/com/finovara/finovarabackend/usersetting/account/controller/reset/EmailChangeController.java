package com.finovara.finovarabackend.usersetting.account.controller.reset;

import com.finovara.finovarabackend.security.SecurityUtils;
import com.finovara.finovarabackend.usersetting.account.dto.emailpolicy.EmailChangeConfirmDto;
import com.finovara.finovarabackend.usersetting.account.dto.emailpolicy.EmailChangeRequestDto;
import com.finovara.finovarabackend.usersetting.account.service.emailpolicy.EmailChangeService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/email-change")
@RequiredArgsConstructor
public class EmailChangeController {

    private final EmailChangeService emailChangeService;

    @PostMapping
    public ResponseEntity<Void> requestEmailChange(@RequestBody @Valid EmailChangeRequestDto dto) {
        emailChangeService.requestEmailChange(SecurityUtils.getCurrentUserId(), dto);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/confirm")
    public ResponseEntity<Void> confirmEmailWithCode(@RequestBody @Valid EmailChangeConfirmDto dto, HttpServletRequest request) {
        emailChangeService.confirmEmailChange(SecurityUtils.getCurrentUserId(), dto, request);
        return ResponseEntity.noContent().build();
    }
}