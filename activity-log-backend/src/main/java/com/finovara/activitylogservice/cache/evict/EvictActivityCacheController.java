package com.finovara.activitylogservice.cache.evict;

import com.finovara.activitylogservice.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/shared-accounts/refresh-activity")
@RequiredArgsConstructor
public class EvictActivityCacheController {

    private final EvictActivityCacheService evictActivityCacheService;

    @PostMapping
    public ResponseEntity<Void> refreshSharedAccountActivityLog() {
        evictActivityCacheService.evictSharedAccountActivityCacheData(SecurityUtils.getCurrentUserId());
        return ResponseEntity.ok().build();
    }
}