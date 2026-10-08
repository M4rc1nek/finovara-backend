package com.finovara.activitylogservice.activitylog.sharedaccount.service;

import com.finovara.activitylogservice.activitylog.sharedaccount.dto.SharedAccountActivityLogDto;
import com.finovara.activitylogservice.activitylog.sharedaccount.mapper.SharedAccountActivityLogMapper;
import com.finovara.activitylogservice.activitylog.sharedaccount.model.SharedAccountActivityLog;
import com.finovara.activitylogservice.activitylog.sharedaccount.repository.SharedAccountActivityLogRepository;
import com.finovara.activitylogservice.feignclient.AuthBackendClient;
import com.finovara.contracts.sharedaccount.event.activity.SharedAccountActivityLogEvent;
import com.finovara.contracts.user.authorization.dto.UserDataDto;
import com.finovara.contracts.user.datadeletable.UserDataDeletable;
import com.finovara.contracts.util.SortType;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class SharedAccountActivityLogService implements UserDataDeletable {

    @Value("${shared-account.activity.page-size}")
    private int pageSize;

    private final SharedAccountActivityLogRepository sharedAccountActivityLogRepository;
    private final SharedAccountActivityLogMapper sharedAccountActivityLogMapper;
    private final AuthBackendClient authBackendClient;

    @Transactional
    public void createSharedAccountActivityLog(SharedAccountActivityLogEvent event) {
        SharedAccountActivityLog activity = SharedAccountActivityLog.builder()
                .ownerId(event.ownerId())
                .memberId(event.memberId())
                .userId(event.userId())
                .targetId(event.targetId())
                .activityType(event.type())
                .createdAt(event.createdAt())
                .build();

        sharedAccountActivityLogRepository.save(activity);
        log.info("Saved shared account activity for user {}, type: {}", event.userId(), event.type());
    }

    @Cacheable(value = "shared-account:activity", key = "#userId + ':' + #sortType")
    public List<SharedAccountActivityLogDto> getSharedAccountLogActivity(Long userId, SortType sortType) {
        List<SharedAccountActivityLog> activities =
                sharedAccountActivityLogRepository.findByOwnerIdOrMemberId(userId, sortType.getPageable(pageSize));

        Map<Long, UserDataDto> users = new HashMap<>();
        activities.stream()
                .map(SharedAccountActivityLog::getUserId)
                .distinct()
                .forEach(id -> users.put(id, resolveUserInfo(id)));

        return activities.stream()
                .map(activity -> sharedAccountActivityLogMapper.mapToDto(activity, users.get(activity.getUserId())))
                .toList();
    }

    private UserDataDto resolveUserInfo(Long userId) {
        try {
            return authBackendClient.getUserSession(userId);
        } catch (FeignException e) {
            log.warn("Could not load user data for userId={}, status={}", userId, e.status());
            return null;
        }
    }

    @Override
    @Transactional
    public void deleteByUserId(Long userId) {
        sharedAccountActivityLogRepository.deleteByUserId(userId);
        log.info("Deleted shared account activity log for userId={}", userId);
    }
}