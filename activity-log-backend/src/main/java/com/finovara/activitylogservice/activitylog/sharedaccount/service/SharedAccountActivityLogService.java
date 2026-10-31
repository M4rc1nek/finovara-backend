package com.finovara.activitylogservice.activitylog.sharedaccount.service;

import com.finovara.activitylogservice.activitylog.mainaccount.core.ActivityPageRequest;
import com.finovara.activitylogservice.activitylog.mainaccount.core.DateRange;
import com.finovara.activitylogservice.activitylog.sharedaccount.dto.SharedAccountActivityLogDto;
import com.finovara.activitylogservice.activitylog.sharedaccount.dto.SharedAccountActivityLogFilterDto;
import com.finovara.activitylogservice.activitylog.sharedaccount.mapper.SharedAccountActivityLogMapper;
import com.finovara.activitylogservice.activitylog.sharedaccount.model.SharedAccountActivityLog;
import com.finovara.activitylogservice.activitylog.sharedaccount.repository.SharedAccountActivityLogRepository;
import com.finovara.activitylogservice.activitylog.util.UserSessionResolver;
import com.finovara.activitylogservice.feignclient.AuthBackendClient;
import com.finovara.contracts.sharedaccount.SharedAccountMemberInfoDto;
import com.finovara.contracts.sharedaccount.event.activity.SharedAccountActivityLogEvent;
import com.finovara.contracts.user.authorization.dto.UserDataDto;
import com.finovara.contracts.user.datadeletable.UserDataDeletable;
import com.finovara.contracts.util.SortType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
    private final UserSessionResolver userSessionResolver;
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

    @Cacheable(value = "shared-account:activity", key = "#userId + ':' + #sortType + ':' + #page")
    public List<SharedAccountActivityLogDto> getSharedAccountLogActivity(Long userId, SortType sortType, int page) {
        List<SharedAccountActivityLog> activities = sharedAccountActivityLogRepository
                .findAllByOwnerIdOrMemberId(userId, ActivityPageRequest.of(sortType, page, pageSize));

        Map<Long, UserDataDto> users = new HashMap<>();
        activities.stream()
                .map(SharedAccountActivityLog::getUserId)
                .distinct()
                .forEach(activityUserId -> users.put(activityUserId, userSessionResolver.getUserInfo(activityUserId)));

        return activities.stream()
                .map(activity -> sharedAccountActivityLogMapper.mapToDto(activity, users.get(activity.getUserId())))
                .toList();
    }

    public List<SharedAccountActivityLogDto> filterSharedAccountLogActivity(Long userId, SharedAccountActivityLogFilterDto filter, int page) {
        Pageable pageable = PageRequest.of(page, pageSize, Sort.unsorted());
        DateRange created = DateRange.of(filter.createdAt());

        Long actorId = filter.userId();

        if (filter.role() != null) {
            actorId = authBackendClient.getSharedAccountMembers(userId).stream()
                    .filter(m -> m.role() == filter.role())
                    .map(SharedAccountMemberInfoDto::userId)
                    .findFirst()
                    .orElse(null);

            if (actorId == null) {
                return List.of();
            }
        }

        List<SharedAccountActivityLog> activities = sharedAccountActivityLogRepository.findFilteredActivities(userId, filter, actorId, created.from(), created.to(), pageable);

        Map<Long, UserDataDto> users = new HashMap<>();
        activities.stream()
                .map(SharedAccountActivityLog::getUserId)
                .distinct()
                .forEach(id -> users.put(id, userSessionResolver.getUserInfo(id)));

        return activities.stream()
                .map(a -> sharedAccountActivityLogMapper.mapToDto(a, users.get(a.getUserId())))
                .toList();
    }

    @Override
    @Transactional
    public void deleteByUserId(Long userId) {
        List<SharedAccountActivityLog> activities = sharedAccountActivityLogRepository.findAllByUserId(userId);
        sharedAccountActivityLogRepository.deleteAll(activities);
        log.info("Deleted shared account activity log for userId={}", userId);
    }
}