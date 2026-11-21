package com.finovara.notificationservice.feignclient;

import com.finovara.contracts.user.authorization.dto.UserDataResponse;
import com.finovara.contracts.user.authorization.dto.UserDataDto;
import com.finovara.contracts.user.authorization.dto.ConfirmAuthorizationCodeDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.List;


@FeignClient(name = "auth-backend", url = "${auth-backend.url}")
public interface AuthBackendClient {

    @GetMapping("/internal/user-data")
    UserDataResponse getUserData(@RequestHeader("X-User-Id") Long userId);

    @PostMapping("/internal/user-sessions")
    List<UserDataDto> getUserSessions(@RequestBody List<Long> userIds);

    @PostMapping("/internal/confirm-authorization-code")
    Void confirmAuthorizationCode(@RequestHeader("X-User-Id") Long userId, @RequestBody ConfirmAuthorizationCodeDto dto);
}