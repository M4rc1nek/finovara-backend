package com.finovara.notificationservice.feignclient;

import com.finovara.contracts.auth.dto.UserDataResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "auth-backend", url = "${auth-backend.url}")
public interface AuthBackendClient {

    @GetMapping("internal/user-data")
    UserDataResponse getUserEmailData(@RequestHeader("X-User-Id") Long userId);
}