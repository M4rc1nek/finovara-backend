package com.finovara.financeservice.sharedaccount.deletion;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class WalletCacheEvictionService {

    private static final String SHARED_WALLET_CACHE = "wallet:shared";
    private static final String USER_WALLET_CACHE = "wallet:user";

    private final CacheManager cacheManager;

    public void evictAfterSharedAccountDeletion(Long ownerId, Long memberId) {
        evictIfPresent(SHARED_WALLET_CACHE, ownerId);
        evictIfPresent(SHARED_WALLET_CACHE, memberId);
        evictIfPresent(USER_WALLET_CACHE, ownerId);
        evictIfPresent(USER_WALLET_CACHE, memberId);
    }

    private void evictIfPresent(String cacheName, Object key) {
        try {
            var cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.evict(key);
            }
        } catch (Exception ex) {
            log.warn("Cache eviction failed for cache={}, key={} — entry may be stale until TTL expiry.",
                    cacheName, key, ex);
        }
    }
}