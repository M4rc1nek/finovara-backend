package com.finovara.activitylogservice.feignclient;

import com.finovara.contracts.user.authorization.dto.ConfirmPasswordDto;
import com.finovara.contracts.sharedaccount.SharedAccountMemberInfoDto;
import com.finovara.contracts.user.authorization.dto.UserDataDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.List;

@FeignClient(name = "auth-backend", url = "${auth-backend.url}")
public interface AuthBackendClient {

    @GetMapping("/internal/user-session")
    UserDataDto getUserSession(@RequestHeader("X-User-Id") Long userId);

    @PostMapping("/internal/verify-password")
    Void verifyPassword(@RequestHeader("X-User-Id") Long userId, @RequestBody ConfirmPasswordDto dto);

    @GetMapping("/internal/user-ids")
    List<Long> getAllUserIds();

    @GetMapping("/internal/shared-account/members")
    List<SharedAccountMemberInfoDto> getSharedAccountMembers(@RequestHeader("X-User-Id") Long userId);

    @GetMapping("/internal/username")
    String getUsername(@RequestHeader("X-User-Id") Long id);
}
