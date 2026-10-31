package com.finovara.activitylogservice.activitylog.util;

import com.finovara.activitylogservice.feignclient.AuthBackendClient;
import com.finovara.contracts.user.authorization.dto.UserDataDto;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class UserSessionResolver {
    private final AuthBackendClient authBackendClient;

    @Cacheable(value = "user-info", key = "#userId", unless = "#result == null")
    public UserDataDto getUserInfo(Long userId) {
        try {
            return authBackendClient.getUserSession(userId);
        } catch (FeignException exception) {
            log.warn("Could not load user data for userId={}, status={}", userId, exception.status());
            return null;
        }
    }
}
