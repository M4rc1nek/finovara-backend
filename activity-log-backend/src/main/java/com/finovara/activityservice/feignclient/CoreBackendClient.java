package com.finovara.activityservice.feignclient;

import com.finovara.contracts.dto.ConfirmPasswordDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "core-backend", url = "${core-backend.url}")
public interface CoreBackendClient {

    @PostMapping("/internal/verify-password")
    void verifyPassword(@RequestHeader("X-User-Id") Long userId, @RequestBody ConfirmPasswordDto dto);
}
