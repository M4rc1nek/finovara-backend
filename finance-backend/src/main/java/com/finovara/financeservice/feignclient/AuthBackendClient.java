package com.finovara.financeservice.feignclient;

import com.finovara.contracts.auth.dto.ConfirmPasswordDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "auth-backend", url = "${auth-backend.url}")
public interface AuthBackendClient {

    @PostMapping("/internal/verify-password")
    Void verifyPassword(@RequestHeader("X-User-Id") Long userId, @RequestBody ConfirmPasswordDto dto);
}
