package com.finovara.financeservice.util.sharedaccount;

import com.finovara.financeservice.feignclient.AuthBackendClient;
import com.finovara.contracts.user.authorization.dto.UserDataDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class SharedUsernameResolver {

    private final AuthBackendClient authBackendClient;

    public Map<Long, String> resolve(Collection<Long> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }

        List<Long> distinctUserIds = new LinkedHashSet<>(userIds).stream().toList();
        return authBackendClient.getUserSessions(distinctUserIds).stream()
                .collect(Collectors.toMap(UserDataDto::id, UserDataDto::username));
    }
}