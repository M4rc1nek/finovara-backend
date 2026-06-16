package com.finovara.activitylogservice.feignclient;

import com.finovara.contracts.auth.dto.ConfirmPasswordDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "auth-backend", url = "${auth-backend.url}")
public interface AuthBackendClient {

    @PostMapping("/internal/verify-password")
    Void verifyPassword(@RequestHeader("X-User-Id") Long userId, @RequestBody ConfirmPasswordDto dto);
}
