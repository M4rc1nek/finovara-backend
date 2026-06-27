package com.finovara.contracts.cache;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;

import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RedisCacheEvictorTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private Cursor<String> cursor;

    @InjectMocks
    private RedisCacheEvictor redisCacheEvictor;

    @BeforeEach
    void setUp() {
        when(redisTemplate.scan(any(ScanOptions.class))).thenReturn(cursor);
    }

    @Nested
    class EvictByPattern {

        @Test
        void shouldDeleteAllMatchingKeys() {
            List<String> keys = List.of("expense::123:1", "expense::123:2");

            doAnswer(inv -> {
                keys.forEach(inv.<Consumer<String>>getArgument(0));
                return null;
            }).when(cursor).forEachRemaining(any(Consumer.class));

            redisCacheEvictor.evictByPattern("expense::*123:*");

            verify(redisTemplate).delete("expense::123:1");
            verify(redisTemplate).delete("expense::123:2");
        }

        @Test
        void shouldNotDeleteAnythingWhenNoKeysMatch() {
            redisCacheEvictor.evictByPattern("nonexistent:*");

            verify(redisTemplate, never()).delete(anyString());
        }

        @Test
        void shouldCloseCursorAfterSuccessfulScan() {
            redisCacheEvictor.evictByPattern("some:pattern:*");

            verify(cursor).close();
        }

        @Test
        void shouldCloseCursorEvenWhenExceptionIsThrown() {
            doThrow(new RuntimeException("Redis down"))
                    .when(cursor).forEachRemaining(any(Consumer.class));

            assertThatThrownBy(() -> redisCacheEvictor.evictByPattern("some:pattern:*"))
                    .isInstanceOf(IllegalStateException.class);

            verify(cursor).close();
        }

        @Test
        void shouldThrowExceptionWithPatternInMessageWhenScanFails() {
            String pattern = "report:*::99";

            doThrow(new RuntimeException("Redis down")).when(cursor).forEachRemaining(any(Consumer.class));

            assertThrows(IllegalStateException.class, () -> redisCacheEvictor.evictByPattern(pattern));
        }
    }

    @Nested
    class EvictByPatterns {

        @Test
        void shouldCallScanForEachPattern() {
            List<String> patterns = List.of("report:*:123*", "report:*::123");

            redisCacheEvictor.evictByPatterns(patterns);

            verify(redisTemplate, times(patterns.size())).scan(any(ScanOptions.class));
        }

        @Test
        void shouldStopProcessingWhenOnePatternFails() {
            List<String> patterns = List.of("report:*:123*", "report:*::123");

            doThrow(new RuntimeException("Redis down")).when(cursor).forEachRemaining(any(Consumer.class));

            assertThatThrownBy(() -> redisCacheEvictor.evictByPatterns(patterns))
                    .isInstanceOf(IllegalStateException.class);

            verify(redisTemplate, times(1)).scan(any(ScanOptions.class));
        }
    }
}