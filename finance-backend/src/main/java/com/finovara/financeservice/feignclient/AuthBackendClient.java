package com.finovara.financeservice.feignclient;

import com.finovara.contracts.authorization.dto.ConfirmPasswordDto;
import com.finovara.contracts.authorization.dto.ConfirmAuthorizationCodeDto;
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

    @GetMapping("/internal/username")
    String getUsername(@RequestHeader("X-User-Id") Long id);

    @GetMapping("/internal/user-ids")
    List<Long> getAllUserIds();

    @GetMapping("/internal/email")
    String getUserEmail(@RequestHeader("X-User-Id") Long id);

}
