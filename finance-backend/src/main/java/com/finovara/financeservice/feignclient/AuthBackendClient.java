package com.finovara.financeservice.feignclient;

import com.finovara.contracts.user.authorization.dto.ConfirmPasswordDto;
import com.finovara.contracts.user.authorization.dto.ConfirmAuthorizationCodeDto;
import com.finovara.contracts.user.authorization.dto.UserDataDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.List;

@FeignClient(name = "auth-backend", url = "${auth-backend.url}")
public interface AuthBackendClient {

    @PostMapping("/internal/verify-password")
    Void verifyPassword(@RequestHeader("X-User-Id") Long userId, @RequestBody ConfirmPasswordDto dto);

    @PostMapping("/internal/confirm-authorization-code")
    Void confirmAuthorizationCode(@RequestHeader("X-User-Id") Long userId, @RequestBody ConfirmAuthorizationCodeDto dto);

    @PostMapping("/internal/user-sessions")
    List<UserDataDto> getUserSessions(@RequestBody List<Long> userIds);

    @GetMapping("/internal/user-ids")
    List<Long> getAllUserIds();

    @GetMapping("/internal/email")
    String getUserEmail(@RequestHeader("X-User-Id") Long id);

}
