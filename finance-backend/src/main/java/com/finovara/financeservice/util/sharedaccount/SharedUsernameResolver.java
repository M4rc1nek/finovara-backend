package com.finovara.financeservice.util.sharedaccount;

import com.finovara.financeservice.feignclient.AuthBackendClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class SharedUsernameResolver {

    private final AuthBackendClient authBackendClient;

    public Map<Long, String> resolve(Collection<Long> userIds) {
        Map<Long, String> result = new HashMap<>();
        for (Long id : new LinkedHashSet<>(userIds)) {
            result.put(id, authBackendClient.getUsername(id));
        }
        return result;
    }
}