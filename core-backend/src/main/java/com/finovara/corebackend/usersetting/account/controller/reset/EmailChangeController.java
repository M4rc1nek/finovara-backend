package com.finovara.corebackend.usersetting.account.controller.reset;

import com.finovara.corebackend.security.SecurityUtils;
import com.finovara.corebackend.usersetting.account.dto.AttemptsDto;
import com.finovara.corebackend.usersetting.account.dto.emailpolicy.EmailChangeConfirmDto;
import com.finovara.corebackend.usersetting.account.dto.emailpolicy.EmailChangeRequestDto;
import com.finovara.corebackend.usersetting.account.service.emailpolicy.EmailChangeService;
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
    public ResponseEntity<AttemptsDto> confirmEmailWithCode(@RequestBody @Valid EmailChangeConfirmDto dto, HttpServletRequest request) {
        return ResponseEntity.ok(emailChangeService.confirmEmailChange(SecurityUtils.getCurrentUserId(), dto, request));
    }
}