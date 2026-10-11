package com.finovara.activitylogservice.cache.evict;

import com.finovara.contracts.cache.RedisCacheEvictor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class EvictActivityCacheServiceTest {

    private static final Long FIRST_USER_ID = 1L;
    private static final Long SECOND_USER_ID = 2L;
    private static final Long THIRD_USER_ID = 3L;

    @Mock
    private RedisCacheEvictor redisCacheEvictor;

    @InjectMocks
    private EvictActivityCacheService evictActivityCacheService;

    private ActivityCache cache;

    @BeforeEach
    void setUp() {
        cache = ActivityCache.values()[0];
    }

    @Nested
    class Evict {

        @Test
        void shouldEvictPatternWhenSingleUserIdIsProvided() {
            evictActivityCacheService.evict(cache, FIRST_USER_ID);

            verify(redisCacheEvictor).evictByPatterns(List.of(cache.patternFor(FIRST_USER_ID)));
        }

        @Test
        void shouldEvictPatternsInOrderWhenMultipleUserIdsAreProvided() {
            evictActivityCacheService.evict(cache, FIRST_USER_ID, SECOND_USER_ID, THIRD_USER_ID);

            verify(redisCacheEvictor).evictByPatterns(List.of(
                    cache.patternFor(FIRST_USER_ID),
                    cache.patternFor(SECOND_USER_ID),
                    cache.patternFor(THIRD_USER_ID)));
        }

        @Test
        void shouldSkipNullUserIdsWhenSomeUserIdsAreNull() {
            evictActivityCacheService.evict(cache, FIRST_USER_ID, null, SECOND_USER_ID);

            verify(redisCacheEvictor).evictByPatterns(List.of(
                    cache.patternFor(FIRST_USER_ID),
                    cache.patternFor(SECOND_USER_ID)));
        }

        @Test
        void shouldEvictEmptyListWhenAllUserIdsAreNull() {
            evictActivityCacheService.evict(cache, null, null);

            verify(redisCacheEvictor).evictByPatterns(List.of());
        }

        @Test
        void shouldEvictEmptyListWhenSingleNullUserIdIsProvided() {
            evictActivityCacheService.evict(cache, (Long) null);

            verify(redisCacheEvictor).evictByPatterns(List.of());
        }

        @Test
        void shouldEvictEmptyListWhenNoUserIdsAreProvided() {
            evictActivityCacheService.evict(cache);

            verify(redisCacheEvictor).evictByPatterns(List.of());
        }

        @Test
        void shouldKeepDuplicatesWhenSameUserIdIsProvidedTwice() {
            evictActivityCacheService.evict(cache, FIRST_USER_ID, FIRST_USER_ID);

            verify(redisCacheEvictor).evictByPatterns(List.of(
                    cache.patternFor(FIRST_USER_ID),
                    cache.patternFor(FIRST_USER_ID)));
        }

        @Test
        void shouldEvictOnlyOnceWhenMultipleUserIdsAreProvided() {
            evictActivityCacheService.evict(cache, FIRST_USER_ID, SECOND_USER_ID);

            verify(redisCacheEvictor).evictByPatterns(List.of(
                    cache.patternFor(FIRST_USER_ID),
                    cache.patternFor(SECOND_USER_ID)));
            verifyNoMoreInteractions(redisCacheEvictor);
        }

        @Test
        void shouldThrowExceptionWhenUserIdsArrayIsNull() {
            assertThrows(NullPointerException.class, () -> evictActivityCacheService.evict(cache, (Long[]) null));
        }

        @Test
        void shouldThrowExceptionWhenCacheIsNull() {
            assertThrows(NullPointerException.class, () -> evictActivityCacheService.evict(null, FIRST_USER_ID));
        }

        @Test
        void shouldThrowExceptionWhenCacheEvictorFails() {
            List<String> patterns = List.of(cache.patternFor(FIRST_USER_ID));
            doThrow(new IllegalStateException()).when(redisCacheEvictor).evictByPatterns(patterns);

            assertThrows(IllegalStateException.class, () -> evictActivityCacheService.evict(cache, FIRST_USER_ID));
        }
    }

    @Nested
    class EvictAllForUser {

        private List<String> expectedPatterns;

        @BeforeEach
        void setUp() {
            expectedPatterns = Arrays.stream(ActivityCache.values())
                    .map(activityCache -> activityCache.patternFor(FIRST_USER_ID))
                    .toList();
        }

        @Test
        void shouldEvictPatternsForAllCachesWhenUserIdIsProvided() {
            evictActivityCacheService.evictAllForUser(FIRST_USER_ID);

            verify(redisCacheEvictor).evictByPatterns(expectedPatterns);
        }

        @Test
        void shouldEvictExactlyOneBatchWhenUserIdIsProvided() {
            evictActivityCacheService.evictAllForUser(FIRST_USER_ID);

            verify(redisCacheEvictor).evictByPatterns(expectedPatterns);
            verifyNoMoreInteractions(redisCacheEvictor);
        }

        @Test
        void shouldEvictPatternsForEveryCacheValueWhenUserIdIsProvided() {
            evictActivityCacheService.evictAllForUser(SECOND_USER_ID);

            List<String> patterns = Arrays.stream(ActivityCache.values())
                    .map(activityCache -> activityCache.patternFor(SECOND_USER_ID))
                    .toList();
            verify(redisCacheEvictor).evictByPatterns(patterns);
        }

        @Test
        void shouldThrowExceptionWhenCacheEvictorFails() {
            doThrow(new IllegalStateException()).when(redisCacheEvictor).evictByPatterns(expectedPatterns);

            assertThrows(IllegalStateException.class, () -> evictActivityCacheService.evictAllForUser(FIRST_USER_ID));
        }
    }
}