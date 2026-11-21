package com.finovara.activitylogservice.activitylog.util;

import com.finovara.activitylogservice.feignclient.AuthBackendClient;
import com.finovara.contracts.user.authorization.dto.UserDataDto;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@Slf4j
@RequiredArgsConstructor
public class UserSessionResolver {
    private static final String USER_INFO_CACHE = "shared-account:user-info";

    private final AuthBackendClient authBackendClient;
    private final CacheManager cacheManager;

    @Cacheable(value = USER_INFO_CACHE, key = "#userId", unless = "#result == null")
    public UserDataDto getUserInfo(Long userId) {
        try {
            return authBackendClient.getUserSession(userId);
        } catch (FeignException exception) {
            log.warn("Could not load user data for userId={}, status={}", userId, exception.status());
            return null;
        }
    }

    public Map<Long, UserDataDto> getUserInfoByIds(List<Long> userIds) {
        Cache cache = cacheManager.getCache(USER_INFO_CACHE);
        if (cache == null) {
            throw new IllegalStateException("Cache is not configured: " + USER_INFO_CACHE);
        }

        Map<Long, UserDataDto> usersById = new LinkedHashMap<>();
        List<Long> missingUserIds = new ArrayList<>();
        for (Long userId : new LinkedHashSet<>(userIds)) {
            UserDataDto cached = cache.get(userId, UserDataDto.class);
            if (cached == null) {
                missingUserIds.add(userId);
            } else {
                usersById.put(userId, cached);
            }
        }

        if (missingUserIds.isEmpty()) {
            return usersById;
        }

        try {
            for (UserDataDto user : authBackendClient.getUserSessions(missingUserIds)) {
                usersById.put(user.id(), user);
                cache.put(user.id(), user);
            }
        } catch (FeignException e) {
            log.warn("Could not load user data for userIds={}, status={}", missingUserIds, e.status());
        }
        return usersById;
    }

    @CacheEvict(value = USER_INFO_CACHE, key = "#userId")
    public void evictUserInfo(Long userId) {
        log.info("Evicted shared-account:user-info cache for userId={}", userId);
    }
}
