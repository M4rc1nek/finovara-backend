package com.finovara.financeservice.sharedaccount.deletion;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WalletCacheEvictionServiceTest {

    @Mock
    private CacheManager cacheManager;

    @Mock
    private Cache sharedWalletCache;

    @Mock
    private Cache userWalletCache;

    private WalletCacheEvictionService cacheEvictionService;

    private Long ownerId;
    private Long memberId;

    @BeforeEach
    void setUp() {
        cacheEvictionService = new WalletCacheEvictionService(cacheManager);
        ownerId = 1L;
        memberId = 2L;
    }

    @Nested
    class WhenCachesArePresent {

        @BeforeEach
        void stubCaches() {
            when(cacheManager.getCache("wallet:shared")).thenReturn(sharedWalletCache);
            when(cacheManager.getCache("wallet:user")).thenReturn(userWalletCache);
        }

        @Test
        void shouldEvictOwnerFromSharedWalletCacheWhenCachesArePresent() {
            cacheEvictionService.evictAfterSharedAccountDeletion(ownerId, memberId);

            verify(sharedWalletCache).evict(ownerId);
        }

        @Test
        void shouldEvictMemberFromSharedWalletCacheWhenCachesArePresent() {
            cacheEvictionService.evictAfterSharedAccountDeletion(ownerId, memberId);

            verify(sharedWalletCache).evict(memberId);
        }

        @Test
        void shouldEvictOwnerFromUserWalletCacheWhenCachesArePresent() {
            cacheEvictionService.evictAfterSharedAccountDeletion(ownerId, memberId);

            verify(userWalletCache).evict(ownerId);
        }

        @Test
        void shouldEvictMemberFromUserWalletCacheWhenCachesArePresent() {
            cacheEvictionService.evictAfterSharedAccountDeletion(ownerId, memberId);

            verify(userWalletCache).evict(memberId);
        }
    }

    @Nested
    class WhenCachesAreMissing {

        @Test
        void shouldNotThrowExceptionWhenSharedWalletCacheIsMissing() {
            when(cacheManager.getCache("wallet:shared")).thenReturn(null);
            when(cacheManager.getCache("wallet:user")).thenReturn(userWalletCache);

            assertDoesNotThrow(() -> cacheEvictionService.evictAfterSharedAccountDeletion(ownerId, memberId));
        }

        @Test
        void shouldNotThrowExceptionWhenBothCachesAreMissing() {
            when(cacheManager.getCache("wallet:shared")).thenReturn(null);
            when(cacheManager.getCache("wallet:user")).thenReturn(null);

            assertDoesNotThrow(() -> cacheEvictionService.evictAfterSharedAccountDeletion(ownerId, memberId));
        }

        @Test
        void shouldEvictSharedWalletCacheWhenUserWalletCacheIsMissing() {
            when(cacheManager.getCache("wallet:shared")).thenReturn(sharedWalletCache);
            when(cacheManager.getCache("wallet:user")).thenReturn(null);

            cacheEvictionService.evictAfterSharedAccountDeletion(ownerId, memberId);

            verify(sharedWalletCache).evict(ownerId);
            verify(sharedWalletCache).evict(memberId);
        }
    }

    @Nested
    class WhenCacheEvictionFails {

        @Test
        void shouldNotThrowExceptionWhenEvictThrowsException() {
            when(cacheManager.getCache("wallet:shared")).thenReturn(sharedWalletCache);
            when(cacheManager.getCache("wallet:user")).thenReturn(userWalletCache);
            doThrow(new RuntimeException("cache unavailable")).when(sharedWalletCache).evict(ownerId);

            assertDoesNotThrow(() -> cacheEvictionService.evictAfterSharedAccountDeletion(ownerId, memberId));
        }

        @Test
        void shouldContinueEvictingRemainingCachesWhenOneEvictionThrowsException() {
            when(cacheManager.getCache("wallet:shared")).thenReturn(sharedWalletCache);
            when(cacheManager.getCache("wallet:user")).thenReturn(userWalletCache);
            doThrow(new RuntimeException("cache unavailable")).when(sharedWalletCache).evict(ownerId);

            cacheEvictionService.evictAfterSharedAccountDeletion(ownerId, memberId);

            verify(sharedWalletCache).evict(memberId);
            verify(userWalletCache).evict(ownerId);
            verify(userWalletCache).evict(memberId);
        }
    }
}