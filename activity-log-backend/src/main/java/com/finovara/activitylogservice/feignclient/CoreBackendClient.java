package com.finovara.activityservice.feignclient;

import com.finovara.contracts.auth.dto.ConfirmPasswordDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "core-backend", url = "${core-backend.url}")
public interface CoreBackendClient {

    @PostMapping("/internal/verify-password")
    Void verifyPassword(@RequestHeader("X-User-Id") Long userId, @RequestBody ConfirmPasswordDto dto);
}
