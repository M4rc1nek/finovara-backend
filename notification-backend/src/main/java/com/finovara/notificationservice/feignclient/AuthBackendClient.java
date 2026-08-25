package com.finovara.notificationservice.feignclient;

import com.finovara.contracts.authorization.dto.UserDataResponse;
import com.finovara.contracts.authorization.dto.ConfirmAuthorizationCodeDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;


@FeignClient(name = "auth-backend", url = "${auth-backend.url}")
public interface AuthBackendClient {

    @GetMapping("/internal/user-data")
    UserDataResponse getUserData(@RequestHeader("X-User-Id") Long userId);

    @PostMapping("/internal/confirm-authorization-code")
    Void confirmAuthorizationCode(@RequestHeader("X-User-Id") Long userId, @RequestBody ConfirmAuthorizationCodeDto dto);
}